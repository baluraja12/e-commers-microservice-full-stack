package order_service.service;

import order_service.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;  // Changed to Object
    private static final String TOPIC = "order-events";

    public void publishOrderCreated(OrderEvent event) {
        log.info("Publishing order event to Kafka: OrderId={}", event.getOrderId());
        kafkaTemplate.send(TOPIC, event.getOrderId().toString(), event);
    }
}