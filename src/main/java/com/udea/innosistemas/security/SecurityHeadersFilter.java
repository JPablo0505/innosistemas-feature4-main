package com.udea.innosistemas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que agrega headers de seguridad estándar a todas las respuestas HTTP.
 * Implementa las mejores prácticas de seguridad recomendadas por OWASP.
 * Headers incluyen: CSP, HSTS, X-Frame-Options, X-Content-Type-Options, etc.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Value("${innosistemas.security.headers.csp:default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self' http://localhost:* ws://localhost:*}")
    private String contentSecurityPolicy;

    @Value("${innosistemas.security.headers.hsts.enabled:true}")
    private boolean hstsEnabled;

    @Value("${innosistemas.security.headers.hsts.max-age:31536000}")
    private long hstsMaxAge;

    @Value("${innosistemas.security.headers.frame-options:DENY}")
    private String frameOptions;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Content Security Policy (CSP)
        // Previene XSS definiendo fuentes permitidas de contenido
        response.setHeader("Content-Security-Policy", contentSecurityPolicy);

        // 2. Strict-Transport-Security (HSTS)
        // Fuerza el uso de HTTPS
        if (hstsEnabled && request.isSecure()) {
            response.setHeader("Strict-Transport-Security",
                    String.format("max-age=%d; includeSubDomains; preload", hstsMaxAge));
        }

        // 3. X-Frame-Options
        // Previene ataques de clickjacking
        response.setHeader("X-Frame-Options", frameOptions);

        // 4. X-Content-Type-Options
        // Previene MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // 5. X-XSS-Protection
        // Habilita protección XSS del navegador (legacy pero útil)
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // 6. Referrer-Policy
        // Controla qué información de referrer se envía
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 7. Permissions-Policy (antes Feature-Policy)
        // Controla qué características del navegador están permitidas
        response.setHeader("Permissions-Policy",
                "geolocation=(), microphone=(), camera=(), payment=(), usb=()");

        // 8. X-Permitted-Cross-Domain-Policies
        // Restringe Adobe Flash y PDF cross-domain policies
        response.setHeader("X-Permitted-Cross-Domain-Policies", "none");

        // 9. Cache-Control para respuestas sensibles
        // Previene caching de información sensible
        if (isSensitiveEndpoint(request)) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }

        // 10. Custom security headers para identificación
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Powered-By", ""); // Ocultar información del servidor

        filterChain.doFilter(request, response);
    }

    /**
     * Determina si un endpoint contiene información sensible
     *
     * @param request HttpServletRequest
     * @return true si el endpoint es sensible
     */
    private boolean isSensitiveEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.contains("/auth/") ||
               uri.contains("/api/") ||
               uri.contains("/graphql") ||
               uri.contains("/user/");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // No aplicar a archivos estáticos
        String path = request.getRequestURI();
        return path.endsWith(".css") ||
               path.endsWith(".js") ||
               path.endsWith(".png") ||
               path.endsWith(".jpg") ||
               path.endsWith(".ico") ||
               path.endsWith(".svg");
    }
}
