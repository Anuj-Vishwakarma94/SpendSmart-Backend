package com.spendsmart.budget.serviceimpl;

import com.spendsmart.budget.dto.BudgetDto;
import com.spendsmart.budget.dto.NotificationMessage;
import com.spendsmart.budget.entity.Budget;
import com.spendsmart.budget.repository.BudgetRepository;
import com.spendsmart.budget.service.BudgetService;
import com.spendsmart.budget.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override @Transactional
    public BudgetDto.BudgetResponse createBudget(Long userId, BudgetDto.CreateBudgetRequest req) {
        Budget budget = Budget.builder()
                .userId(userId)
                .categoryId(req.getCategoryId())
                .name(req.getName() != null ? req.getName() : "Budget")
                .limitAmount(req.getLimitAmount())
                .currency(req.getCurrency() != null ? req.getCurrency() : "INR")
                .period(parsePeriod(req.getPeriod()))
                .startDate(req.getStartDate() != null ? LocalDate.parse(req.getStartDate()) : LocalDate.now().withDayOfMonth(1))
                .endDate(req.getEndDate() != null ? LocalDate.parse(req.getEndDate()) : LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()))
                .spentAmount(0.0)
                .alertThreshold(req.getAlertThreshold() != null ? req.getAlertThreshold() : 80)
                .isActive(true)
                .build();
        return toResponse(budgetRepository.save(budget));
    }

    @Override
    public BudgetDto.BudgetResponse getBudgetById(Long budgetId, Long userId) {
        return toResponse(findAndValidate(budgetId, userId));
    }

    @Override
    public List<BudgetDto.BudgetResponse> getBudgetsByUser(Long userId) {
        return budgetRepository.findByUserId(userId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<BudgetDto.BudgetResponse> getActiveBudgets(Long userId) {
        return budgetRepository.findByUserIdAndIsActive(userId, true).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override @Transactional
    public BudgetDto.BudgetResponse updateBudget(Long budgetId, Long userId, BudgetDto.UpdateBudgetRequest req) {
        Budget b = findAndValidate(budgetId, userId);
        if (req.getName() != null)           b.setName(req.getName());
        if (req.getLimitAmount() != null)    b.setLimitAmount(req.getLimitAmount());
        if (req.getAlertThreshold() != null) b.setAlertThreshold(req.getAlertThreshold());
        if (req.getIsActive() != null)       b.setIsActive(req.getIsActive());
        if (req.getStartDate() != null)      b.setStartDate(LocalDate.parse(req.getStartDate()));
        if (req.getEndDate() != null)        b.setEndDate(LocalDate.parse(req.getEndDate()));
        return toResponse(budgetRepository.save(b));
    }

    @Override @Transactional
    public BudgetDto.MessageResponse deleteBudget(Long budgetId, Long userId) {
        findAndValidate(budgetId, userId);
        budgetRepository.deleteByBudgetId(budgetId);
        return new BudgetDto.MessageResponse("Budget deleted", true);
    }

    @Override @Transactional
    public BudgetDto.BudgetResponse updateSpentAmount(Long budgetId, Long userId, Double delta) {
        Budget b = findAndValidate(budgetId, userId);
        
        double oldSpent = b.getSpentAmount();
        double newSpent = Math.max(0, oldSpent + delta);
        b.setSpentAmount(newSpent);
        Budget saved = budgetRepository.save(b);
        
        checkAndSendNotifications(saved, oldSpent, newSpent);
        
        return toResponse(saved);
    }

    private void checkAndSendNotifications(Budget b, double oldSpent, double newSpent) {
        double limit = b.getLimitAmount();
        if (limit <= 0) return;

        double oldPct = (oldSpent / limit) * 100;
        double newPct = (newSpent / limit) * 100;
        int threshold = b.getAlertThreshold();

        // 1. Check for Limit Exceeded
        if (oldSpent < limit && newSpent >= limit) {
            sendNotification(b, "BUDGET_EXCEEDED", "CRITICAL", 
                "🚨 Budget Exceeded: " + b.getName(),
                String.format("You have exceeded your %s budget (Spent: ₹%.2f / Limit: ₹%.2f).", 
                    b.getName(), newSpent, limit));
        }
        // 2. Check for Threshold Breached (only if not already exceeded)
        else if (oldPct < threshold && newPct >= threshold) {
            sendNotification(b, "BUDGET_ALERT", "WARNING", 
                "⚠️ Budget Alert: " + b.getName(),
                String.format("You have used %.0f%% of your %s budget (Spent: ₹%.2f / Limit: ₹%.2f).", 
                    newPct, b.getName(), newSpent, limit));
        }
    }

    private void sendNotification(Budget b, String type, String severity, String title, String message) {
        try {
            NotificationMessage msg = NotificationMessage.builder()
                    .recipientId(b.getUserId())
                    .type(type)
                    .severity(severity)
                    .title(title)
                    .message(message)
                    .relatedId(b.getBudgetId())
                    .relatedType("BUDGET")
                    .build();
            
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, msg);
            log.info("Sent budget notification via RabbitMQ for user {}: {}", b.getUserId(), title);
        } catch (Exception e) {
            log.error("Failed to send RabbitMQ notification: {}", e.getMessage());
        }
    }

    @Override
    public BudgetDto.BudgetProgress getBudgetProgress(Long budgetId, Long userId) {
        return toProgress(findAndValidate(budgetId, userId));
    }

    @Override
    public List<BudgetDto.BudgetProgress> getAllProgress(Long userId) {
        return budgetRepository.findByUserIdAndIsActive(userId, true)
                .stream().map(this::toProgress).collect(Collectors.toList());
    }

    @Override
    public List<String> checkBudgetAlerts(Long userId) {
        return budgetRepository.findByUserIdAndIsActive(userId, true).stream()
                .filter(b -> {
                    double pct = b.getLimitAmount() > 0 ? (b.getSpentAmount() / b.getLimitAmount()) * 100 : 0;
                    return pct >= b.getAlertThreshold();
                })
                .map(b -> {
                    double pct = Math.round((b.getSpentAmount() / b.getLimitAmount()) * 100);
                    return String.format("Budget '%s' is at %.0f%% (%.2f / %.2f)", b.getName(), pct, b.getSpentAmount(), b.getLimitAmount());
                })
                .collect(Collectors.toList());
    }

    @Override @Transactional
    public void resetBudgetPeriods(Long userId, String period) {
        budgetRepository.resetPeriodForUser(userId, parsePeriod(period));
    }

    @Override
    public List<BudgetDto.BudgetResponse> getBudgetsByCategory(Long userId, Long categoryId) {
        return budgetRepository.findByUserIdAndCategoryId(userId, categoryId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── Helpers ──────────────────────────────────────────
    private Budget findAndValidate(Long budgetId, Long userId) {
        Budget b = budgetRepository.findByBudgetId(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found: " + budgetId));
        if (!b.getUserId().equals(userId)) throw new SecurityException("Access denied");
        return b;
    }

    private Budget.BudgetPeriod parsePeriod(String p) {
        try { return p != null ? Budget.BudgetPeriod.valueOf(p.toUpperCase()) : Budget.BudgetPeriod.MONTHLY; }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid period: " + p); }
    }

    private BudgetDto.BudgetProgress toProgress(Budget b) {
        BudgetDto.BudgetProgress p = new BudgetDto.BudgetProgress();
        p.setBudgetId(b.getBudgetId());
        p.setName(b.getName());
        p.setLimitAmount(b.getLimitAmount());
        p.setSpentAmount(b.getSpentAmount());
        p.setRemainingAmount(Math.max(0, b.getLimitAmount() - b.getSpentAmount()));
        double pct = b.getLimitAmount() > 0 ? (b.getSpentAmount() / b.getLimitAmount()) * 100 : 0;
        p.setPercentageUsed(Math.round(pct * 10.0) / 10.0);
        p.setThresholdBreached(pct >= b.getAlertThreshold());
        p.setLimitExceeded(b.getSpentAmount() >= b.getLimitAmount());
        p.setStatus(b.getSpentAmount() >= b.getLimitAmount() ? "EXCEEDED" : pct >= b.getAlertThreshold() ? "WARNING" : "SAFE");
        return p;
    }

    private BudgetDto.BudgetResponse toResponse(Budget b) {
        BudgetDto.BudgetResponse r = new BudgetDto.BudgetResponse();
        r.setBudgetId(b.getBudgetId()); r.setUserId(b.getUserId()); r.setCategoryId(b.getCategoryId());
        r.setName(b.getName()); r.setLimitAmount(b.getLimitAmount()); r.setSpentAmount(b.getSpentAmount());
        r.setCurrency(b.getCurrency()); r.setPeriod(b.getPeriod().name());
        r.setStartDate(b.getStartDate() != null ? b.getStartDate().toString() : null);
        r.setEndDate(b.getEndDate() != null ? b.getEndDate().toString() : null);
        r.setAlertThreshold(b.getAlertThreshold()); r.setIsActive(b.getIsActive());
        r.setCreatedAt(b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);
        return r;
    }
}
