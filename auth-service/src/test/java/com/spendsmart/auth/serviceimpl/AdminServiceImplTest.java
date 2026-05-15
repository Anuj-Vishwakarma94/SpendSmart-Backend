package com.spendsmart.auth.serviceimpl;

import com.spendsmart.auth.dto.AdminDto;
import com.spendsmart.auth.dto.AuthDto;
import com.spendsmart.auth.entity.AuditLog;
import com.spendsmart.auth.entity.User;
import com.spendsmart.auth.repository.AuditLogRepository;
import com.spendsmart.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User testUser;
    private AuditLog testLog;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .fullName("Test User")
                .email("test@example.com")
                .provider(User.AuthProvider.LOCAL)
                .role(User.Role.USER)
                .isActive(true)
                .build();

        testLog = AuditLog.builder()
                .id(1L)
                .actorEmail("admin@example.com")
                .action("SUSPEND_USER")
                .targetDescription("userId=1 email=test@example.com")
                .build();
    }

    @Test
    void getAllUsers_ReturnsList() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        List<AdminDto.UserListItem> users = adminService.getAllUsers();
        assertEquals(1, users.size());
        assertEquals("test@example.com", users.get(0).getEmail());
    }

    @Test
    void suspendUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        AuthDto.MessageResponse response = adminService.suspendUser(1L);
        assertTrue(response.isSuccess());
        assertFalse(testUser.getIsActive());
        verify(userRepository, times(1)).save(testUser);
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void unsuspendUser_Success() {
        testUser.setIsActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        AuthDto.MessageResponse response = adminService.unsuspendUser(1L);
        assertTrue(response.isSuccess());
        assertTrue(testUser.getIsActive());
        verify(userRepository, times(1)).save(testUser);
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void deleteUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        AuthDto.MessageResponse response = adminService.deleteUser(1L);
        assertTrue(response.isSuccess());
        verify(userRepository, times(1)).deleteById(1L);
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void getPlatformStats_Success() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByIsActive(true)).thenReturn(8L);
        when(userRepository.countByIsActive(false)).thenReturn(2L);
        when(userRepository.countByRole(User.Role.ADMIN)).thenReturn(1L);
        when(userRepository.countByRole(User.Role.USER)).thenReturn(9L);

        AdminDto.PlatformStatsResponse stats = adminService.getPlatformStats();
        assertEquals(10L, stats.getTotalUsers());
        assertEquals(8L, stats.getActiveUsers());
        assertEquals(2L, stats.getSuspendedUsers());
        assertEquals(1L, stats.getAdminUsers());
        assertEquals(9L, stats.getRegularUsers());
    }

    @Test
    void broadcastNotification_Success() {
        AdminDto.BroadcastRequest request = new AdminDto.BroadcastRequest();
        request.setMessage("Test broadcast");
        AuthDto.MessageResponse response = adminService.broadcastNotification(request);
        assertTrue(response.isSuccess());
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void getAuditLogs_ReturnsList() {
        when(auditLogRepository.findTop100ByOrderByCreatedAtDesc()).thenReturn(List.of(testLog));
        List<AdminDto.AuditLogResponse> logs = adminService.getAuditLogs();
        assertEquals(1, logs.size());
        assertEquals("SUSPEND_USER", logs.get(0).getAction());
    }

    @Test
    void logAction_Success() {
        adminService.logAction("admin@example.com", "TEST_ACTION", "Test Target");
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }
}
