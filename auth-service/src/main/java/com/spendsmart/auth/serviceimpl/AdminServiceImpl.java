package com.spendsmart.auth.serviceimpl;

import com.spendsmart.auth.dto.AdminDto;
import com.spendsmart.auth.dto.AuthDto;
import com.spendsmart.auth.entity.AuditLog;
import com.spendsmart.auth.entity.User;
import com.spendsmart.auth.repository.AuditLogRepository;
import com.spendsmart.auth.repository.UserRepository;
import com.spendsmart.auth.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    // ─── Users ────────────────────────────────────────────

    @Override
    public List<AdminDto.UserListItem> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toUserListItem)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AuthDto.MessageResponse suspendUser(Long userId) {
        User user = getUser(userId);
        user.setIsActive(false);
        userRepository.save(user);
        logAction("SYSTEM", "SUSPEND_USER", "userId=" + userId + " email=" + user.getEmail());
        return new AuthDto.MessageResponse("User suspended: " + user.getEmail(), true);
    }

    @Override
    @Transactional
    public AuthDto.MessageResponse unsuspendUser(Long userId) {
        User user = getUser(userId);
        user.setIsActive(true);
        userRepository.save(user);
        logAction("SYSTEM", "UNSUSPEND_USER", "userId=" + userId + " email=" + user.getEmail());
        return new AuthDto.MessageResponse("User reactivated: " + user.getEmail(), true);
    }

    @Override
    @Transactional
    public AuthDto.MessageResponse deleteUser(Long userId) {
        User user = getUser(userId);
        String email = user.getEmail();
        userRepository.deleteById(userId);
        logAction("SYSTEM", "DELETE_USER", "userId=" + userId + " email=" + email);
        return new AuthDto.MessageResponse("User deleted: " + email, true);
    }

    // ─── Stats ────────────────────────────────────────────

    @Override
    public AdminDto.PlatformStatsResponse getPlatformStats() {
        long total    = userRepository.count();
        long active   = userRepository.countByIsActive(true);
        long suspended= userRepository.countByIsActive(false);
        long admins   = userRepository.countByRole(User.Role.ADMIN);
        long regulars = userRepository.countByRole(User.Role.USER);

        AdminDto.PlatformStatsResponse stats = new AdminDto.PlatformStatsResponse();
        stats.setTotalUsers(total);
        stats.setActiveUsers(active);
        stats.setSuspendedUsers(suspended);
        stats.setAdminUsers(admins);
        stats.setRegularUsers(regulars);
        return stats;
    }

    // ─── Broadcast ────────────────────────────────────────

    @Override
    @Transactional
    public AuthDto.MessageResponse broadcastNotification(AdminDto.BroadcastRequest request) {
        String target = request.getUserIds() == null || request.getUserIds().isEmpty()
                ? "ALL_USERS"
                : "userIds=" + request.getUserIds();
        logAction("ADMIN", "BROADCAST_NOTIFICATION", target + " | msg=" + request.getMessage());
        return new AuthDto.MessageResponse("Broadcast sent to " + target, true);
    }

    // ─── Audit Logs ───────────────────────────────────────

    @Override
    public List<AdminDto.AuditLogResponse> getAuditLogs() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toAuditLogResponse)
                .toList();
    }

    @Override
    public void logAction(String actorEmail, String action, String targetDescription) {
        AuditLog log = AuditLog.builder()
                .actorEmail(actorEmail)
                .action(action)
                .targetDescription(targetDescription)
                .build();
        auditLogRepository.save(log);
    }

    // ─── Helpers ──────────────────────────────────────────

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    private AdminDto.UserListItem toUserListItem(User user) {
        AdminDto.UserListItem item = new AdminDto.UserListItem();
        item.setUserId(user.getUserId());
        item.setFullName(user.getFullName());
        item.setEmail(user.getEmail());
        item.setRole(user.getRole().name());
        item.setProvider(user.getProvider().name());
        item.setIsActive(user.getIsActive());
        item.setCurrency(user.getCurrency());
        item.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        item.setUpdatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null);
        return item;
    }

    private AdminDto.AuditLogResponse toAuditLogResponse(AuditLog log) {
        AdminDto.AuditLogResponse res = new AdminDto.AuditLogResponse();
        res.setId(log.getId());
        res.setActorEmail(log.getActorEmail());
        res.setAction(log.getAction());
        res.setTargetDescription(log.getTargetDescription());
        res.setCreatedAt(log.getCreatedAt() != null ? log.getCreatedAt().toString() : null);
        return res;
    }
}
