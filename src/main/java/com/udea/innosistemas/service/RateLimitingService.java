package com.udea.innosistemas.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de Rate Limiting para controlar el número de peticiones por usuario.
 * Utiliza algoritmo Token Bucket con Bucket4j y caché local en memoria.
 * Soporta diferentes límites por tipo de usuario y endpoint.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
@Service
public class RateLimitingService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);

    private final Map<String, Bucket> localCache = new ConcurrentHashMap<>();

    @Value("${innosistemas.ratelimit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${innosistemas.ratelimit.default.capacity:100}")
    private long defaultCapacity;

    @Value("${innosistemas.ratelimit.default.refill-tokens:100}")
    private long defaultRefillTokens;

    @Value("${innosistemas.ratelimit.default.refill-period-minutes:1}")
    private long defaultRefillPeriodMinutes;

    @Value("${innosistemas.ratelimit.auth.capacity:10}")
    private long authCapacity;

    @Value("${innosistemas.ratelimit.auth.refill-tokens:10}")
    private long authRefillTokens;

    @Value("${innosistemas.ratelimit.auth.refill-period-minutes:1}")
    private long authRefillPeriodMinutes;

    /**
     * Verifica si el usuario puede realizar una petición (consume 1 token)
     *
     * @param key Clave única del usuario (username, IP, etc.)
     * @return true si la petición es permitida, false si excede el límite
     */
    public boolean allowRequest(String key) {
        return allowRequest(key, 1);
    }

    /**
     * Verifica si el usuario puede realizar una petición consumiendo N tokens
     *
     * @param key Clave única del usuario
     * @param tokens Número de tokens a consumir
     * @return true si la petición es permitida, false si excede el límite
     */
    public boolean allowRequest(String key, long tokens) {
        if (!rateLimitEnabled) {
            return true;
        }

        try {
            Bucket bucket = resolveBucket(key);
            boolean allowed = bucket.tryConsume(tokens);

            if (!allowed) {
                logger.warn("Rate limit exceeded for key: {}", key);
            }

            return allowed;
        } catch (Exception e) {
            logger.error("Error checking rate limit for key {}: {}", key, e.getMessage());
            // En caso de error, permitir la petición por defecto (fail-open)
            return true;
        }
    }

    /**
     * Obtiene o crea un bucket para una clave específica
     *
     * @param key Clave única
     * @return Bucket configurado
     */
    private Bucket resolveBucket(String key) {
        return localCache.computeIfAbsent(key, k -> createNewBucket());
    }

    /**
     * Crea un nuevo bucket con configuración por defecto
     *
     * @return Bucket configurado
     */
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(
                defaultCapacity,
                Refill.intervally(defaultRefillTokens, Duration.ofMinutes(defaultRefillPeriodMinutes))
        );
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Crea un bucket para endpoints de autenticación (límites más estrictos)
     *
     * @return Bucket configurado para auth
     */
    private Bucket createAuthBucket() {
        Bandwidth limit = Bandwidth.classic(
                authCapacity,
                Refill.intervally(authRefillTokens, Duration.ofMinutes(authRefillPeriodMinutes))
        );
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Verifica límite para operaciones de autenticación (login, refresh token)
     *
     * @param key Clave del usuario
     * @return true si permitido
     */
    public boolean allowAuthRequest(String key) {
        if (!rateLimitEnabled) {
            return true;
        }

        try {
            String authKey = "auth:" + key;
            Bucket bucket = localCache.computeIfAbsent(authKey, k -> createAuthBucket());
            boolean allowed = bucket.tryConsume(1);

            if (!allowed) {
                logger.warn("Auth rate limit exceeded for key: {}", key);
            }

            return allowed;
        } catch (Exception e) {
            logger.error("Error checking auth rate limit for key {}: {}", key, e.getMessage());
            return true;
        }
    }

    /**
     * Obtiene el número de tokens disponibles para una clave
     *
     * @param key Clave del usuario
     * @return Número de tokens disponibles
     */
    public long getAvailableTokens(String key) {
        try {
            Bucket bucket = localCache.get(key);
            if (bucket == null) {
                return defaultCapacity;
            }
            return bucket.getAvailableTokens();
        } catch (Exception e) {
            logger.error("Error getting available tokens for key {}: {}", key, e.getMessage());
            return 0;
        }
    }

    /**
     * Reinicia el bucket para una clave específica (uso administrativo)
     *
     * @param key Clave del usuario
     */
    public void resetBucket(String key) {
        try {
            localCache.remove(key);
            logger.info("Rate limit bucket reset for key: {}", key);
        } catch (Exception e) {
            logger.error("Error resetting bucket for key {}: {}", key, e.getMessage());
        }
    }

    /**
     * Limpia todos los buckets (uso administrativo)
     */
    public void clearAllBuckets() {
        try {
            localCache.clear();
            logger.info("All rate limit buckets cleared");
        } catch (Exception e) {
            logger.error("Error clearing all buckets: {}", e.getMessage());
        }
    }

    /**
     * Obtiene estadísticas de uso para una clave
     *
     * @param key Clave del usuario
     * @return Información de uso en formato String
     */
    public String getBucketStats(String key) {
        try {
            Bucket bucket = localCache.get(key);
            if (bucket == null) {
                return String.format("Key: %s - No data (bucket not created)", key);
            }

            long available = bucket.getAvailableTokens();
            return String.format("Key: %s - Available tokens: %d/%d", key, available, defaultCapacity);
        } catch (Exception e) {
            logger.error("Error getting bucket stats for key {}: {}", key, e.getMessage());
            return "Error retrieving stats";
        }
    }

    /**
     * Verifica si el rate limiting está habilitado
     *
     * @return true si está habilitado
     */
    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }
}
