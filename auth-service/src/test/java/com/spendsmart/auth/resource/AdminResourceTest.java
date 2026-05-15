package com.spendsmart.auth.resource;

import com.spendsmart.auth.dto.AdminDto;
import com.spendsmart.auth.dto.AuthDto;
import com.spendsmart.auth.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminResourceTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminResource adminResource;

    private AdminDto.UserListItem userListItem;
    private AuthDto.MessageResponse messageResponse;

    @BeforeEach
    void setUp() {
        userListItem = new AdminDto.UserListItem();
        userListItem.setUserId(1L);
        userListItem.setEmail("test@example.com");

        messageResponse = new AuthDto.MessageResponse("Success", true);
    }

    @Test
    void getAllUsers_ReturnsList() {
        when(adminService.getAllUsers()).thenReturn(List.of(userListItem));
        ResponseEntity<List<AdminDto.UserListItem>> response = adminResource.getAllUsers();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void suspendUser_ReturnsOk() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin");
        when(adminService.suspendUser(1L)).thenReturn(messageResponse);
        ResponseEntity<AuthDto.MessageResponse> response = adminResource.suspendUser(1L, auth);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messageResponse, response.getBody());
    }

    @Test
    void unsuspendUser_ReturnsOk() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin");
        when(adminService.unsuspendUser(1L)).thenReturn(messageResponse);
        ResponseEntity<AuthDto.MessageResponse> response = adminResource.unsuspendUser(1L, auth);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messageResponse, response.getBody());
    }

    @Test
    void deleteUser_ReturnsOk() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin");
        when(adminService.deleteUser(1L)).thenReturn(messageResponse);
        ResponseEntity<AuthDto.MessageResponse> response = adminResource.deleteUser(1L, auth);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messageResponse, response.getBody());
    }

    @Test
    void getPlatformStats_ReturnsStats() {
        AdminDto.PlatformStatsResponse stats = new AdminDto.PlatformStatsResponse();
        when(adminService.getPlatformStats()).thenReturn(stats);
        ResponseEntity<AdminDto.PlatformStatsResponse> response = adminResource.getPlatformStats();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(stats, response.getBody());
    }

    @Test
    void broadcast_ReturnsOk() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin");
        AdminDto.BroadcastRequest request = new AdminDto.BroadcastRequest();
        when(adminService.broadcastNotification(any(AdminDto.BroadcastRequest.class))).thenReturn(messageResponse);
        ResponseEntity<AuthDto.MessageResponse> response = adminResource.broadcast(request, auth);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messageResponse, response.getBody());
    }

    @Test
    void getAuditLogs_ReturnsLogs() {
        AdminDto.AuditLogResponse log = new AdminDto.AuditLogResponse();
        when(adminService.getAuditLogs()).thenReturn(List.of(log));
        ResponseEntity<List<AdminDto.AuditLogResponse>> response = adminResource.getAuditLogs();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }
}
