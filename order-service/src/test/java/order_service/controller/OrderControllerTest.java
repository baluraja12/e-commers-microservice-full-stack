package order_service.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import order_service.dto.OrderStatusUpdateRequest;
import order_service.entity.Order;
import order_service.entity.OrderItem;
import order_service.entity.OrderStatus;
import order_service.service.OrderService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@DisplayName("OrderController Tests")
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    private OrderController orderController;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderController = new OrderController(orderService);

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUserId(1L);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setTotalAmount(BigDecimal.valueOf(199.98));

        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setQuantity(2);
        item.setUnitPrice(BigDecimal.valueOf(99.99));
        testOrder.setOrderItems(List.of(item));
    }

    @Test
    @DisplayName("Should create order and return 201 CREATED")
    void testCreateOrder() {
        // Arrange
        when(orderService.createOrder(any(Order.class))).thenReturn(testOrder);

        // Act
        ResponseEntity<Order> response = orderController.createOrder(testOrder);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testOrder.getId(), response.getBody().getId());
        verify(orderService, times(1)).createOrder(any(Order.class));
    }

    @Test
    @DisplayName("Should get all orders successfully")
    void testGetAllOrders() {
        // Arrange
        Order order2 = new Order();
        order2.setId(2L);
        order2.setUserId(2L);
        List<Order> orders = Arrays.asList(testOrder, order2);
        when(orderService.getAllOrders()).thenReturn(orders);

        // Act
        ResponseEntity<List<Order>> response = orderController.getAllOrders();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        verify(orderService, times(1)).getAllOrders();
    }

    @Test
    @DisplayName("Should return empty list when no orders exist")
    void testGetAllOrdersEmpty() {
        // Arrange
        when(orderService.getAllOrders()).thenReturn(List.of());

        // Act
        ResponseEntity<List<Order>> response = orderController.getAllOrders();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(orderService, times(1)).getAllOrders();
    }

    @Test
    @DisplayName("Should get order by id successfully")
    void testGetOrderById() {
        // Arrange
        when(orderService.getOrderById(1L)).thenReturn(testOrder);

        // Act
        ResponseEntity<Order> response = orderController.getOrderById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testOrder.getId(), response.getBody().getId());
        verify(orderService, times(1)).getOrderById(1L);
    }

    @Test
    @DisplayName("Should get orders by user id successfully")
    void testGetOrdersByUser() {
        // Arrange
        List<Order> userOrders = List.of(testOrder);
        when(orderService.getOrdersByUserId(1L)).thenReturn(userOrders);

        // Act
        ResponseEntity<List<Order>> response = orderController.getOrdersByUser(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(orderService, times(1)).getOrdersByUserId(1L);
    }

    @Test
    @DisplayName("Should return empty list when user has no orders")
    void testGetOrdersByUserNoOrders() {
        // Arrange
        when(orderService.getOrdersByUserId(999L)).thenReturn(List.of());

        // Act
        ResponseEntity<List<Order>> response = orderController.getOrdersByUser(999L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(orderService, times(1)).getOrdersByUserId(999L);
    }

    @Test
    @DisplayName("Should update order status successfully")
    void testUpdateOrderStatus() {
        // Arrange
        Order updatedOrder = new Order();
        updatedOrder.setId(1L);
        updatedOrder.setStatus(OrderStatus.SHIPPED);
        
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest();
        request.setStatus(OrderStatus.SHIPPED);

        when(orderService.updateOrderStatus(1L, OrderStatus.SHIPPED)).thenReturn(updatedOrder);

        // Act
        ResponseEntity<Order> response = orderController.updateOrderStatus(1L, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(OrderStatus.SHIPPED, response.getBody().getStatus());
        verify(orderService, times(1)).updateOrderStatus(1L, OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("Should transition order from PENDING to CONFIRMED")
    void testUpdateOrderStatusPendingToConfirmed() {
        // Arrange
        Order updatedOrder = new Order();
        updatedOrder.setId(1L);
        updatedOrder.setStatus(OrderStatus.CONFIRMED);
        
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest();
        request.setStatus(OrderStatus.CONFIRMED);

        when(orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED)).thenReturn(updatedOrder);

        // Act
        ResponseEntity<Order> response = orderController.updateOrderStatus(1L, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(OrderStatus.CONFIRMED, response.getBody().getStatus());
        verify(orderService, times(1)).updateOrderStatus(1L, OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Should transition order from CONFIRMED to SHIPPED")
    void testUpdateOrderStatusConfirmedToShipped() {
        // Arrange
        Order updatedOrder = new Order();
        updatedOrder.setId(1L);
        updatedOrder.setStatus(OrderStatus.SHIPPED);
        
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest();
        request.setStatus(OrderStatus.SHIPPED);

        when(orderService.updateOrderStatus(1L, OrderStatus.SHIPPED)).thenReturn(updatedOrder);

        // Act
        ResponseEntity<Order> response = orderController.updateOrderStatus(1L, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(OrderStatus.SHIPPED, response.getBody().getStatus());
        verify(orderService, times(1)).updateOrderStatus(1L, OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("Should delete order and return 204 NO CONTENT")
    void testDeleteOrder() {
        // Arrange
        doNothing().when(orderService).deleteOrder(1L);

        // Act
        ResponseEntity<Void> response = orderController.deleteOrder(1L);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(orderService, times(1)).deleteOrder(1L);
    }

    @Test
    @DisplayName("Should handle delete order when order does not exist")
    void testDeleteOrderNotFound() {
        // Arrange
        doThrow(new RuntimeException("Order not found")).when(orderService).deleteOrder(999L);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            orderController.deleteOrder(999L);
        });
        verify(orderService, times(1)).deleteOrder(999L);
    }
}
