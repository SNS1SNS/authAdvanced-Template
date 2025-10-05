package com.basic.project.service;

import com.basic.project.dto.AuthDtos;
import com.basic.project.entity.UserEntity;
import com.basic.project.exception.AuthException;
import com.basic.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;


    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.access.ttl}")
    private long accessTokenExpiration;
    /**
     * Регистрация нового пользователя
     */

    @Transactional
    public AuthDtos.TokenResponse register(AuthDtos.RegisterRequest request) {

        // Проверяем, существует ли пользователь с таким email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw AuthException.userAlreadyExists(request.getEmail());
        }

        // Проверяем, существует ли пользователь с таким username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw AuthException.userAlreadyExists(request.getUsername());
        }


        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(request.getUsername());
        userEntity.setPassword(passwordEncoder.encode(request.getPassword()));
        userEntity.setEmail(request.getEmail());
        userEntity.setPhone(request.getPhone());
        userEntity.setEnabled(true);

        UserEntity savedUserEntity = userRepository.save(userEntity);


        String accessToken = jwtService.generateAccessToken(String.valueOf(savedUserEntity.getId()));
        String refreshToken = jwtService.generateRefreshToken(String.valueOf(savedUserEntity.getId()));


        return new AuthDtos.TokenResponse(
            savedUserEntity.getId(),
            accessToken,
            refreshToken,
            accessTokenExpiration
        );

    }

    /**
     * Вход в систему
     */

    @Transactional(readOnly = true)
    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(
                request.getPrincipal(), request.getPrincipal())
                .orElseThrow(() -> {
                    log.warn("User not found: {}", request.getPrincipal());
                    return AuthException.invalidCredentials();
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Incorrect password for user: {}", request.getPrincipal());
            throw AuthException.invalidCredentials();
        }

        if (!user.isEnabled()){
            log.warn("User is disabled");
            throw AuthException.userDisabled();
        }

        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        log.info("User logged in successfully: {}", user.getEmail());

        return new AuthDtos.TokenResponse(
            user.getId(),
            accessToken,
            refreshToken,
            accessTokenExpiration
        );
    }

    /**
     * Обновление токена
     */

    @Transactional(readOnly = true)
    public AuthDtos.TokenResponse refresh(AuthDtos.RefreshRequest request) {

        String email = jwtService.getEmailFromToken(request.getRefreshToken());
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> AuthException.invalidRefreshToken());

        // Проверяем, что пользователь активен
        if (!user.isEnabled()) {
            log.warn("Disabled user attempted to refresh token: {}", email);
            throw AuthException.userDisabled();
        }

        // Генерируем новые токены
        String newAccessToken = jwtService.generateAccessToken(user.getEmail());
        String newRefreshToken = jwtService.generateRefreshToken(user.getEmail());

        return new AuthDtos.TokenResponse(
                user.getId(),
                newAccessToken,
                newRefreshToken,
                accessTokenExpiration
        );
    }


    /**
     * Валидация токена
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "tokenValidationCache", key = "#token")
    public boolean validateToken(String token) {
        return jwtService.isValidAccessToken(token);
    }


}
