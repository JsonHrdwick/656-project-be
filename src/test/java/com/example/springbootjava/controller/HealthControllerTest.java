package com.example.springbootjava.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataSource dataSource;

    @Test
    void testPublicHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("AI Study Platform Backend"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testHealthEndpointWithDatabaseUp() throws Exception {
        Connection mockConnection = org.mockito.Mockito.mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(mockConnection);
        
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.service").value("AI Study Platform Backend"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testHealthEndpointWithDatabaseDown() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection failed"));
        
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("DOWN"))
                .andExpect(jsonPath("$.databaseError").exists())
                .andExpect(jsonPath("$.service").value("AI Study Platform Backend"));
    }

    @Test
    void testRootHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("Service is running"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testRailwayHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/railway"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("springboot-java"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}

