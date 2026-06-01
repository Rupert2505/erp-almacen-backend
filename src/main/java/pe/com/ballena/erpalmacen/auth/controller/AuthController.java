package pe.com.ballena.erpalmacen.auth.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.ballena.erpalmacen.auth.dto.AuthUserResponse;
import pe.com.ballena.erpalmacen.auth.dto.LoginRequest;
import pe.com.ballena.erpalmacen.auth.dto.LoginResponse;
import pe.com.ballena.erpalmacen.auth.service.AuthService;
import pe.com.ballena.erpalmacen.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Login exitoso", authService.login(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthUserResponse>> me(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(authService.getAuthenticatedUser(authentication)));
    }
}
