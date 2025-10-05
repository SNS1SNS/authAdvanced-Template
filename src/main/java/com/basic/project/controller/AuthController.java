package com.basic.project.controller;


import com.basic.project.dto.AuthDtos;
import com.basic.project.exception.AuthException;
import com.basic.project.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API для аутентификации и авторизации")
@Validated
@Slf4j
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Регистрация нового пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно зарегистрирован"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "409", description = "Пользователь уже существует")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthDtos.TokenResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest registerRequest) {
        try {
            AuthDtos.TokenResponse tokenResponse = authService.register(registerRequest);
            return ResponseEntity.ok(tokenResponse);
        }
        catch (AuthException e) {
            log.warn(e.getMessage());
            throw e;
        }
    }

    @Operation(summary = "Вход в систему")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный вход"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "401", description = "Неверные учетные данные")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthDtos.TokenResponse> login(@Valid @RequestBody AuthDtos.LoginRequest loginRequest) {
        try {
            AuthDtos.TokenResponse tokenResponse = authService.login(loginRequest);

            return ResponseEntity.ok(tokenResponse);
        }
        catch (AuthException e) {
            log.warn(e.getMessage());
            throw e;
        }
    }


    @Operation(summary = "Обновление токена")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Токен успешно обновлен"),
            @ApiResponse(responseCode = "400", description = "Некорректный refresh токен"),
            @ApiResponse(responseCode = "401", description = "Недействительный refresh токен")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthDtos.TokenResponse> refresh(@Valid @RequestBody AuthDtos.RefreshRequest refreshRequest) {
        log.info("Обновление токена для пользователя");
        try {
            AuthDtos.TokenResponse response = authService.refresh(refreshRequest);
            log.info("Токен успешно обновлен");
            return ResponseEntity.ok(response);
        } catch (AuthException e) {
            log.warn("Ошибка при обновлении токена: {}", e.getMessage());
            throw e;
        }
    }

    @Operation(summary = "Валидация токена")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Токен валиден"),
            @ApiResponse(responseCode = "401", description = "Токен недействителен")
    })
    @PostMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestHeader("Authorization") String authorization) {
        log.debug("Валидация токена");
        try {
            String token = authorization.replace("Bearer ", "");
            boolean isValid = authService.validateToken(token);
            return ResponseEntity.ok(isValid);
        } catch (Exception e) {
            log.warn("Ошибка при валидации токена: {}", e.getMessage());
            return ResponseEntity.ok(false);
        }
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<AuthDtos.ErrorResponse> handleAuthException(AuthException e) {
        log.error("Ошибка аутентификации: {}", e.getMessage());
        AuthDtos.ErrorResponse errorResponse = new AuthDtos.ErrorResponse(
                e.getErrorCode(),
                e.getMessage(),
                System.currentTimeMillis(),
                e.getHttpStatus().value()
        );
        return ResponseEntity.status(e.getHttpStatus()).body(errorResponse);
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AuthDtos.ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Некорректные данные: {}", e.getMessage());
        AuthDtos.ErrorResponse errorResponse = new AuthDtos.ErrorResponse(
                "INVALID_INPUT",
                e.getMessage(),
                System.currentTimeMillis(),
                HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
