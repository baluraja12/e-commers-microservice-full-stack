package user_service.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import user_service.entity.User;
import user_service.service.UserService;
import user_service.util.JwtUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    private AuthController authController;
    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authController = new AuthController(userService, jwtUtil, passwordEncoder);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("$2a$10$encoded_password_hash");
    }

    @Test
    @DisplayName("Should register user successfully")
    void testRegisterSuccess() {
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("plainpassword");

        User createdUser = new User();
        createdUser.setId(2L);
        createdUser.setUsername("newuser");
        createdUser.setEmail("new@example.com");

        when(passwordEncoder.encode("plainpassword")).thenReturn("$2a$10$encoded_password_hash");
        when(userService.createUser(any(User.class))).thenReturn(createdUser);

        ResponseEntity<Map<String, Object>> response = authController.register(newUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User registered successfully", response.getBody().get("message"));
        assertEquals(2L, response.getBody().get("userId"));
        verify(userService, times(1)).createUser(any(User.class));
    }

    @Test
    @DisplayName("Should return error when password is null")
    void testRegisterPasswordNull() {
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword(null);

        ResponseEntity<Map<String, Object>> response = authController.register(newUser);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Password is required", response.getBody().get("error"));
        verify(userService, never()).createUser(any(User.class));
    }

    @Test
    @DisplayName("Should return error when password is blank")
    void testRegisterPasswordBlank() {
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword(" ");

        ResponseEntity<Map<String, Object>> response = authController.register(newUser);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Password is required", response.getBody().get("error"));
        verify(userService, never()).createUser(any(User.class));
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLoginSuccess() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "testuser");
        credentials.put("password", "plainpassword");

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("plainpassword", "$2a$10$encoded_password_hash")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "testuser")).thenReturn("jwt_token_here");

        ResponseEntity<Map<String, Object>> response = authController.login(credentials);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("jwt_token_here", response.getBody().get("token"));
        assertEquals(1L, response.getBody().get("userId"));
        assertEquals("testuser", response.getBody().get("username"));
        verify(userService, times(1)).getUserByUsername("testuser");
    }

    @Test
    @DisplayName("Should return 401 when user not found")
    void testLoginUserNotFound() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "nonexistent");
        credentials.put("password", "password");

        when(userService.getUserByUsername("nonexistent")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = authController.login(credentials);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid username or password", response.getBody().get("error"));
        verify(jwtUtil, never()).generateToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should return 401 when password is incorrect")
    void testLoginInvalidPassword() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "testuser");
        credentials.put("password", "wrongpassword");

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "$2a$10$encoded_password_hash")).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = authController.login(credentials);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid username or password", response.getBody().get("error"));
        verify(jwtUtil, never()).generateToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should return error when username is null")
    void testLoginUsernameNull() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("password", "password");

        ResponseEntity<Map<String, Object>> response = authController.login(credentials);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Username and password are required", response.getBody().get("error"));
        verify(userService, never()).getUserByUsername(anyString());
    }

    @Test
    @DisplayName("Should return error when password is null")
    void testLoginPasswordNull() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "testuser");

        ResponseEntity<Map<String, Object>> response = authController.login(credentials);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Username and password are required", response.getBody().get("error"));
        verify(userService, never()).getUserByUsername(anyString());
    }

    @Test
    @DisplayName("Should return error when username is blank")
    void testLoginUsernameBlank() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", " ");
        credentials.put("password", "password");

        ResponseEntity<Map<String, Object>> response = authController.login(credentials);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Username and password are required", response.getBody().get("error"));
        verify(userService, never()).getUserByUsername(anyString());
    }

    @Test
    @DisplayName("Should return error when password is blank")
    void testLoginPasswordBlank() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "testuser");
        credentials.put("password", " ");

        ResponseEntity<Map<String, Object>> response = authController.login(credentials);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Username and password are required", response.getBody().get("error"));
        verify(userService, never()).getUserByUsername(anyString());
    }
}