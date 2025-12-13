package com.seft.learn.jenkinstest.controller;

import org.springframework.web.bind.annotation.*;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    // Hardcoded database credentials - SECURITY ISSUE
    private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb";
    private static final String DB_USER = "admin";
    private static final String DB_PASSWORD = "password123";

    // Hardcoded API key - SECURITY ISSUE
    private String apiKey = "sk-1234567890abcdef";

    @GetMapping("/{id}")
    public Map<String, Object> getUser(@PathVariable String id) {
        Map<String, Object> user = null;
        
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            
            // SQL Injection vulnerability - SECURITY ISSUE
            String query = "SELECT * FROM users WHERE id = " + id;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            if (rs.next()) {
                user = new HashMap<>();
                user.put("id", rs.getString("id"));
                user.put("name", rs.getString("name"));
                user.put("email", rs.getString("email"));
            }
            
            // Resource leak - connection not closed properly
            
        } catch (SQLException e) {
            // Poor error handling - swallowing exception
            e.printStackTrace();
        }
        
        // Potential NullPointerException - not checking if user is null
        user.put("timestamp", System.currentTimeMillis());
        
        return user;
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) {
        // Logging sensitive data - SECURITY ISSUE
        System.out.println("Login attempt: " + username + " / " + password);
        
        // Hardcoded credentials check - SECURITY ISSUE
        if (username.equals("admin") && password.equals("admin123")) {
            return "Login successful";
        }
        
        return "Login failed";
    }

    @GetMapping("/search")
    public List<String> searchUsers(@RequestParam String query) {
        List<String> results = new ArrayList<>();
        
        // No input validation - potential injection
        // No pagination - could return too many results
        
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement stmt = conn.createStatement();
            
            // Another SQL injection
            ResultSet rs = stmt.executeQuery("SELECT name FROM users WHERE name LIKE '%" + query + "%'");
            
            while (rs.next()) {
                results.add(rs.getString("name"));
            }
        } catch (Exception e) {
            // Catching generic Exception - bad practice
            return null; // Returning null instead of empty list
        }
        
        return results;
    }

    // Unused method - dead code
    private void unusedMethod() {
        System.out.println("This method is never called");
    }
}
