package api_gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/product")
    public Map<String, String> productFallback() {
        return Map.of(
                "status", "error",
                "service", "product-service",
                "message", "Product Service is temporarily unavailable. Please try again later."
        );
    }

    @GetMapping("/order")
    public Map<String, String> orderFallback() {
        return Map.of(
                "status", "error",
                "service", "order-service",
                "message", "Order Service is temporarily unavailable. Please try again later."
        );
    }

    @GetMapping("/user")
    public Map<String, String> userFallback() {
        return Map.of(
                "status", "error",
                "service", "user-service",
                "message", "User Service is temporarily unavailable. Please try again later."
        );
    }
}