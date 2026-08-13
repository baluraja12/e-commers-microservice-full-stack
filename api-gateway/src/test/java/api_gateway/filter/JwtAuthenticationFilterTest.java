package api_gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    private JwtAuthenticationFilter filter;
    private String validToken;
    private String secretKey;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new JwtAuthenticationFilter();
        secretKey = "my-super-secret-key-my-super-secret-key";
        ReflectionTestUtils.setField(filter, "secretKey", secretKey);

        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(chain.filter(any())).thenReturn(Mono.empty());
        when(response.setComplete()).thenReturn(Mono.empty());

        // Generate a valid token
        validToken = generateValidToken("1", "testuser", "USER");
    }

    private String generateValidToken(String userId, String username, String role) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("Should pass through OPTIONS request (CORS preflight)")
    void testOptionsRequest() {
        // Arrange
        when(request.getMethod()).thenReturn(HttpMethod.OPTIONS);
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080/api/users/login"));

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        assertNotNull(result);
        verify(chain, times(1)).filter(any());
    }

    @Test
    @DisplayName("Should allow public endpoint /api/users/login")
    void testPublicEndpointLogin() {
        // Arrange
        when(request.getMethod()).thenReturn(HttpMethod.POST);
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080/api/users/login"));

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        assertNotNull(result);
        verify(chain, times(1)).filter(exchange);
    }

    @Test
    @DisplayName("Should allow public endpoint /api/users/register")
    void testPublicEndpointRegister() {
        // Arrange
        when(request.getMethod()).thenReturn(HttpMethod.POST);
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080/api/users/register"));

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        assertNotNull(result);
        verify(chain, times(1)).filter(exchange);
    }

    @Test
    @DisplayName("Should allow public endpoint /api/products")
    void testPublicEndpointProducts() {
        // Arrange
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080/api/products"));

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        assertNotNull(result);
        verify(chain, times(1)).filter(exchange);
    }

    @Test
    @DisplayName("Should allow public endpoint /actuator/**")
    void testPublicEndpointActuator() {
        // Arrange
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080/actuator/health"));

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        assertNotNull(result);
        verify(chain, times(1)).filter(exchange);
    }

    @Test
    @DisplayName("Should reject request with missing Authorization header")
    void testMissingAuthorizationHeader() {
        // Arrange
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080/api/orders"));
        when(request.getHeaders()).thenReturn(org.springframework.http.HttpHeaders.EMPTY);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        assertNotNull(result);
        verify(response, times(1)).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response, times(1)).setComplete();
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Should reject request with missing Bearer prefix")
    void testMissingBearerPrefix() {
        // Arrange
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("Authorization", "InvalidToken");
        
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080/api/orders"));
        when(request.getHeaders()).thenReturn(headers);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        assertNotNull(result);
        verify(response, times(1)).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response, times(1)).setComplete();
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Should reject request with invalid JWT token")
    void testInvalidJwtToken() {
        // Arrange
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("Authorization", "Bearer invalid_token_here");
        
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080/api/orders"));
        when(request.getHeaders()).thenReturn(headers);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        assertNotNull(result);
        verify(response, times(1)).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response, times(1)).setComplete();
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Should reject request with expired JWT token")
    void testExpiredJwtToken() {
        // Arrange
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", "testuser");
        claims.put("role", "USER");

        String expiredToken = Jwts.builder()
                .setClaims(claims)
                .setSubject("1")
                .setIssuedAt(new Date(System.currentTimeMillis() - 3600000))
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(key)
                .compact();

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("Authorization", "Bearer " + expiredToken);
        
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080/api/orders"));
        when(request.getHeaders()).thenReturn(headers);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        assertNotNull(result);
        verify(response, times(1)).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response, times(1)).setComplete();
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Should return correct filter order")
    void testGetOrder() {
        // Act
        int order = filter.getOrder();

        // Assert
        assertEquals(-100, order);
    }
}
