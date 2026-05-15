package com.spendsmart.recurring.resource;

import com.spendsmart.recurring.dto.RecurringDto;
import com.spendsmart.recurring.service.RecurringService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/recurring")
@RequiredArgsConstructor
public class RecurringResource {

    private final RecurringService recurringService;
    private Long uid(Authentication a) { return (Long) a.getDetails(); }

    @PostMapping
    public ResponseEntity<RecurringDto.RecurringResponse> add(Authentication a, @Valid @RequestBody RecurringDto.CreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recurringService.addRecurring(uid(a), req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecurringDto.RecurringResponse> getById(Authentication a, @PathVariable Long id) {
        return ResponseEntity.ok(recurringService.getById(id, uid(a)));
    }

    @GetMapping
    public ResponseEntity<List<RecurringDto.RecurringResponse>> getAll(Authentication a) {
        return ResponseEntity.ok(recurringService.getByUser(uid(a)));
    }

    @GetMapping("/active")
    public ResponseEntity<List<RecurringDto.RecurringResponse>> getActive(Authentication a) {
        return ResponseEntity.ok(recurringService.getActiveRecurring(uid(a)));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<RecurringDto.RecurringResponse>> getByType(Authentication a, @PathVariable String type) {
        return ResponseEntity.ok(recurringService.getByType(uid(a), type));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<RecurringDto.RecurringResponse>> getUpcomingThisMonth(Authentication a) {
        return ResponseEntity.ok(recurringService.getUpcomingThisMonth(uid(a)));
    }

    @GetMapping("/due-soon")
    public ResponseEntity<List<RecurringDto.RecurringResponse>> getDueSoon(@RequestParam(defaultValue = "3") int days) {
        return ResponseEntity.ok(recurringService.getDueWithinDays(days));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringDto.RecurringResponse> update(Authentication a, @PathVariable Long id, @RequestBody RecurringDto.UpdateRequest req) {
        return ResponseEntity.ok(recurringService.updateRecurring(id, uid(a), req));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<RecurringDto.MessageResponse> deactivate(Authentication a, @PathVariable Long id) {
        return ResponseEntity.ok(recurringService.deactivateRecurring(id, uid(a)));
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<RecurringDto.MessageResponse> processManual(
            Authentication a, @PathVariable Long id, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        return ResponseEntity.ok(recurringService.processManualPayment(id, uid(a), authHeader));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RecurringDto.MessageResponse> delete(Authentication a, @PathVariable Long id) {
        return ResponseEntity.ok(recurringService.deleteRecurring(id, uid(a)));
    }
}
