package com.civicdesk.publicworks.controller;

import com.civicdesk.publicworks.security.JwtAuthFilter;
import com.civicdesk.publicworks.security.JwtTokenProvider;
import com.civicdesk.publicworks.service.WorkOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@WebMvcTest(WorkOrderController.class)
class WorkOrderControllerTest {

    @MockBean
    private WorkOrderService workOrderService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void contextLoads() {
    }
}