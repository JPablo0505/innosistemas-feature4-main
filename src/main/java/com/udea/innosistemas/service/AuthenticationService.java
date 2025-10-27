package com.udea.innosistemas.service;

import com.udea.innosistemas.dto.AuthResponse;
import com.udea.innosistemas.dto.LoginRequest;
import com.udea.innosistemas.dto.LogoutResponse;
import com.udea.innosistemas.dto.UserInfo;
import com.udea.innosistemas.entity.User;
import com.udea.innosistemas.exception.AuthenticationException;
import com.udea.innosistemas.repository.UserRepository;
import com.udea.innosistemas.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

//clase para gestionar la autenticación de usuarios en la aplicación.
//Proporciona métodos para iniciar sesión, validar credenciales y generar tokens JWT.
//Maneja excepciones específicas para diferentes fallos de autenticación.
// Autor: Fábrica-Escuela de Software UdeA
// Versión: 1.0.0

@Service
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private SessionManagementService sessionManagementService;

    public AuthResponse login(LoginRequest loginRequest) {
        try {
            logger.info("Attempting login for user ID");

            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = tokenProvider.generateToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(authentication);

            // Registrar sesión activa
            String sessionId = UUID.randomUUID().toString();
            sessionManagementService.registerSession(user.getEmail(), sessionId);

            // Usar el nuevo constructor que incluye todos los campos
            UserInfo userInfo = new UserInfo(user);

            logger.info("Login successful for user ID: {}", user.getId());

            return new AuthResponse(jwt, refreshToken, userInfo);

        } catch (BadCredentialsException e) {
            logger.warn("Login failed - Invalid credentials");
            throw new AuthenticationException("Credenciales inválidas");
        } catch (UsernameNotFoundException e) {
            logger.warn("Login failed - User not found");
            throw new AuthenticationException("Usuario no encontrado");
        } catch (Exception e) {
            logger.error("Login failed - Unexpected error: {}", e.getMessage());
            throw new AuthenticationException("Error durante la autenticación");
        }
    }

    public boolean validateCredentials(String email, String password) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
            return true;
        } catch (BadCredentialsException e) {
            return false;
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        try {
            logger.info("Attempting to refresh token");

            // Validar que el token no esté en la blacklist
            if (tokenBlacklistService.isTokenBlacklisted(refreshToken)) {
                logger.warn("Refresh token is blacklisted");
                throw new AuthenticationException("Token inválido o revocado");
            }

            // Validar el refresh token
            if (!tokenProvider.validateToken(refreshToken)) {
                logger.warn("Invalid refresh token");
                throw new AuthenticationException("Token inválido");
            }

            // Verificar que sea un refresh token
            if (!tokenProvider.isRefreshToken(refreshToken)) {
                logger.warn("Token is not a refresh token");
                throw new AuthenticationException("Token no es un refresh token");
            }

            // Extraer username del token
            String username = tokenProvider.getUsernameFromJWT(refreshToken);

            // Buscar usuario
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

            // Verificar que el usuario tenga sesiones activas
            if (!sessionManagementService.hasActiveSessions(username)) {
                logger.warn("User has no active sessions");
                throw new AuthenticationException("No hay sesiones activas");
            }

            // Generar nuevos tokens con claims completos
            String newAccessToken = tokenProvider.generateTokenFromUser(user);
            String newRefreshToken = tokenProvider.generateRefreshTokenFromUser(user);

            // Invalidar el refresh token anterior
            tokenBlacklistService.blacklistToken(refreshToken, tokenProvider.getExpirationDateFromJWT(refreshToken));

            UserInfo userInfo = new UserInfo(user);

            logger.info("Token refreshed successfully for user: {}", username);

            return new AuthResponse(newAccessToken, newRefreshToken, userInfo);

        } catch (UsernameNotFoundException e) {
            logger.warn("Token refresh failed - User not found");
            throw new AuthenticationException("Usuario no encontrado");
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Token refresh failed - Unexpected error: {}", e.getMessage());
            throw new AuthenticationException("Error al renovar el token");
        }
    }

    public LogoutResponse logout(String token) {
        try {
            logger.info("Attempting logout");

            // Validar el token
            if (!tokenProvider.validateToken(token)) {
                logger.warn("Invalid token for logout");
                return new LogoutResponse(false, "Token inválido");
            }

            // Extraer username
            String username = tokenProvider.getUsernameFromJWT(token);

            // Agregar token a la blacklist
            tokenBlacklistService.blacklistToken(token, tokenProvider.getExpirationDateFromJWT(token));

            // Invalidar todas las sesiones del usuario
            long sessionsInvalidated = sessionManagementService.invalidateAllUserSessions(username);

            // Limpiar el SecurityContext
            SecurityContextHolder.clearContext();

            logger.info("Logout successful for user: {}, sessions invalidated: {}", username, sessionsInvalidated);

            return new LogoutResponse(true, "Logout exitoso");

        } catch (Exception e) {
            logger.error("Logout failed - Unexpected error: {}", e.getMessage());
            return new LogoutResponse(false, "Error durante el logout");
        }
    }

    public LogoutResponse logoutFromAllDevices(String username) {
        try {
            logger.info("Attempting logout from all devices for user: {}", username);

            // Invalidar todas las sesiones del usuario
            long sessionsInvalidated = sessionManagementService.invalidateAllUserSessions(username);

            logger.info("Logout from all devices successful for user: {}, sessions invalidated: {}", username, sessionsInvalidated);

            return new LogoutResponse(true, "Logout exitoso de todos los dispositivos");

        } catch (Exception e) {
            logger.error("Logout from all devices failed - Unexpected error: {}", e.getMessage());
            return new LogoutResponse(false, "Error durante el logout");
        }
    }
}