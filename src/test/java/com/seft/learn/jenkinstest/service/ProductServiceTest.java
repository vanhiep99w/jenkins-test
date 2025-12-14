package com.seft.learn.jenkinstest.service;

import com.seft.learn.jenkinstest.dto.ProductRequest;
import com.seft.learn.jenkinstest.exception.ProductNotFoundException;
import com.seft.learn.jenkinstest.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductServiceTest {

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService();
    }

    private ProductRequest createValidRequest() {
        return new ProductRequest("Test Product", "Test Description", BigDecimal.valueOf(99.99), 10, "Electronics");
    }

    @Nested
    @DisplayName("Create Product Tests")
    class CreateProductTests {

        @Test
        @DisplayName("Should create product successfully with valid data")
        void shouldCreateProductSuccessfully() {
            ProductRequest request = createValidRequest();

            Product product = productService.createProduct(request);

            assertNotNull(product);
            assertNotNull(product.getId());
            assertEquals("Test Product", product.getName());
            assertEquals("Test Description", product.getDescription());
            assertEquals(BigDecimal.valueOf(99.99), product.getPrice());
            assertEquals(10, product.getQuantity());
            assertEquals("Electronics", product.getCategory());
            assertTrue(product.isActive());
            assertNotNull(product.getCreatedAt());
            assertNotNull(product.getUpdatedAt());
        }

        @Test
        @DisplayName("Should throw exception when name is null")
        void shouldThrowExceptionWhenNameIsNull() {
            ProductRequest request = new ProductRequest(null, "Desc", BigDecimal.TEN, 5, "Category");

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> productService.createProduct(request));
            assertEquals("Product name is required", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when name is blank")
        void shouldThrowExceptionWhenNameIsBlank() {
            ProductRequest request = new ProductRequest("   ", "Desc", BigDecimal.TEN, 5, "Category");

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> productService.createProduct(request));
            assertEquals("Product name is required", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when price is negative")
        void shouldThrowExceptionWhenPriceIsNegative() {
            ProductRequest request = new ProductRequest("Test", "Desc", BigDecimal.valueOf(-10), 5, "Category");

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> productService.createProduct(request));
            assertEquals("Product price must be a non-negative number", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when quantity is negative")
        void shouldThrowExceptionWhenQuantityIsNegative() {
            ProductRequest request = new ProductRequest("Test", "Desc", BigDecimal.TEN, -5, "Category");

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> productService.createProduct(request));
            assertEquals("Product quantity must be a non-negative number", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Get Product Tests")
    class GetProductTests {

        @Test
        @DisplayName("Should get product by id successfully")
        void shouldGetProductById() {
            Product created = productService.createProduct(createValidRequest());

            Product found = productService.getProductById(created.getId());

            assertEquals(created.getId(), found.getId());
            assertEquals(created.getName(), found.getName());
        }

        @Test
        @DisplayName("Should throw exception when product not found")
        void shouldThrowExceptionWhenProductNotFound() {
            ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
                    () -> productService.getProductById(999L));
            assertTrue(exception.getMessage().contains("999"));
        }

        @Test
        @DisplayName("Should get all products")
        void shouldGetAllProducts() {
            productService.createProduct(createValidRequest());
            productService.createProduct(new ProductRequest("Product 2", "Desc", BigDecimal.ONE, 5, "Books"));

            List<Product> products = productService.getAllProducts();

            assertEquals(2, products.size());
        }
    }

    @Nested
    @DisplayName("Filter and Search Tests")
    class FilterAndSearchTests {

        @Test
        @DisplayName("Should filter products by category")
        void shouldFilterByCategory() {
            productService.createProduct(new ProductRequest("Laptop", "Gaming laptop", BigDecimal.valueOf(1500), 5, "Electronics"));
            productService.createProduct(new ProductRequest("Book", "Java book", BigDecimal.valueOf(50), 20, "Books"));
            productService.createProduct(new ProductRequest("Phone", "Smartphone", BigDecimal.valueOf(800), 10, "Electronics"));

            List<Product> electronics = productService.getProductsByCategory("Electronics");

            assertEquals(2, electronics.size());
            assertTrue(electronics.stream().allMatch(p -> "Electronics".equalsIgnoreCase(p.getCategory())));
        }

        @Test
        @DisplayName("Should get active products only")
        void shouldGetActiveProductsOnly() {
            Product active = productService.createProduct(createValidRequest());
            Product inactive = productService.createProduct(new ProductRequest("Inactive", "Desc", BigDecimal.TEN, 5, "Category"));
            productService.toggleProductStatus(inactive.getId());

            List<Product> activeProducts = productService.getActiveProducts();

            assertEquals(1, activeProducts.size());
            assertEquals(active.getId(), activeProducts.get(0).getId());
        }

        @Test
        @DisplayName("Should search products by name")
        void shouldSearchByName() {
            productService.createProduct(new ProductRequest("iPhone 15", "Latest iPhone", BigDecimal.valueOf(999), 10, "Electronics"));
            productService.createProduct(new ProductRequest("Samsung Galaxy", "Android phone", BigDecimal.valueOf(799), 15, "Electronics"));

            List<Product> results = productService.searchProducts("iPhone");

            assertEquals(1, results.size());
            assertTrue(results.get(0).getName().contains("iPhone"));
        }

        @Test
        @DisplayName("Should search products by description")
        void shouldSearchByDescription() {
            productService.createProduct(new ProductRequest("Product A", "Contains keyword here", BigDecimal.TEN, 5, "Cat"));
            productService.createProduct(new ProductRequest("Product B", "No match", BigDecimal.TEN, 5, "Cat"));

            List<Product> results = productService.searchProducts("keyword");

            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("Should return all products when search query is blank")
        void shouldReturnAllWhenSearchQueryBlank() {
            productService.createProduct(createValidRequest());
            productService.createProduct(new ProductRequest("Product 2", "Desc", BigDecimal.ONE, 5, "Cat"));

            List<Product> results = productService.searchProducts("   ");

            assertEquals(2, results.size());
        }
    }

    @Nested
    @DisplayName("Update Product Tests")
    class UpdateProductTests {

        @Test
        @DisplayName("Should update product successfully")
        void shouldUpdateProduct() {
            Product created = productService.createProduct(createValidRequest());
            ProductRequest updateRequest = new ProductRequest("Updated Name", "Updated Desc", BigDecimal.valueOf(199.99), 20, "New Category");

            Product updated = productService.updateProduct(created.getId(), updateRequest);

            assertEquals("Updated Name", updated.getName());
            assertEquals("Updated Desc", updated.getDescription());
            assertEquals(BigDecimal.valueOf(199.99), updated.getPrice());
            assertEquals(20, updated.getQuantity());
            assertEquals("New Category", updated.getCategory());
        }

        @Test
        @DisplayName("Should update quantity successfully")
        void shouldUpdateQuantity() {
            Product created = productService.createProduct(createValidRequest());

            Product updated = productService.updateProductQuantity(created.getId(), 50);

            assertEquals(50, updated.getQuantity());
        }

        @Test
        @DisplayName("Should throw exception when updating quantity with negative value")
        void shouldThrowExceptionForNegativeQuantity() {
            Product created = productService.createProduct(createValidRequest());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> productService.updateProductQuantity(created.getId(), -5));
            assertTrue(exception.getMessage().contains("non-negative"));
        }

        @Test
        @DisplayName("Should toggle product status")
        void shouldToggleStatus() {
            Product created = productService.createProduct(createValidRequest());
            assertTrue(created.isActive());

            Product toggled = productService.toggleProductStatus(created.getId());
            assertFalse(toggled.isActive());

            Product toggledAgain = productService.toggleProductStatus(created.getId());
            assertTrue(toggledAgain.isActive());
        }
    }

    @Nested
    @DisplayName("Delete Product Tests")
    class DeleteProductTests {

        @Test
        @DisplayName("Should delete product successfully")
        void shouldDeleteProduct() {
            Product created = productService.createProduct(createValidRequest());

            productService.deleteProduct(created.getId());

            assertThrows(ProductNotFoundException.class, () -> productService.getProductById(created.getId()));
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent product")
        void shouldThrowExceptionWhenDeletingNonExistent() {
            assertThrows(ProductNotFoundException.class, () -> productService.deleteProduct(999L));
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should count total products")
        void shouldCountTotalProducts() {
            productService.createProduct(createValidRequest());
            productService.createProduct(new ProductRequest("P2", "D", BigDecimal.ONE, 1, "C"));

            assertEquals(2, productService.countProducts());
        }

        @Test
        @DisplayName("Should count active products only")
        void shouldCountActiveProducts() {
            Product p1 = productService.createProduct(createValidRequest());
            productService.createProduct(new ProductRequest("P2", "D", BigDecimal.ONE, 1, "C"));
            productService.toggleProductStatus(p1.getId());

            assertEquals(2, productService.countProducts());
            assertEquals(1, productService.countActiveProducts());
        }
    }
}
