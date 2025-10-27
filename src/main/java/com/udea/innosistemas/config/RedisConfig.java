package com.udea.innosistemas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Configuración de Redis para gestión de caché, blacklist de tokens y sesiones.
 * Configura conexión a Redis y RedisTemplate para operaciones de datos.
 * Soporta configuración por perfiles (dev, test, prod).
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
@Configuration
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.timeout:2000ms}")
    private String redisTimeout;

    /**
     * Configura la conexión a Redis usando Jedis
     *
     * @return RedisConnectionFactory configurado
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);

        // Configurar password si está presente
        if (redisPassword != null && !redisPassword.isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }

        // Configurar timeout de conexión
        Duration timeout = parseDuration(redisTimeout);
        JedisClientConfiguration.JedisClientConfigurationBuilder jedisClientConfiguration =
                JedisClientConfiguration.builder();
        jedisClientConfiguration.connectTimeout(timeout);
        jedisClientConfiguration.readTimeout(timeout);

        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(
                redisConfig, jedisClientConfiguration.build());

        return jedisConnectionFactory;
    }

    /**
     * Configura RedisTemplate para operaciones con Redis
     * Usa StringRedisSerializer para claves y valores
     *
     * @param connectionFactory Factory de conexión a Redis
     * @return RedisTemplate configurado
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Usar StringRedisSerializer para serialización de claves y valores
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Parsea una duración en formato Spring (ej: "2000ms", "2s")
     *
     * @param duration String de duración
     * @return Duration parseada
     */
    private Duration parseDuration(String duration) {
        try {
            if (duration.endsWith("ms")) {
                long millis = Long.parseLong(duration.substring(0, duration.length() - 2));
                return Duration.ofMillis(millis);
            } else if (duration.endsWith("s")) {
                long seconds = Long.parseLong(duration.substring(0, duration.length() - 1));
                return Duration.ofSeconds(seconds);
            } else if (duration.endsWith("m")) {
                long minutes = Long.parseLong(duration.substring(0, duration.length() - 1));
                return Duration.ofMinutes(minutes);
            } else {
                // Por defecto, interpretar como milisegundos
                return Duration.ofMillis(Long.parseLong(duration));
            }
        } catch (Exception e) {
            // Valor por defecto en caso de error
            return Duration.ofMillis(2000);
        }
    }
}
