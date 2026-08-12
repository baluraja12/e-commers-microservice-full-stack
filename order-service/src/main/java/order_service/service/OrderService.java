package order_service.service;

import order_service.client.ProductDto;
import order_service.client.ProductServiceClient;
import order_service.client.UserDto;
import order_service.client.UserServiceClient;
import order_service.dto.OrderEvent;
import order_service.dto.OrderItemEvent;
import order_service.entity.Order;
import order_service.entity.OrderStatus;
import order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;
    private final ProductServiceClient productServiceClient;
    private final UserServiceClient userServiceClient;

    @Transactional
    public Order createOrder(Order order) {
        // 1. Validate User exists
        log.info("Validating user with ID: {}", order.getUserId());
        UserDto user = userServiceClient.getUserById(order.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("User not found with id: " + order.getUserId());
        }

        // 2. Validate Products exist and have stock
        for (var item : order.getOrderItems()) {
            log.info("Validating product with ID: {}", item.getProductId());
            ProductDto product = productServiceClient.getProductById(item.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("Product not found with id: " + item.getProductId());
            }
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new IllegalStateException(
                        String.format("Insufficient stock for product: %s. Available: %d, Requested: %d",
                                product.getName(), product.getStockQuantity(), item.getQuantity())
                );
            }
            // Update item price from actual product price
            item.setUnitPrice(product.getPrice());
            item.setProductName(product.getName());
        }

        // 3. Calculate total
        BigDecimal total = order.getOrderItems().stream()
                .map(item -> {
                    item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                    return item.getSubtotal();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PENDING); // Always set initial status
        Order savedOrder = orderRepository.save(order);

        // 4. Publish event to Kafka (async stock update)
        OrderEvent event = OrderEvent.builder()
                .orderId(savedOrder.getId())
                .userId(savedOrder.getUserId())
                .totalAmount(savedOrder.getTotalAmount())
                .status(savedOrder.getStatus().name())
                .timestamp(LocalDateTime.now())
                .items(savedOrder.getOrderItems().stream()
                        .map(item -> OrderItemEvent.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .build())
                        .collect(Collectors.toList()))
                .build();

        eventPublisher.publishOrderCreated(event);
        log.info("Order created successfully: orderId={}", savedOrder.getId());

        return savedOrder;
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @Transactional
    public Order updateOrderStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + id));

        // Validate status transition
        if (!isValidStatusTransition(order.getStatus(), status)) {
            throw new IllegalStateException(
                    String.format("Invalid status transition from %s to %s", order.getStatus(), status)
            );
        }

        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Transactional
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new IllegalArgumentException("Order not found with id: " + id);
        }
        orderRepository.deleteById(id);
    }

    private boolean isValidStatusTransition(OrderStatus current, OrderStatus next) {
        return switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
            case SHIPPED -> next == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}