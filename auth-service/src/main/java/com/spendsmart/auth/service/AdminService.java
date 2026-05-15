package com.spendsmart.auth.service;

import com.spendsmart.auth.dto.AdminDto;
import com.spendsmart.auth.dto.AuthDto;

import java.util.List;

public interface AdminService {

    List<AdminDto.UserListItem> getAllUsers();

    AuthDto.MessageResponse suspendUser(Long userId);

    AuthDto.MessageResponse unsuspendUser(Long userId);

    AuthDto.MessageResponse deleteUser(Long userId);

    AdminDto.PlatformStatsResponse getPlatformStats();

    AuthDto.MessageResponse broadcastNotification(AdminDto.BroadcastRequest request);

    List<AdminDto.AuditLogResponse> getAuditLogs();

    void logAction(String actorEmail, String action, String targetDescription);
}
