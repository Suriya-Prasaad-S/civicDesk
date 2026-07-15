package com.civicdesk.grievance.service;

import com.civicdesk.grievance.client.AuthClient;
import com.civicdesk.grievance.dto.request.CreateAuditLogRequest;
import com.civicdesk.grievance.security.JwtUserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditHelperServiceTest {

    @Mock
    private AuthClient authClient;

    @Test
    void log_Success() {

        AuditHelperService service =
                new AuditHelperService(authClient);

        try (MockedStatic<JwtUserContext> mocked =
                     mockStatic(JwtUserContext.class)) {

            mocked.when(
                            JwtUserContext::getCurrentUserId)
                    .thenReturn("1001");

            service.log("CREATE_GRIEVANCE");

            ArgumentCaptor<CreateAuditLogRequest> captor =
                    ArgumentCaptor.forClass(
                            CreateAuditLogRequest.class);

            verify(authClient)
                    .createAuditLog(captor.capture());

            CreateAuditLogRequest request =
                    captor.getValue();

            assertEquals(
                    "1001",
                    request.getUserId());

            assertEquals(
                    "CREATE_GRIEVANCE",
                    request.getAction());

            assertEquals(
                    "GRIEVANCE",
                    request.getModule());
        }
    }
}