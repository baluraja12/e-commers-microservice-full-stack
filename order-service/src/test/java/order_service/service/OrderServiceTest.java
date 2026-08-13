package order_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import order_service.client.ProductDto;
import order_service.client.ProductServiceClient;
import order_service.client.UserDto;
import order_service.client.UserServiceClient;
import order_service.dto.OrderEvent;
import order_service.entity.Order;
import order_service.entity.OrderItem;
import order_service.entity.OrderStatus;
import order_service.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@DisplayName("OrderService Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    private OrderService orderService;
    private Order testOrder;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderService = new OrderService(orderRepository, orderEventPublisher, productServiceClient, userServiceClient);
        
        testOrderItem = new OrderItem();
        testOrderItem.setProductId(1L);
        testOrderItem.setProductName("Test Product");
        testOrderItem.setQuantity(2);
        testOrderItem.setUnitPrice(BigDecimal.valueOf(99.99));
        testOrderItem.setSubtotal(BigDecimal.valueOf(199.98));
        
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUserId(1L);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setTotalAmount(BigDecimal.valueOf(199.98));
        testOrder.setOrderItems(List.of(testOrderItem));
    }

    @Test
    @DisplayName("Should create order successfully with valid data")
    void testCreateOrderSuccess() {
        // Arrange
        ProductDto product = new ProductDto();
        product.setId(1L);
        product.setStockQuantity(10);
        product.setPrice(BigDecimal.valueOf(99.99));
        
        UserDto user = new UserDto();
        user.setId(1L);
        
        when(userServiceClient.getUserById(1L)).thenReturn(user);
        when(productServiceClient.getProductById(1L)).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(orderEventPublisher).publishOrderCreated(any(OrderEvent.class));

        // Act
        Order result = orderService.createOrder(testOrder);

        // Assert
        assertNotNull(result);
        assertEquals(testOrder.getId(), result.getId());
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderEventPublisher, times(1)).publishOrderCreated(any(OrderEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found during order creation")
    void testCreateOrderUserNotFound() {
        // Arrange
        when(userServiceClient.getUserById(999L)).thenReturn(null);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            testOrder.setUserId(999L);
            orderService.createOrder(testOrder);
        });
    }

    @Test
    @DisplayName("Should throw exception when product not found during order creation")
    void testCreateOrderProductNotFound() {
        // Arrange
        UserDto user = new UserDto();
        user.setId(1L);
        
        when(userServiceClient.getUserById(1L)).thenReturn(user);
        when(productServiceClient.getProductById(anyLong())).thenReturn(null);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            orderService.createOrder(testOrder);
        });
    }

    @Test
    @DisplayName("Should throw exception when product has insufficient stock")
    void testCreateOrderInsufficientStock() {
        // Arrange
        ProductDto product = new ProductDto();
        product.setId(1L);
        product.setStockQuantity(1); // Only 1 in stock, but order requests 2
        product.setPrice(BigDecimal.valueOf(99.99));
        
        UserDto user = new UserDto();
        user.setId(1L);
        
        when(userServiceClient.getUserById(1L)).thenReturn(user);
        when(productServiceClient.getProductById(1L)).thenReturn(product);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            orderService.createOrder(testOrder);
        });
    }

    @Test
    @DisplayName("Should get order by id successfully")
    void testGetOrderById() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act
        Order result = orderService.getOrderById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testOrder.getId(), result.getId());
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when order not found by id")
    void testGetOrderByIdNotFound() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(Exception.class, () -> {
            orderService.getOrderById(999L);
        });
    }

    @Test
    @DisplayName("Should get all orders successfully")
    void testGetAllOrders() {
        // Arrange
        List<Order> orders = List.of(testOrder);
        when(orderRepository.findAll()).thenReturn(orders);

        // Act
        List<Order> result = orderService.getAllOrders();

        // Assert
        assertEquals(1, result.size());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no orders exist")
    void testGetAllOrdersEmpty() {
        // Arrange
        when(orderRepository.findAll()).thenReturn(List.of());

        // Act
        List<Order> result = orderService.getAllOrders();

        // Assert
        assertTrue(result.isEmpty());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should get orders by user id successfully")
    void testGetOrdersByUserId() {
        // Arrange
        List<Order> userOrders = List.of(testOrder);
        when(orderRepository.findByUserId(1L)).thenReturn(userOrders);

        // Act
        List<Order> result = orderService.getOrdersByUserId(1L);

        // Assert
        assertEquals(1, result.size());
        verify(orderRepository, times(1)).findByUserId(1L);
    }

    @Test
    @DisplayName("Should update order status successfully")
    void testUpdateOrderStatus() {
        // Arrange
        Order updatedOrder = new Order();
        updatedOrder.setId(1L);
        updatedOrder.setStatus(OrderStatus.CONFIRMED);
        
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);

        // Act
        Order result = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Should delete order successfully")
    void testDeleteOrder() {
        // Arrange
        when(orderRepository.existsById(1L)).thenReturn(true);
        doNothing().when(orderRepository).deleteById(1L);

        // Act
        orderService.deleteOrder(1L);

        // Assert
        verify(orderRepository, times(1)).existsById(1L);
        verify(orderRepository, times(1)).deleteById(1L);
    }
}
