package com.udea.innosistemas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Servicio para gestionar la blacklist de tokens JWT revocados utilizando Redis.
 * Permite invalidar tokens antes de su expiración natural (logout, cambio de contraseña, etc.).
 * Utiliza Redis para almacenamiento distribuido y expira automáticamente los tokens.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
@Service
public class TokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * Agrega un token a la blacklist
     *
     * @param token Token a invalidar
     * @param expirationDate Fecha de expiración del token
     */
    public void blacklistToken(String token, Date expirationDate) {
        try {
            String key = BLACKLIST_PREFIX + token;
            long ttl = expirationDate.getTime() - System.currentTimeMillis();

            if (ttl > 0) {
                redisTemplate.opsForValue().set(key, "revoked", ttl, TimeUnit.MILLISECONDS);
                logger.info("Token added to blacklist with TTL: {} ms", ttl);
            } else {
                logger.warn("Token already expired, not adding to blacklist");
            }
        } catch (Exception e) {
            logger.error("Error adding token to blacklist: {}", e.getMessage(), e);
        }
    }

    /**
     * Verifica si un token está en la blacklist
     *
     * @param token Token a verificar
     * @return true si el token está revocado, false en caso contrario
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            logger.error("Error checking token blacklist: {}", e.getMessage(), e);
            // En caso de error de Redis, rechazar el token por seguridad
            return true;
        }
    }

    /**
     * Remueve un token de la blacklist (uso administrativo)
     *
     * @param token Token a remover
     */
    public void removeTokenFromBlacklist(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            redisTemplate.delete(key);
            logger.info("Token removed from blacklist");
        } catch (Exception e) {
            logger.error("Error removing token from blacklist: {}", e.getMessage(), e);
        }
    }

    /**
     * Limpia todos los tokens blacklisted (uso administrativo)
     */
    public void clearBlacklist() {
        try {
            var keys = redisTemplate.keys(BLACKLIST_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.info("Blacklist cleared: {} tokens removed", keys.size());
            }
        } catch (Exception e) {
            logger.error("Error clearing blacklist: {}", e.getMessage(), e);
        }
    }
}
