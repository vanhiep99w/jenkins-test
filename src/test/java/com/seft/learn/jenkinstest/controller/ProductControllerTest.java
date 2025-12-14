package com.seft.learn.jenkinstest.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/v1/products - Should create product successfully")
    void createProduct_ShouldReturnCreated() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Test Laptop",
                                    "description": "Gaming laptop",
                                    "price": 1499.99,
                                    "quantity": 10,
                                    "category": "Electronics"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product created successfully"))
                .andExpect(jsonPath("$.data.name").value("Test Laptop"))
                .andExpect(jsonPath("$.data.price").value(1499.99))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/products - Should return 400 for missing name")
    void createProduct_ShouldReturn400ForMissingName() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Description",
                                    "price": 99.99,
                                    "quantity": 5,
                                    "category": "Category"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("name")));
    }

    @Test
    @DisplayName("POST /api/v1/products - Should return 400 for negative price")
    void createProduct_ShouldReturn400ForNegativePrice() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Test Product",
                                    "price": -10.00,
                                    "quantity": 5,
                                    "category": "Category"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/products - Should return all products")
    void getAllProducts_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Should return 404 for non-existent product")
    void getProduct_ShouldReturn404ForNonExistent() throws Exception {
        mockMvc.perform(get("/api/v1/products/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("99999")));
    }

    @Test
    @DisplayName("GET /api/v1/products/search - Should search products")
    void searchProducts_ShouldReturnResults() throws Exception {
        mockMvc.perform(get("/api/v1/products/search").param("q", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/products/stats - Should return statistics")
    void getStats_ShouldReturnProductStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/products/stats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalProducts").isNumber())
                .andExpect(jsonPath("$.data.activeProducts").isNumber());
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} - Should return 404 for non-existent product")
    void deleteProduct_ShouldReturn404ForNonExistent() throws Exception {
        mockMvc.perform(delete("/api/v1/products/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
