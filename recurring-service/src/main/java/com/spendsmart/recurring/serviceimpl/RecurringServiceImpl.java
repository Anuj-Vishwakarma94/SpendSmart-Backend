package com.spendsmart.recurring.serviceimpl;

import com.spendsmart.recurring.dto.RecurringDto;
import com.spendsmart.recurring.entity.RecurringTransaction;
import com.spendsmart.recurring.entity.RecurringTransaction.*;
import com.spendsmart.recurring.repository.RecurringRepository;
import com.spendsmart.recurring.service.RecurringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class RecurringServiceImpl implements RecurringService {

    private final RecurringRepository recurringRepository;
    private final RestTemplate restTemplate;

    @Value("${expense.service.url:http://localhost:8082}") private String expenseUrl;
    @Value("${income.service.url:http://localhost:8083}")  private String incomeUrl;

    // ─── CRUD ─────────────────────────────────────────────

    @Override @Transactional
    public RecurringDto.RecurringResponse addRecurring(Long userId, RecurringDto.CreateRequest req) {
        LocalDate start = LocalDate.parse(req.getStartDate());
        RecurringTransaction rt = RecurringTransaction.builder()
                .userId(userId)
                .categoryId(req.getCategoryId())
                .title(req.getTitle())
                .amount(req.getAmount())
                .type(parseType(req.getType()))
                .frequency(parseFreq(req.getFrequency()))
                .startDate(start)
                .endDate(req.getEndDate() != null ? LocalDate.parse(req.getEndDate()) : null)
                .nextDueDate(start)
                .isActive(true)
                .paymentMethod(parsePM(req.getPaymentMethod()))
                .description(req.getDescription())
                .currency(req.getCurrency() != null ? req.getCurrency() : "INR")
                .build();
        return toResponse(recurringRepository.save(rt));
    }

    @Override
    public RecurringDto.RecurringResponse getById(Long id, Long userId) {
        return toResponse(findAndValidate(id, userId));
    }

    @Override
    public List<RecurringDto.RecurringResponse> getByUser(Long userId) {
        return recurringRepository.findByUserId(userId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<RecurringDto.RecurringResponse> getActiveRecurring(Long userId) {
        return recurringRepository.findByUserIdAndIsActive(userId, true).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<RecurringDto.RecurringResponse> getByType(Long userId, String type) {
        return recurringRepository.findByUserIdAndType(userId, parseType(type)).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override @Transactional
    public RecurringDto.RecurringResponse updateRecurring(Long id, Long userId, RecurringDto.UpdateRequest req) {
        RecurringTransaction rt = findAndValidate(id, userId);
        if (req.getTitle() != null)         rt.setTitle(req.getTitle());
        if (req.getAmount() != null)        rt.setAmount(req.getAmount());
        if (req.getFrequency() != null)     rt.setFrequency(parseFreq(req.getFrequency()));
        if (req.getEndDate() != null)       rt.setEndDate(LocalDate.parse(req.getEndDate()));
        if (req.getCategoryId() != null)    rt.setCategoryId(req.getCategoryId());
        if (req.getPaymentMethod() != null) rt.setPaymentMethod(parsePM(req.getPaymentMethod()));
        if (req.getDescription() != null)   rt.setDescription(req.getDescription());
        if (req.getIsActive() != null)      rt.setIsActive(req.getIsActive());
        return toResponse(recurringRepository.save(rt));
    }

    @Override @Transactional
    public RecurringDto.MessageResponse deactivateRecurring(Long id, Long userId) {
        RecurringTransaction rt = findAndValidate(id, userId);
        rt.setIsActive(false);
        recurringRepository.save(rt);
        return new RecurringDto.MessageResponse("Recurring rule deactivated. Past entries preserved.", true);
    }

    @Override @Transactional
    public RecurringDto.MessageResponse deleteRecurring(Long id, Long userId) {
        findAndValidate(id, userId);
        recurringRepository.deleteByRecurringId(id);
        return new RecurringDto.MessageResponse("Recurring rule deleted", true);
    }

    @Override @Transactional
    public RecurringDto.MessageResponse processManualPayment(Long id, Long userId, String authToken) {
        RecurringTransaction rt = findAndValidate(id, userId);

        if (!rt.getIsActive()) {
            throw new RuntimeException("Cannot process inactive rule");
        }

        if (rt.getNextDueDate().isAfter(LocalDate.now())) {
            throw new RuntimeException("Transaction is not yet due");
        }

        try {
            generateTransaction(rt, authToken);
            rt.setNextDueDate(advanceDate(rt.getNextDueDate(), rt.getFrequency()));

            if (rt.getEndDate() != null && rt.getNextDueDate().isAfter(rt.getEndDate())) {
                rt.setIsActive(false);
            }
            recurringRepository.save(rt);
            return new RecurringDto.MessageResponse("Transaction processed successfully", true);
        } catch (Exception e) {
            log.error("Failed to process manual payment for rule {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to process transaction: " + e.getMessage());
        }
    }

    // ─── Scheduler ────────────────────────────────────────

    /**
     * Runs every day at 00:05 AM.
     * Finds all active recurring rules whose nextDueDate <= today,
     * creates the transaction via Expense/Income-Service,
     * then advances nextDueDate by one frequency period.
     */
    @Override
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void processUpcomingDue() {
        List<RecurringTransaction> due = recurringRepository
                .findByIsActiveAndNextDueDateLessThanEqual(true, LocalDate.now());

        log.info("RecurringScheduler: processing {} due transactions", due.size());

        for (RecurringTransaction rt : due) {
            try {
                generateTransaction(rt, null);
                rt.setNextDueDate(advanceDate(rt.getNextDueDate(), rt.getFrequency()));

                // Deactivate if past endDate
                if (rt.getEndDate() != null && rt.getNextDueDate().isAfter(rt.getEndDate())) {
                    rt.setIsActive(false);
                    log.info("RecurringScheduler: rule {} reached endDate, deactivated", rt.getRecurringId());
                }
                recurringRepository.save(rt);
            } catch (Exception e) {
                log.error("RecurringScheduler: failed to process rule {}: {}", rt.getRecurringId(), e.getMessage());
            }
        }
    }

    @Override
    public List<RecurringDto.RecurringResponse> getUpcomingThisMonth(Long userId) {
        return recurringRepository.findDueThisMonth(userId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<RecurringDto.RecurringResponse> getDueWithinDays(int days) {
        LocalDate cutoff = LocalDate.now().plusDays(days);
        return recurringRepository.findDueWithinDays(cutoff).stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── Private helpers ──────────────────────────────────

    private void generateTransaction(RecurringTransaction rt, String authToken) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title",         rt.getTitle());
        payload.put("amount",        rt.getAmount());
        payload.put("categoryId",    rt.getCategoryId());
        payload.put("date",          rt.getNextDueDate().toString());
        payload.put("notes",         "Auto-generated from recurring rule #" + rt.getRecurringId());
        payload.put("isRecurring",   false);
        payload.put("currency",      rt.getCurrency());
        payload.put("paymentMethod", rt.getPaymentMethod().name());

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        if (authToken != null) {
            headers.set("Authorization", authToken);
        }
        var entity = new org.springframework.http.HttpEntity<>(payload, headers);

        if (rt.getType() == TransactionType.EXPENSE) {
            restTemplate.postForObject(expenseUrl + "/api/expenses", entity, Map.class);
        } else {
            payload.put("source", "OTHER");
            restTemplate.postForObject(incomeUrl + "/api/incomes", entity, Map.class);
        }
    }

    private LocalDate advanceDate(LocalDate date, Frequency freq) {
        return switch (freq) {
            case DAILY     -> date.plusDays(1);
            case WEEKLY    -> date.plusWeeks(1);
            case MONTHLY   -> date.plusMonths(1);
            case QUARTERLY -> date.plusMonths(3);
            case YEARLY    -> date.plusYears(1);
        };
    }

    private RecurringTransaction findAndValidate(Long id, Long userId) {
        RecurringTransaction rt = recurringRepository.findByRecurringId(id)
                .orElseThrow(() -> new RuntimeException("Recurring rule not found: " + id));
        if (!rt.getUserId().equals(userId)) throw new SecurityException("Access denied");
        return rt;
    }

    private TransactionType parseType(String t) {
        try { return TransactionType.valueOf(t.toUpperCase()); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid type: " + t); }
    }

    private Frequency parseFreq(String f) {
        try { return Frequency.valueOf(f.toUpperCase()); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid frequency: " + f); }
    }

    private PaymentMethod parsePM(String p) {
        try { return p != null ? PaymentMethod.valueOf(p.toUpperCase()) : PaymentMethod.CASH; }
        catch (Exception e) { return PaymentMethod.CASH; }
    }

    private RecurringDto.RecurringResponse toResponse(RecurringTransaction rt) {
        RecurringDto.RecurringResponse r = new RecurringDto.RecurringResponse();
        r.setRecurringId(rt.getRecurringId()); r.setUserId(rt.getUserId()); r.setCategoryId(rt.getCategoryId());
        r.setTitle(rt.getTitle()); r.setAmount(rt.getAmount()); r.setCurrency(rt.getCurrency());
        r.setType(rt.getType().name()); r.setFrequency(rt.getFrequency().name());
        r.setStartDate(rt.getStartDate().toString());
        r.setEndDate(rt.getEndDate() != null ? rt.getEndDate().toString() : null);
        r.setNextDueDate(rt.getNextDueDate() != null ? rt.getNextDueDate().toString() : null);
        r.setIsActive(rt.getIsActive()); r.setDescription(rt.getDescription());
        r.setPaymentMethod(rt.getPaymentMethod().name());
        r.setCreatedAt(rt.getCreatedAt() != null ? rt.getCreatedAt().toString() : null);
        if (rt.getNextDueDate() != null) {
            r.setDaysUntilDue(ChronoUnit.DAYS.between(LocalDate.now(), rt.getNextDueDate()));
        }
        return r;
    }
}
