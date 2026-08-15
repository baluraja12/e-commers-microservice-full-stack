# ============================================
# Cart Service Setup Script
# ============================================
$repo = "C:\Users\Public\E-commerse-microservices"
Set-Location $repo

# ---- 1. Create cart-service folder structure ----
$folders = @(
    "cart-service\src\main\java\cart_service\config",
    "cart-service\src\main\java\cart_service\controller",
    "cart-service\src\main\java\cart_service\model",
    "cart-service\src\main\java\cart_service\service",
    "cart-service\src\main\resources"
)
foreach ($f in $folders) { New-Item -ItemType Directory -Force -Path $f | Out-Null }

# ---- 2. CartServiceApplication.java ----
Set-Content -Path "cart-service\src\main\java\cart_service\CartServiceApplication.java" -Value @"
package cart_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class CartServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CartServiceApplication.class, args);
    }
}
"@

# ---- 3. model/CartItem.java ----
Set-Content -Path "cart-service\src\main\java\cart_service\model\CartItem.java" -Value @"
package cart_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}
"@

# ---- 4. model/Cart.java ----
Set-Content -Path "cart-service\src\main\java\cart_service\model\Cart.java" -Value @"
package cart_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
    private String userId;
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();
    private BigDecimal totalAmount;
    private Integer totalItems;

    public void recalculate() {
        this.totalItems = items.stream().mapToInt(CartItem::getQuantity).sum();
        this.totalAmount = items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
"@

# ---- 5. model/AddToCartRequest.java ----
Set-Content -Path "cart-service\src\main\java\cart_service\model\AddToCartRequest.java" -Value @"
package cart_service.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddToCartRequest {
    @NotNull(message = \"Product ID is required\")
    private Long productId;

    @NotNull(message = \"Quantity is required\")
    @Min(value = 1, message = \"Quantity must be at least 1\")
    private Integer quantity;
}
"@

# ---- 6. model/CheckoutEvent.java ----
Set-Content -Path "cart-service\src\main\java\cart_service\model\CheckoutEvent.java" -Value @"
package cart_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutEvent {
    private String userId;
    private List<CartItem> items;
    private BigDecimal totalAmount;
    private Long timestamp;
}
"@

Write-Host "Models created..." -ForegroundColor Green

# ---- 7. config/RedisConfig.java ----
Set-Content -Path "cart-service\src\main\java\cart_service\config\RedisConfig.java" -Value @"
package cart_service.config;

import cart_service.model.Cart;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Cart> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Cart> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<Cart> serializer = new Jackson2JsonRedisSerializer<>(mapper, Cart.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}
"@

# ---- 8. service/ProductClient.java ----
Set-Content -Path "cart-service\src\main\java\cart_service\service\ProductClient.java" -Value @"
package cart_service.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient(name = \"product-service\", url = ""${product-service.url}"")
public interface ProductClient {

    @GetMapping(""/api/products/{id}"")
    ProductDto getProduct(@PathVariable(""id"") Long id);

    record ProductDto(
            Long id,
            String name,
            String description,
            BigDecimal price,
            Integer stockQuantity,
            Boolean available
    ) {}
}
"@

# ---- 9. service/CartRepository.java ----
Set-Content -Path "cart-service\src\main\java\cart_service\service\CartRepository.java" -Value @"
package cart_service.service;

import cart_service.model.Cart;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CartRepository {

    private final RedisTemplate<String, Cart> redisTemplate;
    private static final String CART_PREFIX = ""cart:"";
    private static final Duration TTL = Duration.ofDays(7);

    private String key(String userId) {
        return CART_PREFIX + userId;
    }

    public Optional<Cart> findByUserId(String userId) {
        Cart cart = redisTemplate.opsForValue().get(key(userId));
        return Optional.ofNullable(cart);
    }

    public void save(String userId, Cart cart) {
        redisTemplate.opsForValue().set(key(userId), cart, TTL);
    }

    public void delete(String userId) {
        redisTemplate.delete(key(userId));
    }
}
"@

Write-Host "Config and Repository created..." -ForegroundColor Green

# ---- 10. service/CartService.java ----
Set-Content -Path "cart-service\src\main\java\cart_service\service\CartService.java" -Value @"
package cart_service.service;

import cart_service.model.AddToCartRequest;
import cart_service.model.Cart;
import cart_service.model.CartItem;
import cart_service.model.CheckoutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final ProductClient productClient;
    private final KafkaTemplate<String, CheckoutEvent> kafkaTemplate;

    private static final String CHECKOUT_TOPIC = ""cart.checkout"";

    public Cart getCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart emptyCart = new Cart();
                    emptyCart.setUserId(userId);
                    emptyCart.setItems(new ArrayList<>());
                    emptyCart.setTotalAmount(BigDecimal.ZERO);
                    emptyCart.setTotalItems(0);
                    return emptyCart;
                });
    }

    public Cart addItem(String userId, AddToCartRequest request) {
        ProductClient.ProductDto product = productClient.getProduct(request.getProductId());

        if (product == null || !Boolean.TRUE.equals(product.available())) {
            throw new RuntimeException(""Product not found or unavailable"");
        }

        if (product.stockQuantity() < request.getQuantity()) {
            throw new RuntimeException(""Insufficient stock. Available: "" + product.stockQuantity());
        }

        Cart cart = getCart(userId);

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQty = item.getQuantity() + request.getQuantity();
            if (newQty > product.stockQuantity()) {
                throw new RuntimeException(""Cannot add more than available stock"");
            }
            item.setQuantity(newQty);
            item.setSubtotal(product.price().multiply(BigDecimal.valueOf(newQty)));
        } else {
            CartItem newItem = CartItem.builder()
                    .productId(product.id())
                    .productName(product.name())
                    .quantity(request.getQuantity())
                    .unitPrice(product.price())
                    .subtotal(product.price().multiply(BigDecimal.valueOf(request.getQuantity())))
                    .build();
            cart.getItems().add(newItem);
        }

        cart.recalculate();
        cartRepository.save(userId, cart);
        log.info(""Added item to cart for user {}: productId={}, qty={}"", userId, request.getProductId(), request.getQuantity());
        return cart;
    }

    public Cart updateQuantity(String userId, Long productId, Integer quantity) {
        if (quantity < 1) {
            return removeItem(userId, productId);
        }

        Cart cart = getCart(userId);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(""Item not in cart""));

        ProductClient.ProductDto product = productClient.getProduct(productId);
        if (product.stockQuantity() < quantity) {
            throw new RuntimeException(""Insufficient stock. Available: "" + product.stockQuantity());
        }

        item.setQuantity(quantity);
        item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
        cart.recalculate();
        cartRepository.save(userId, cart);
        return cart;
    }

    public Cart removeItem(String userId, Long productId) {
        Cart cart = getCart(userId);
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        cart.recalculate();
        cartRepository.save(userId, cart);
        log.info(""Removed item from cart for user {}: productId={}"", userId, productId);
        return cart;
    }

    public void clearCart(String userId) {
        cartRepository.delete(userId);
        log.info(""Cleared cart for user {}"", userId);
    }

    public CheckoutEvent checkout(String userId) {
        Cart cart = getCart(userId);

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException(""Cannot checkout an empty cart"");
        }

        for (CartItem item : cart.getItems()) {
            ProductClient.ProductDto product = productClient.getProduct(item.getProductId());
            if (product.stockQuantity() < item.getQuantity()) {
                throw new RuntimeException(""Product '"" + product.name() + ""' is out of stock"");
            }
        }

        CheckoutEvent event = CheckoutEvent.builder()
                .userId(userId)
                .items(new ArrayList<>(cart.getItems()))
                .totalAmount(cart.getTotalAmount())
                .timestamp(System.currentTimeMillis())
                .build();

        kafkaTemplate.send(CHECKOUT_TOPIC, userId, event);
        log.info(""Checkout event published for user {}: total={}"", userId, cart.getTotalAmount());

        clearCart(userId);
        return event;
    }
}
"@

