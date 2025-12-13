package com.seft.learn.jenkinstest.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Health endpoint should return UP status")
    void healthCheck_ShouldReturnUpStatus() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("jenkins-test"));
    }

    @Test
    @DisplayName("Hello endpoint should return greeting message")
    void hello_ShouldReturnGreetingMessage() throws Exception {
        mockMvc.perform(get("/api/v1/hello"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Hello from Jenkins Test Application!"));
    }

    @Test
    @DisplayName("Hello with name should return personalized greeting")
    void helloName_ShouldReturnPersonalizedGreeting() throws Exception {
        mockMvc.perform(get("/api/v1/hello/John"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Hello, John!"));
    }

    @Test
    @DisplayName("Info endpoint should return application info")
    void info_ShouldReturnApplicationInfo() throws Exception {
        mockMvc.perform(get("/api/v1/info"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.application").value("jenkins-test"))
                .andExpect(jsonPath("$.version").value("0.0.1-SNAPSHOT"));
    }
}
