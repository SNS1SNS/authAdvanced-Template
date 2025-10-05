package com.basic.project.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;


public class AuthDtos {


    @Data
    @Schema(description = "Запрос на регистрацию пользователя")
    public static class RegisterRequest {
        @Schema(description = "Имя пользователя", example = "john_doe")
        @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "Username должен быть из 3-20 символов,толкьо буквы цифры и подчекривания")
        @NotBlank(message = "Имя пользователя обязательно")
        private String username;

        @Schema(description = "Пароль", example = "SecurePassword123")
        @NotBlank(message = "Пароль обязателен для заполнения")
        @Size(min = 8, max = 100, message = "Пароль должен содержать от 8 до 100 символов")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "Пароль должен содержать минимум одну строчную букву, одну заглавную букву, одну цифру и один специальный символ")
        private String password;

        @Schema(description = "Email адрес", example = "ivan.ivanov@example.com")
        @NotBlank(message = "Email обязателен для заполнения")
        @Email(message = "Некорректный формат email")
        private String email;

        @Schema(description = "Номер телефона")
        @NotBlank(message = "Номер телефона обязателен")
        @Pattern(regexp = "^\\+?[0-9]{11}$",
                message = "Номер телефона должен состоять из 11 цифр и может начинаться с +")
        private String  phone;
    }


    @Data
    @Schema(description = "Запрос на вход в систему")
    public static class LoginRequest {

        @Schema(description = "Email адрес или имя пользователя", example = "ivan.ivanov@example.com")
        @NotBlank(message = "Email или username обязателен для заполнения")
        private String principal;

        @Schema(description = "Пароль", example = "SecurePass123!")
        @NotBlank(message = "Пароль обязателен для заполнения")
        private String password;
    }



    @Data
    @Schema(description = "Запрос на обновление токена")
    public static class RefreshRequest {

        @Schema(description = "Refresh токен")
        @NotBlank(message = "Refresh токен обязателен для заполнения")
        private String refreshToken;
    }

    @Data
    @Schema(description = "Универсальный ответ с токенами")
    public static class TokenResponse {

        @Schema(description = "ID пользователя")
        private Long id;

        @Schema(description = "Access токен")
        private String accessToken;

        @Schema(description = "Refresh токен")
        private String refreshToken;

        @Schema(description = "Тип токена")
        private String tokenType = "Bearer";

        @Schema(description = "Время жизни токена в секундах")
        private Long expiresIn;

        public TokenResponse(Long id, String accessToken, String refreshToken, Long expiresIn) {
            this.id = id;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
        }
    }

    @Data
    @Schema(description = "Ответ на регистрацию")
    public static class RegisterResponse {

        @Schema(description = "ID пользователя")
        private Long userId;

        @Schema(description = "Email пользователя")
        private String email;

        @Schema(description = "Имя пользователя")
        private String username;

        @Schema(description = "Дата создания")
        private LocalDateTime createdAt;

        public RegisterResponse(Long userId, String email, String username, LocalDateTime createdAt) {
            this.userId = userId;
            this.email = email;
            this.username = username;
            this.createdAt = createdAt;
        }
    }

    @Data
    @Schema(description = "Ответ на вход в систему")
    public static class LoginResponse {

        @Schema(description = "ID пользователя")
        private Long userId;

        @Schema(description = "Email пользователя")
        private String email;

        @Schema(description = "Имя пользователя")
        private String username;

        @Schema(description = "Роль пользователя")
        private String role;

        @Schema(description = "Access токен")
        private String accessToken;

        @Schema(description = "Refresh токен")
        private String refreshToken;

        @Schema(description = "Тип токена")
        private String tokenType = "Bearer";

        @Schema(description = "Время жизни токена в секундах")
        private Long expiresIn;

        public LoginResponse(Long userId, String email, String username, String role,
                             String accessToken, String refreshToken, Long expiresIn) {
            this.userId = userId;
            this.email = email;
            this.username = username;
            this.role = role;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
        }
    }


    @Data
    @Schema(description = "Ответ на обновление токена")
    public static class RefreshResponse {

        @Schema(description = "Новый access токен")
        private String accessToken;

        @Schema(description = "Новый refresh токен")
        private String refreshToken;

        @Schema(description = "Тип токена")
        private String tokenType = "Bearer";

        @Schema(description = "Время жизни токена в секундах")
        private Long expiresIn;

        public RefreshResponse(String accessToken, String refreshToken, Long expiresIn) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
        }
    }

    @Data
    @Schema(description = "Ответ с ошибкой")
    public static class ErrorResponse {

        @Schema(description = "Код ошибки")
        private String errorCode;

        @Schema(description = "Сообщение об ошибке")
        private String message;

        @Schema(description = "Временная метка ошибки")
        private long timestamp;

        @Schema(description = "HTTP статус")
        private int status;

        public ErrorResponse(String errorCode, String message, long timestamp, int status) {
            this.errorCode = errorCode;
            this.message = message;
            this.timestamp = timestamp;
            this.status = status;
        }
    }

}