Write-Host "CartService created..." -ForegroundColor Green

# ---- 11. controller/CartController.java ----
Set-Content -Path "cart-service\src\main\java\cart_service\controller\CartController.java" -Value @"
package cart_service.controller;

import cart_service.model.AddToCartRequest;
import cart_service.model.Cart;
import cart_service.model.CheckoutEvent;
import cart_service.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(""/api/cart"")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping(""/{userId}"")
    public ResponseEntity<Cart> getCart(@PathVariable String userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping(""/{userId}/items"")
    public ResponseEntity<Cart> addItem(
            @PathVariable String userId,
            @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addItem(userId, request));
    }

    @PutMapping(""/{userId}/items/{productId}"")
    public ResponseEntity<Cart> updateQuantity(
            @PathVariable String userId,
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(cartService.updateQuantity(userId, productId, quantity));
    }

    @DeleteMapping(""/{userId}/items/{productId}"")
    public ResponseEntity<Cart> removeItem(
            @PathVariable String userId,
            @PathVariable Long productId) {
        return ResponseEntity.ok(cartService.removeItem(userId, productId));
    }

    @DeleteMapping(""/{userId}"")
    public ResponseEntity<Void> clearCart(@PathVariable String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(""/{userId}/checkout"")
    public ResponseEntity<CheckoutEvent> checkout(@PathVariable String userId) {
        return ResponseEntity.ok(cartService.checkout(userId));
    }
}
"@

