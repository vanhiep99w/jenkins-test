package com.seft.learn.jenkinstest.service;

import com.seft.learn.jenkinstest.dto.ProductRequest;
import com.seft.learn.jenkinstest.exception.ProductNotFoundException;
import com.seft.learn.jenkinstest.model.Product;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final Map<Long, Product> productStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Product createProduct(ProductRequest request) {
        validateProductRequest(request);

        Product product = new Product();
        product.setId(idGenerator.getAndIncrement());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setCategory(request.getCategory());

        productStore.put(product.getId(), product);
        return product;
    }

    public Product getProductById(Long id) {
        return Optional.ofNullable(productStore.get(id))
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public List<Product> getAllProducts() {
        return new ArrayList<>(productStore.values());
    }

    public List<Product> getProductsByCategory(String category) {
        return productStore.values().stream()
                .filter(p -> p.getCategory() != null && p.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    public List<Product> getActiveProducts() {
        return productStore.values().stream()
                .filter(Product::isActive)
                .collect(Collectors.toList());
    }

    public List<Product> searchProducts(String query) {
        if (query == null || query.isBlank()) {
            return getAllProducts();
        }

        String lowerQuery = query.toLowerCase();
        return productStore.values().stream()
                .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(lowerQuery)) ||
                        (p.getDescription() != null && p.getDescription().toLowerCase().contains(lowerQuery)))
                .collect(Collectors.toList());
    }

    public Product updateProduct(Long id, ProductRequest request) {
        Product existing = getProductById(id);
        validateProductRequest(request);

        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setPrice(request.getPrice());
        existing.setQuantity(request.getQuantity());
        existing.setCategory(request.getCategory());
        existing.setUpdatedAt(LocalDateTime.now());

        return existing;
    }

    public Product updateProductQuantity(Long id, Integer quantity) {
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Quantity must be a non-negative number");
        }

        Product product = getProductById(id);
        product.setQuantity(quantity);
        product.setUpdatedAt(LocalDateTime.now());
        return product;
    }

    public Product toggleProductStatus(Long id) {
        Product product = getProductById(id);
        product.setActive(!product.isActive());
        product.setUpdatedAt(LocalDateTime.now());
        return product;
    }

    public void deleteProduct(Long id) {
        if (!productStore.containsKey(id)) {
            throw new ProductNotFoundException(id);
        }
        productStore.remove(id);
    }

    public long countProducts() {
        return productStore.size();
    }

    public long countActiveProducts() {
        return productStore.values().stream()
                .filter(Product::isActive)
                .count();
    }

    private void validateProductRequest(ProductRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (request.getPrice() == null || request.getPrice().doubleValue() < 0) {
            throw new IllegalArgumentException("Product price must be a non-negative number");
        }
        if (request.getQuantity() == null || request.getQuantity() < 0) {
            throw new IllegalArgumentException("Product quantity must be a non-negative number");
        }
    }
}
