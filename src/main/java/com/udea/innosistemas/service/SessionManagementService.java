package com.udea.innosistemas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Servicio para gestionar sesiones activas de usuarios utilizando Redis.
 * Permite trackear sesiones concurrentes, limitar dispositivos simultáneos y gestionar sesiones activas.
 * Proporciona funcionalidades de auditoría y control de sesiones por usuario.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
@Service
public class SessionManagementService {

    private static final Logger logger = LoggerFactory.getLogger(SessionManagementService.class);
    private static final String SESSION_PREFIX = "session:user:";
    private static final String SESSION_COUNT_PREFIX = "session:count:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${innosistemas.auth.jwt.expiration}")
    private long jwtExpirationInMs;

    /**
     * Registra una nueva sesión para un usuario
     *
     * @param username Nombre de usuario
     * @param sessionId ID único de la sesión (puede ser el token o un UUID)
     * @return true si la sesión fue registrada exitosamente
     */
    public boolean registerSession(String username, String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + username;
            String value = sessionId + ":" + Instant.now().toEpochMilli();

            // Agregar sesión al conjunto de sesiones activas del usuario
            redisTemplate.opsForSet().add(sessionKey, value);

            // Establecer expiración en el conjunto
            redisTemplate.expire(sessionKey, jwtExpirationInMs, TimeUnit.SECONDS);

            // Incrementar contador de sesiones
            incrementSessionCount(username);

            logger.info("Session registered for user: {}, sessionId: {}", username, sessionId);
            return true;
        } catch (Exception e) {
            logger.error("Error registering session for user {}: {}", username, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Remueve una sesión específica de un usuario
     *
     * @param username Nombre de usuario
     * @param sessionId ID de la sesión a remover
     * @return true si la sesión fue removida exitosamente
     */
    public boolean removeSession(String username, String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + username;
            Set<String> sessions = redisTemplate.opsForSet().members(sessionKey);

            if (sessions != null) {
                // Buscar y remover la sesión que coincida con el sessionId
                sessions.stream()
                        .filter(s -> s.startsWith(sessionId + ":"))
                        .forEach(s -> redisTemplate.opsForSet().remove(sessionKey, s));

                decrementSessionCount(username);
                logger.info("Session removed for user: {}, sessionId: {}", username, sessionId);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error removing session for user {}: {}", username, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Invalida todas las sesiones de un usuario
     *
     * @param username Nombre de usuario
     * @return Número de sesiones invalidadas
     */
    public long invalidateAllUserSessions(String username) {
        try {
            String sessionKey = SESSION_PREFIX + username;
            Set<String> sessions = redisTemplate.opsForSet().members(sessionKey);

            if (sessions != null && !sessions.isEmpty()) {
                long count = sessions.size();
                redisTemplate.delete(sessionKey);
                resetSessionCount(username);
                logger.info("All sessions invalidated for user: {}, count: {}", username, count);
                return count;
            }
            return 0;
        } catch (Exception e) {
            logger.error("Error invalidating sessions for user {}: {}", username, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Obtiene el número de sesiones activas de un usuario
     *
     * @param username Nombre de usuario
     * @return Número de sesiones activas
     */
    public long getActiveSessionCount(String username) {
        try {
            String sessionKey = SESSION_PREFIX + username;
            Long count = redisTemplate.opsForSet().size(sessionKey);
            return count != null ? count : 0;
        } catch (Exception e) {
            logger.error("Error getting session count for user {}: {}", username, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Verifica si un usuario tiene sesiones activas
     *
     * @param username Nombre de usuario
     * @return true si el usuario tiene al menos una sesión activa
     */
    public boolean hasActiveSessions(String username) {
        return getActiveSessionCount(username) > 0;
    }

    /**
     * Obtiene todas las sesiones activas de un usuario
     *
     * @param username Nombre de usuario
     * @return Conjunto de IDs de sesión
     */
    public Set<String> getUserSessions(String username) {
        try {
            String sessionKey = SESSION_PREFIX + username;
            return redisTemplate.opsForSet().members(sessionKey);
        } catch (Exception e) {
            logger.error("Error getting sessions for user {}: {}", username, e.getMessage(), e);
            return Set.of();
        }
    }

    /**
     * Verifica si una sesión específica está activa
     *
     * @param username Nombre de usuario
     * @param sessionId ID de la sesión
     * @return true si la sesión está activa
     */
    public boolean isSessionActive(String username, String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + username;
            Set<String> sessions = redisTemplate.opsForSet().members(sessionKey);

            if (sessions != null) {
                return sessions.stream().anyMatch(s -> s.startsWith(sessionId + ":"));
            }
            return false;
        } catch (Exception e) {
            logger.error("Error checking session status: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Limpia sesiones expiradas (normalmente Redis lo hace automáticamente)
     */
    public void cleanupExpiredSessions() {
        try {
            Set<String> keys = redisTemplate.keys(SESSION_PREFIX + "*");
            if (keys != null) {
                long cleaned = 0;
                for (String key : keys) {
                    Set<String> sessions = redisTemplate.opsForSet().members(key);
                    if (sessions != null) {
                        long now = Instant.now().toEpochMilli();
                        for (String session : sessions) {
                            String[] parts = session.split(":");
                            if (parts.length >= 2) {
                                long timestamp = Long.parseLong(parts[parts.length - 1]);
                                if (now - timestamp > jwtExpirationInMs * 1000) {
                                    redisTemplate.opsForSet().remove(key, session);
                                    cleaned++;
                                }
                            }
                        }
                    }
                }
                logger.info("Cleaned up {} expired sessions", cleaned);
            }
        } catch (Exception e) {
            logger.error("Error cleaning up expired sessions: {}", e.getMessage(), e);
        }
    }

    // Métodos privados para gestión de contadores

    private void incrementSessionCount(String username) {
        try {
            String countKey = SESSION_COUNT_PREFIX + username;
            redisTemplate.opsForValue().increment(countKey);
        } catch (Exception e) {
            logger.error("Error incrementing session count: {}", e.getMessage());
        }
    }

    private void decrementSessionCount(String username) {
        try {
            String countKey = SESSION_COUNT_PREFIX + username;
            redisTemplate.opsForValue().decrement(countKey);
        } catch (Exception e) {
            logger.error("Error decrementing session count: {}", e.getMessage());
        }
    }

    private void resetSessionCount(String username) {
        try {
            String countKey = SESSION_COUNT_PREFIX + username;
            redisTemplate.delete(countKey);
        } catch (Exception e) {
            logger.error("Error resetting session count: {}", e.getMessage());
        }
    }

    /**
     * Obtiene estadísticas de sesiones
     *
     * @return Número total de usuarios con sesiones activas
     */
    public long getTotalActiveUsers() {
        try {
            Set<String> keys = redisTemplate.keys(SESSION_PREFIX + "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            logger.error("Error getting total active users: {}", e.getMessage(), e);
            return 0;
        }
    }
}