# ---- 12. controller/GlobalExceptionHandler.java ----
Set-Content -Path "cart-service\src\main\java\cart_service\controller\GlobalExceptionHandler.java" -Value @"
package cart_service.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error(""Runtime exception: {}"", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error(""Unexpected error: {}"", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ""An unexpected error occurred"", LocalDateTime.now()));
    }

    public record ErrorResponse(int status, String message, LocalDateTime timestamp) {}
}
"@

Write-Host "Controllers created..." -ForegroundColor Green

# ---- 13. resources/application.yml ----
Set-Content -Path "cart-service\src\main\resources\application.yml" -Value @"
spring:
  application:
    name: cart-service
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
  jackson:
    serialization:
      write-dates-as-timestamps: false

server:
  port: 8084

---
spring:
  config:
    activate:
      on-profile: local
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true

product-service:
  url: http://product-service

---
spring:
  config:
    activate:
      on-profile: docker
  redis:
    host: ${REDIS_HOST:redis}
    port: ${REDIS_PORT:6379}
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:kafka:29092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE:http://eureka-server:8761/eureka}
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true

product-service:
  url: http://product-service
"@

Write-Host "application.yml created..." -ForegroundColor Green

# ---- 14. cart-service/pom.xml ----
Set-Content -Path "cart-service\pom.xml" -Value @"
<?xml version=""1.0"" encoding=""UTF-8""?>
<project xmlns=""http://maven.apache.org/POM/4.0.0""
         xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance""
         xsi:schemaLocation=""http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd"">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>ecommerce-microservices</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>cart-service</artifactId>
    <name>Cart Service</name>
    <description>Shopping cart microservice with Redis</description>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
"@

# ---- 15. cart-service/Dockerfile ----
Set-Content -Path "cart-service\Dockerfile" -Value @"
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT [""java"", ""-jar"", ""app.jar""]
"@

Write-Host "pom.xml and Dockerfile created..." -ForegroundColor Green

# ---- 16. Update root pom.xml (add cart-service module + Lombok processor) ----
$parentPom = @"
<?xml version=""1.0"" encoding=""UTF-8""?>
<project xmlns=""http://maven.apache.org/POM/4.0.0""
         xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance""
         xsi:schemaLocation=""http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd"">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>ecommerce-microservices</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <name>E-Commerce Microservices Parent</name>

    <modules>
        <module>config-server</module>
        <module>eureka-server</module>
        <module>api-gateway</module>
        <module>product-service</module>
        <module>order-service</module>
        <module>user-service</module>
        <module>cart-service</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring-boot.version>3.2.5</spring-boot.version>
        <spring-cloud.version>2023.0.1</spring-cloud.version>
        <lombok.version>1.18.30</lombok.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                    <executions>
                        <execution>
                            <goals>
                                <goal>repackage</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.11.0</version>
                    <configuration>
                        <release>21</release>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                                <version>${lombok.version}</version>
                            </path>
                        </annotationProcessorPaths>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>

    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
"@
Set-Content -Path "pom.xml" -Value $parentPom

Write-Host "Root pom.xml updated with Lombok processor..." -ForegroundColor Green

