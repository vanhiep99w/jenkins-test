package com.seft.learn.jenkinstest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class TestController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now().toString(),
                "service", "jenkins-test"
        ));

        
    }

    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello() {
        return ResponseEntity.ok(Map.of("message", "Hello from Jenkins Test Application!"));
    }

    @GetMapping("/hello/{name}")
    public ResponseEntity<Map<String, String>> helloName(@PathVariable String name) {
        return ResponseEntity.ok(Map.of("message", "Hello, " + name + "!"));
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
                "application", "jenkins-test",
                "version", "0.0.1-SNAPSHOT",
                "java", System.getProperty("java.version"),
                "environment", getEnvironment()
        ));
    }

    private String getEnvironment() {
        String env = System.getenv("APP_ENV");
        return env != null ? env : "development";
    }
}
