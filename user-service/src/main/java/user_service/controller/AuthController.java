package user_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import user_service.entity.User;
import user_service.service.UserService;
import user_service.util.JwtUtil;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User created = userService.createUser(user);
        return ResponseEntity.ok(Map.of(
                "message", "User registered successfully",
                "userId", created.getId()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        return userService.getUserByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(user -> {
                    String token = jwtUtil.generateToken(user.getId(), user.getUsername());
                    return ResponseEntity.ok(Map.of(
                            "token", token,
                            "userId", user.getId(),
                            "username", user.getUsername()
                    ));
                })
                .orElse(ResponseEntity.status(401).body(Map.of("error", "Invalid username or password")));
    }
}