# ---- 17. Update docker-compose.yml ----
$dockerCompose = @"
version: '3.8'

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - ""2181:2181""
    networks:
      - ecommerce-net

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: kafka
    depends_on:
      - zookeeper
    ports:
      - ""9092:9092""
      - ""29092:29092""
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    networks:
      - ecommerce-net

  redis:
    image: redis:7-alpine
    container_name: redis
    ports:
      - ""6379:6379""
    networks:
      - ecommerce-net
    healthcheck:
      test: [""CMD"", ""redis-cli"", ""ping""]
      interval: 10s
      timeout: 3s
      retries: 5

  mysql:
    image: mysql:8.0
    container_name: mysql
    ports:
      - ""3307:3306""
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ecommerce
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - ecommerce-net
    healthcheck:
      test: [""CMD-SHELL"", ""mysqladmin ping -h localhost -p${MYSQL_ROOT_PASSWORD} || exit 1""]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s

  zipkin:
    image: openzipkin/zipkin:latest
    container_name: zipkin
    ports:
      - ""9411:9411""
    networks:
      - ecommerce-net

  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - ""9090:9090""
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro
    networks:
      - ecommerce-net

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - ""3000:3000""
    environment:
      GF_SECURITY_ADMIN_USER: ${GRAFANA_USER:-admin}
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD:-admin}
    volumes:
      - grafana_data:/var/lib/grafana
    networks:
      - ecommerce-net

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.17.0
    container_name: elasticsearch
    environment:
      discovery.type: single-node
      ES_JAVA_OPTS: ""-Xms512m -Xmx512m""
      xpack.security.enabled: ""false""
    ports:
      - ""9200:9200""
    volumes:
      - es_data:/usr/share/elasticsearch/data
    networks:
      - ecommerce-net
    healthcheck:
      test: [""CMD-SHELL"", ""curl -f http://localhost:9200/_cluster/health || exit 1""]
      interval: 30s
      timeout: 10s
      retries: 5

  logstash:
    image: docker.elastic.co/logstash/logstash:7.17.0
    container_name: logstash
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf:ro
    ports:
      - ""5000:5000""
    depends_on:
      elasticsearch:
        condition: service_healthy
    networks:
      - ecommerce-net

  kibana:
    image: docker.elastic.co/kibana/kibana:7.17.0
    container_name: kibana
    ports:
      - ""5601:5601""
    environment:
      ELASTICSEARCH_HOSTS: http://elasticsearch:9200
    depends_on:
      elasticsearch:
        condition: service_healthy
    networks:
      - ecommerce-net

  config-server:
    build: ./config-server
    container_name: config-server
    ports:
      - ""8888:8888""
    environment:
      SPRING_PROFILES_ACTIVE: docker
    healthcheck:
      test: [""CMD-SHELL"", ""bash -c 'cat < /dev/null > /dev/tcp/localhost/8888'""]
      interval: 15s
      timeout: 10s
      retries: 10
      start_period: 90s
    networks:
      - ecommerce-net

  eureka-server:
    build: ./eureka-server
    container_name: eureka-server
    ports:
      - ""8761:8761""
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_CLOUD_CONFIG_URI: http://config-server:8888
      EUREKA_CLIENT_REGISTER_WITH_EUREKA: ""false""
      EUREKA_CLIENT_FETCH_REGISTRY: ""false""
    healthcheck:
      test: [""CMD-SHELL"", ""bash -c 'cat < /dev/null > /dev/tcp/localhost/8761'""]
      interval: 30s
      timeout: 10s
      retries: 15
      start_period: 120s
    depends_on:
      config-server:
        condition: service_healthy
    networks:
      - ecommerce-net

  api-gateway:
    build: ./api-gateway
    container_name: api-gateway
    ports:
      - ""8080:8080""
    environment:
      SPRING_PROFILES_ACTIVE: docker
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://eureka-server:8761/eureka
      JWT_SECRET: ${JWT_SECRET}
      REDIS_HOST: redis
      REDIS_PORT: 6379
    depends_on:
      redis:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
      config-server:
        condition: service_healthy
    networks:
      - ecommerce-net

  product-service:
    build: ./product-service
    container_name: product-service
    ports:
      - ""8081:8081""
    environment:
      SPRING_PROFILES_ACTIVE: docker
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://eureka-server:8761/eureka
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/productdb?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    depends_on:
      mysql:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
      kafka:
        condition: service_started
    networks:
      - ecommerce-net

  order-service:
    build: ./order-service
    container_name: order-service
    ports:
      - ""8082:8082""
    environment:
      SPRING_PROFILES_ACTIVE: docker
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://eureka-server:8761/eureka
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/orderdb?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    depends_on:
      mysql:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
      kafka:
        condition: service_started
    networks:
      - ecommerce-net

  user-service:
    build: ./user-service
    container_name: user-service
    ports:
      - ""8083:8083""
    environment:
      SPRING_PROFILES_ACTIVE: docker
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://eureka-server:8761/eureka
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/userdb?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      mysql:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
    networks:
      - ecommerce-net

  cart-service:
    build: ./cart-service
    container_name: cart-service
    ports:
      - ""8084:8084""
    environment:
      SPRING_PROFILES_ACTIVE: docker
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://eureka-server:8761/eureka
      REDIS_HOST: redis
      REDIS_PORT: 6379
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    depends_on:
      redis:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
      kafka:
        condition: service_started
    networks:
      - ecommerce-net

volumes:
  mysql_data:
  grafana_data:
  es_data:

networks:
  ecommerce-net:
    driver: bridge
"@
Set-Content -Path "docker-compose.yml" -Value $dockerCompose

# ---- 18. Update prometheus.yml ----
$prometheus = @"
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'api-gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['api-gateway:8080']

  - job_name: 'product-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['product-service:8081']

  - job_name: 'order-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['order-service:8082']

  - job_name: 'user-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['user-service:8083']

  - job_name: 'cart-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['cart-service:8084']

  - job_name: 'eureka-server'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['eureka-server:8761']

  - job_name: 'config-server'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['config-server:8888']
"@
Set-Content -Path "prometheus.yml" -Value $prometheus

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ALL FILES CREATED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next step: Build the project" -ForegroundColor Yellow
Write-Host "Run: .\mvnw clean install -pl cart-service -am" -ForegroundColor White
