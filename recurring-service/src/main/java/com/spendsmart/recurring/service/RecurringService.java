package com.spendsmart.recurring.service;

import com.spendsmart.recurring.dto.RecurringDto;
import java.util.List;

public interface RecurringService {
    RecurringDto.RecurringResponse addRecurring(Long userId, RecurringDto.CreateRequest req);
    RecurringDto.RecurringResponse getById(Long recurringId, Long userId);
    List<RecurringDto.RecurringResponse> getByUser(Long userId);
    List<RecurringDto.RecurringResponse> getActiveRecurring(Long userId);
    List<RecurringDto.RecurringResponse> getByType(Long userId, String type);
    RecurringDto.RecurringResponse updateRecurring(Long recurringId, Long userId, RecurringDto.UpdateRequest req);
    RecurringDto.MessageResponse deactivateRecurring(Long recurringId, Long userId);
    RecurringDto.MessageResponse deleteRecurring(Long recurringId, Long userId);
    RecurringDto.MessageResponse processManualPayment(Long recurringId, Long userId, String authToken);

    /** Called by Spring @Scheduled daily at midnight — generates expense/income entries. */
    void processUpcomingDue();

    /** Returns rules due within the current calendar month. */
    List<RecurringDto.RecurringResponse> getUpcomingThisMonth(Long userId);

    /** Returns rules whose nextDueDate is within the next N days. */
    List<RecurringDto.RecurringResponse> getDueWithinDays(int days);
}
