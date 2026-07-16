package com.civicdesk.analytics.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.civicdesk.analytics.service.IReportService;
import com.civicdesk.analytics.dto.response.ReportSummaryResponse;
import com.civicdesk.analytics.security.JwtAuthFilter;

import com.civicdesk.analytics.security.JwtTokenProvider;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IReportService reportService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;
    
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    

    @Test
    @WithMockUser(username = "testuser", roles = {"ADM"})
    void getReports_ShouldReturn200() throws Exception {

        ReportSummaryResponse response =
                ReportSummaryResponse.builder()
                        .count(0)
                        .build();

        when(reportService.getReportsByUserId("user1"))
                .thenReturn(response);

        mockMvc.perform(get("/reports/user/user1"))
                .andExpect(status().isOk());

        verify(reportService)
                .getReportsByUserId("user1");
    }
}