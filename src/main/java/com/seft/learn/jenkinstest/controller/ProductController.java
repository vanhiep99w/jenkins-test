package com.seft.learn.jenkinstest.controller;

import com.seft.learn.jenkinstest.dto.ApiResponse;
import com.seft.learn.jenkinstest.dto.ProductRequest;
import com.seft.learn.jenkinstest.dto.ProductResponse;
import com.seft.learn.jenkinstest.model.Product;
import com.seft.learn.jenkinstest.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@RequestBody ProductRequest request) {
        Product product = productService.createProduct(request);
        ProductResponse response = ProductResponse.fromProduct(product);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        ProductResponse response = ProductResponse.fromProduct(product);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean activeOnly) {

        List<Product> products;

        if (category != null && !category.isBlank()) {
            products = productService.getProductsByCategory(category);
        } else if (Boolean.TRUE.equals(activeOnly)) {
            products = productService.getActiveProducts();
        } else {
            products = productService.getAllProducts();
        }

        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::fromProduct)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(@RequestParam String q) {
        List<Product> products = productService.searchProducts(q);
        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::fromProduct)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequest request) {
        Product product = productService.updateProduct(id, request);
        ProductResponse response = ProductResponse.fromProduct(product);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", response));
    }

    @PatchMapping("/{id}/quantity")
    public ResponseEntity<ApiResponse<ProductResponse>> updateQuantity(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {
        Integer quantity = body.get("quantity");
        Product product = productService.updateProductQuantity(id, quantity);
        ProductResponse response = ProductResponse.fromProduct(product);
        return ResponseEntity.ok(ApiResponse.success("Quantity updated successfully", response));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<ProductResponse>> toggleStatus(@PathVariable Long id) {
        Product product = productService.toggleProductStatus(id);
        ProductResponse response = ProductResponse.fromProduct(product);
        String message = product.isActive() ? "Product activated" : "Product deactivated";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = Map.of(
                "totalProducts", productService.countProducts(),
                "activeProducts", productService.countActiveProducts()
        );
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
