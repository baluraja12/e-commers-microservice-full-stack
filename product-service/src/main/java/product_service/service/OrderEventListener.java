package product_service.service;

import product_service.dto.OrderEvent;
import product_service.dto.OrderItemEvent;
import product_service.entity.Product;
import product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final ProductRepository productRepository;

@KafkaListener(topics = "order-events", groupId = "product-service-group")
public void handleOrderCreated(OrderEvent event) {
    for (OrderItemEvent item : event.getItems()) {
        productRepository.findById(item.getProductId())
            .ifPresent(product -> {
                int oldStock = product.getStockQuantity(); // Capture BEFORE change
                int newStock = oldStock - item.getQuantity();
                if (newStock >= 0) {
                    product.setStockQuantity(newStock);
                    productRepository.save(product);
                    log.info("Stock updated for Product {}: {} -> {} (ordered: {})",
                        item.getProductId(), oldStock, newStock, item.getQuantity());
                } else {
                    log.warn("Insufficient stock for Product {}! Required: {}, Available: {}",
                        item.getProductId(), item.getQuantity(), oldStock);
                }
            });
    }
}
}
