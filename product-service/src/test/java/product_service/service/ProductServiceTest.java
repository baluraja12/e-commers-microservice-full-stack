package product_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import product_service.entity.Product;
import product_service.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@DisplayName("ProductService Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        productService = new ProductService(productRepository);
        
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(BigDecimal.valueOf(99.99));
        testProduct.setStockQuantity(10);
        testProduct.setCategory("Electronics");
    }

    @Test
    @DisplayName("Should create product successfully")
    void testCreateProduct() {
        // Arrange
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // Act
        Product result = productService.createProduct(testProduct);

        // Assert
        assertNotNull(result);
        assertEquals(testProduct.getId(), result.getId());
        assertEquals(testProduct.getName(), result.getName());
        assertEquals(BigDecimal.valueOf(99.99), result.getPrice());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should get all products successfully")
    void testGetAllProducts() {
        // Arrange
        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Product 2");
        List<Product> products = Arrays.asList(testProduct, product2);
        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<Product> result = productService.getAllProducts();

        // Assert
        assertEquals(2, result.size());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no products exist")
    void testGetAllProductsEmpty() {
        // Arrange
        when(productRepository.findAll()).thenReturn(List.of());

        // Act
        List<Product> result = productService.getAllProducts();

        // Assert
        assertTrue(result.isEmpty());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should get product by id successfully")
    void testGetProductById() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // Act
        Optional<Product> result = productService.getProductById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testProduct.getId(), result.get().getId());
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should return empty when product not found by id")
    void testGetProductByIdNotFound() {
        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Product> result = productService.getProductById(999L);

        // Assert
        assertFalse(result.isPresent());
        verify(productRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should get products by category successfully")
    void testGetProductsByCategory() {
        // Arrange
        List<Product> categoryProducts = List.of(testProduct);
        when(productRepository.findByCategory("Electronics")).thenReturn(categoryProducts);

        // Act
        List<Product> result = productService.getProductsByCategory("Electronics");

        // Assert
        assertEquals(1, result.size());
        assertEquals("Electronics", result.get(0).getCategory());
        verify(productRepository, times(1)).findByCategory("Electronics");
    }

    @Test
    @DisplayName("Should return empty list when no products found by category")
    void testGetProductsByCategoryEmpty() {
        // Arrange
        when(productRepository.findByCategory("NonExistent")).thenReturn(List.of());

        // Act
        List<Product> result = productService.getProductsByCategory("NonExistent");

        // Assert
        assertTrue(result.isEmpty());
        verify(productRepository, times(1)).findByCategory("NonExistent");
    }

    @Test
    @DisplayName("Should update product successfully")
    void testUpdateProduct() {
        // Arrange
        Product updatedProduct = new Product();
        updatedProduct.setId(1L);
        updatedProduct.setName("Updated Product");
        updatedProduct.setDescription("Updated Description");
        updatedProduct.setPrice(BigDecimal.valueOf(149.99));
        updatedProduct.setStockQuantity(20);
        updatedProduct.setCategory("Updated Category");

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        // Act
        Product result = productService.updateProduct(1L, updatedProduct);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Product", result.getName());
        assertEquals(BigDecimal.valueOf(149.99), result.getPrice());
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw exception when product to update not found")
    void testUpdateProductNotFound() {
        // Arrange
        Product updatedProduct = new Product();
        updatedProduct.setName("Updated");
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            productService.updateProduct(999L, updatedProduct);
        });
        verify(productRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should delete product successfully")
    void testDeleteProduct() {
        // Arrange
        doNothing().when(productRepository).deleteById(1L);

        // Act
        productService.deleteProduct(1L);

        // Assert
        verify(productRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should handle delete exception")
    void testDeleteProductException() {
        // Arrange
        doThrow(new RuntimeException("Delete failed")).when(productRepository).deleteById(999L);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            productService.deleteProduct(999L);
        });
        verify(productRepository, times(1)).deleteById(999L);
    }
}
