package com.basic.project.exception;


import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AuthException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public AuthException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode.name();
        this.httpStatus = errorCode.getStatus();
    }

    public AuthException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public AuthException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public static AuthException userAlreadyExists(String email) {
        return new AuthException(
                "Пользователь с email " + email + " уже существует",
                "USER_ALREADY_EXISTS",
                HttpStatus.CONFLICT
        );
    }

    public static AuthException usernameAlreadyExists(String username) {
        return new AuthException(
                "Пользователь с username " + username + " уже существует",
                "USERNAME_ALREADY_EXISTS",
                HttpStatus.CONFLICT
        );
    }

    public static AuthException invalidCredentials() {
        return new AuthException(
                "Неверные учетные данные",
                "INVALID_CREDENTIALS",
                HttpStatus.UNAUTHORIZED
        );
    }

    public static AuthException invalidRefreshToken() {
        return new AuthException(
                "Недействительный refresh токен",
                "INVALID_REFRESH_TOKEN",
                HttpStatus.UNAUTHORIZED
        );
    }

    public static AuthException userNotFound(String identifier) {
        return new AuthException(
                "Пользователь не найден: " + identifier,
                "USER_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
    }

    public static AuthException userDisabled() {
        return new AuthException(
                "Пользователь отключен",
                "USER_DISABLED",
                HttpStatus.FORBIDDEN
        );
    }

    public static AuthException invalidInput(String message) {
        return new AuthException(
                message,
                "INVALID_INPUT",
                HttpStatus.BAD_REQUEST
        );
    }

    public enum ErrorCode {
        USER_ALREADY_EXISTS("Пользователь уже существует", HttpStatus.CONFLICT),
        USERNAME_ALREADY_EXISTS("Username уже занят", HttpStatus.CONFLICT),
        INVALID_CREDENTIALS("Неверные учетные данные", HttpStatus.UNAUTHORIZED),
        INVALID_REFRESH_TOKEN("Недействительный refresh токен", HttpStatus.UNAUTHORIZED),
        USER_NOT_FOUND("Пользователь не найден", HttpStatus.NOT_FOUND),
        USER_DISABLED("Пользователь отключен", HttpStatus.FORBIDDEN),
        INVALID_INPUT("Некорректные данные", HttpStatus.BAD_REQUEST),
        TOKEN_EXPIRED("Токен истек", HttpStatus.UNAUTHORIZED),
        INSUFFICIENT_PERMISSIONS("Недостаточно прав", HttpStatus.FORBIDDEN);

        private final String message;
        private final HttpStatus status;

        ErrorCode(String message, HttpStatus status) {
            this.message = message;
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }
}
