package com.udea.innosistemas.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.*;

/**
 * Interceptor que implementa control de acceso basado en roles (RBAC).
 * Verifica que el usuario tenga los roles necesarios para acceder a endpoints específicos.
 * Complementa las anotaciones @PreAuthorize de Spring Security.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
@Component
public class RoleBasedAccessInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RoleBasedAccessInterceptor.class);

    // Mapeo de rutas a roles requeridos
    private static final Map<String, Set<String>> ROUTE_ROLE_MAPPING = new HashMap<>();

    static {
        // Admin only endpoints
        ROUTE_ROLE_MAPPING.put("/api/v1/admin/**", Set.of("ROLE_ADMIN"));
        ROUTE_ROLE_MAPPING.put("/actuator/prometheus", Set.of("ROLE_ADMIN"));
        ROUTE_ROLE_MAPPING.put("/actuator/metrics", Set.of("ROLE_ADMIN"));

        // Student endpoints
        ROUTE_ROLE_MAPPING.put("/api/v1/projects/**", Set.of("ROLE_STUDENT", "ROLE_ADMIN"));
        ROUTE_ROLE_MAPPING.put("/api/v1/teams/**", Set.of("ROLE_STUDENT", "ROLE_ADMIN"));

        // Public endpoints (no roles required)
        ROUTE_ROLE_MAPPING.put("/auth/**", Set.of());
        ROUTE_ROLE_MAPPING.put("/graphql", Set.of());
        ROUTE_ROLE_MAPPING.put("/graphiql/**", Set.of());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String requestURI = request.getRequestURI();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Si no hay autenticación, permitir (será manejado por Spring Security)
        if (authentication == null || !authentication.isAuthenticated()) {
            return true;
        }

        // Obtener roles del usuario
        Set<String> userRoles = extractUserRoles(authentication);

        // Verificar acceso basado en la ruta
        Set<String> requiredRoles = getRequiredRoles(requestURI);

        if (requiredRoles.isEmpty()) {
            // No hay restricciones de roles para esta ruta
            return true;
        }

        // Verificar si el usuario tiene alguno de los roles requeridos
        boolean hasRequiredRole = userRoles.stream()
                .anyMatch(requiredRoles::contains);

        if (!hasRequiredRole) {
            logger.warn("Access denied for user {} to {}: Required roles {}, User roles {}",
                    authentication.getName(), requestURI, requiredRoles, userRoles);

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                    String.format("{\"error\":\"Forbidden\",\"message\":\"Insufficient permissions. Required roles: %s\"}",
                            requiredRoles)
            );
            return false;
        }

        logger.debug("Access granted for user {} to {}", authentication.getName(), requestURI);
        return true;
    }

    /**
     * Extrae los roles del usuario autenticado
     *
     * @param authentication Objeto de autenticación
     * @return Set de roles del usuario
     */
    private Set<String> extractUserRoles(Authentication authentication) {
        Set<String> roles = new HashSet<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            roles.add(authority.getAuthority());
        }
        return roles;
    }

    /**
     * Obtiene los roles requeridos para una URI específica
     *
     * @param uri URI de la petición
     * @return Set de roles requeridos
     */
    private Set<String> getRequiredRoles(String uri) {
        for (Map.Entry<String, Set<String>> entry : ROUTE_ROLE_MAPPING.entrySet()) {
            String pattern = entry.getKey();
            if (matchesPattern(uri, pattern)) {
                return entry.getValue();
            }
        }
        return Set.of(); // No restrictions
    }

    /**
     * Verifica si una URI coincide con un patrón (soporta **)
     *
     * @param uri URI a verificar
     * @param pattern Patrón con wildcards
     * @return true si coincide
     */
    private boolean matchesPattern(String uri, String pattern) {
        // Convertir patrón de Ant a regex
        String regex = pattern
                .replace("**", ".*")
                .replace("*", "[^/]*")
                .replace("?", ".");

        return uri.matches(regex);
    }

    /**
     * Agrega una regla de acceso personalizada
     *
     * @param pattern Patrón de ruta
     * @param roles Roles permitidos
     */
    public static void addAccessRule(String pattern, Set<String> roles) {
        ROUTE_ROLE_MAPPING.put(pattern, roles);
    }

    /**
     * Remueve una regla de acceso
     *
     * @param pattern Patrón de ruta
     */
    public static void removeAccessRule(String pattern) {
        ROUTE_ROLE_MAPPING.remove(pattern);
    }

    /**
     * Obtiene todas las reglas de acceso configuradas
     *
     * @return Mapa de reglas
     */
    public static Map<String, Set<String>> getAllAccessRules() {
        return new HashMap<>(ROUTE_ROLE_MAPPING);
    }
}
