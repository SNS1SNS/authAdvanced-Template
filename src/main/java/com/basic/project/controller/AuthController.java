package com.basic.project.controller;


import com.basic.project.dto.AuthDtos;
import com.basic.project.exception.AuthException;
import com.basic.project.service.AuthService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
    private final Bucket loginRateLimitBucket;
    private final Bucket registerRateLimitBucket;
    private final Bucket refreshRateLimitBucket;

    @Operation(summary = "Регистрация нового пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно зарегистрирован"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "409", description = "Пользователь уже существует"),
            @ApiResponse(responseCode = "429", description = "Слишком много запросов")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthDtos.RegisterRequest registerRequest, 
                                     HttpServletRequest request) {
        // Rate limiting для регистрации
        ConsumptionProbe probe = registerRateLimitBucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            log.warn("Rate limit exceeded for registration from IP: {}", getClientIpAddress(request));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new AuthDtos.ErrorResponse(
                            "RATE_LIMIT_EXCEEDED",
                            "Слишком много попыток регистрации. Попробуйте позже.",
                            System.currentTimeMillis(),
                            HttpStatus.TOO_MANY_REQUESTS.value()
                    ));
        }

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
            @ApiResponse(responseCode = "401", description = "Неверные учетные данные"),
            @ApiResponse(responseCode = "429", description = "Слишком много запросов")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthDtos.LoginRequest loginRequest, 
                                  HttpServletRequest request) {
        // Rate limiting для входа
        ConsumptionProbe probe = loginRateLimitBucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            log.warn("Rate limit exceeded for login from IP: {}", getClientIpAddress(request));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new AuthDtos.ErrorResponse(
                            "RATE_LIMIT_EXCEEDED",
                            "Слишком много попыток входа. Попробуйте позже.",
                            System.currentTimeMillis(),
                            HttpStatus.TOO_MANY_REQUESTS.value()
                    ));
        }

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
            @ApiResponse(responseCode = "401", description = "Недействительный refresh токен"),
            @ApiResponse(responseCode = "429", description = "Слишком много запросов")
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody AuthDtos.RefreshRequest refreshRequest, 
                                   HttpServletRequest request) {
        // Rate limiting для обновления токена
        ConsumptionProbe probe = refreshRateLimitBucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            log.warn("Rate limit exceeded for token refresh from IP: {}", getClientIpAddress(request));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new AuthDtos.ErrorResponse(
                            "RATE_LIMIT_EXCEEDED",
                            "Слишком много попыток обновления токена. Попробуйте позже.",
                            System.currentTimeMillis(),
                            HttpStatus.TOO_MANY_REQUESTS.value()
                    ));
        }

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

    /**
     * Получение IP адреса клиента для rate limiting
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
