package order_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import order_service.entity.OrderStatus;

@Data
public class OrderStatusUpdateRequest {
    @NotNull(message = "Status is required")
    private OrderStatus status;
}