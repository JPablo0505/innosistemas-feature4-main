package com.udea.innosistemas.resolver;

import com.udea.innosistemas.dto.AuthResponse;
import com.udea.innosistemas.dto.LoginRequest;
import com.udea.innosistemas.dto.LogoutResponse;
import com.udea.innosistemas.security.JwtTokenProvider;
import com.udea.innosistemas.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * Resolver GraphQL para manejar mutaciones de autenticación.
 * Proporciona endpoints para login, logout, refresh token y gestión de sesiones.
 * Implementa seguridad basada en JWT con soporte de refresh tokens.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 2.0.0
 */
@Controller
@Validated
public class AuthMutationResolver {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired(required = false)
    private HttpServletRequest request;

    /**
     * Mutation para autenticar un usuario
     *
     * @param email Email del usuario
     * @param password Contraseña del usuario
     * @return AuthResponse con tokens y información del usuario
     */
    @MutationMapping
    @PreAuthorize("permitAll()")
    public AuthResponse login(
            @Argument @Valid @Email @NotBlank String email,
            @Argument @Valid @NotBlank String password) {
        LoginRequest loginRequest = new LoginRequest(email, password);
        return authenticationService.login(loginRequest);
    }

    /**
     * Mutation para renovar el token de acceso usando un refresh token
     *
     * @param refreshToken Refresh token válido
     * @return AuthResponse con nuevos tokens
     */
    @MutationMapping
    @PreAuthorize("permitAll()")
    public AuthResponse refreshToken(@Argument @Valid @NotBlank String refreshToken) {
        return authenticationService.refreshToken(refreshToken);
    }

    /**
     * Mutation para cerrar sesión del usuario actual
     *
     * @param token Token JWT a invalidar (opcional, se puede extraer del header)
     * @return LogoutResponse indicando el resultado
     */
    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public LogoutResponse logout(@Argument String token) {
        // Si no se proporciona token, extraer del header
        String jwt = token;
        if (!StringUtils.hasText(jwt) && request != null) {
            jwt = extractTokenFromRequest(request);
        }

        if (!StringUtils.hasText(jwt)) {
            return new LogoutResponse(false, "Token no proporcionado");
        }

        return authenticationService.logout(jwt);
    }

    /**
     * Mutation para cerrar sesión de todos los dispositivos del usuario actual
     *
     * @return LogoutResponse indicando el resultado
     */
    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public LogoutResponse logoutFromAllDevices() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return authenticationService.logoutFromAllDevices(username);
    }

    /**
     * Extrae el token JWT del header Authorization de la request
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}