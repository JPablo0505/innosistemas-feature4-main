package com.udea.innosistemas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Clase principal de la aplicación InnoSistemas
 * 
 * Plataforma de Integración y Desarrollo de Software para Estudiantes de Ingeniería de Sistemas
 * Universidad de Antioquia - Facultad de Ingeniería
 * 
 * Esta aplicación facilita la integración práctica de los conocimientos adquiridos por los 
 * estudiantes a lo largo de siete cursos del área de Ingeniería de Software, permitiendo 
 * formar equipos multidisciplinarios y desarrollar productos mínimos viables (MVPs) en un 
 * entorno de desarrollo ágil y colaborativo.
 * 
 * @author Fábrica-Escuela de Software UdeA
 * @version 1.0.0
 * @since 2025
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@ConfigurationPropertiesScan(basePackages = "com.udea.innosistemas.config.properties")
public class InnoSistemasApplication {

    /**
     * Método principal que inicia la aplicación Spring Boot
     * 
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        // Configurar propiedades del sistema antes de iniciar la aplicación
        configureSystemProperties();
        
        // Iniciar la aplicación Spring Boot
        SpringApplication application = new SpringApplication(InnoSistemasApplication.class);
        
        // Configurar propiedades adicionales de la aplicación
        configureApplicationProperties(application);
        
        // Ejecutar la aplicación
        application.run(args);
        
        // Log de inicio exitoso
        logApplicationStartup();
    }
    
    /**
     * Configura las propiedades del sistema necesarias para la aplicación
     */
    private static void configureSystemProperties() {
        // Configurar zona horaria por defecto para Colombia
        System.setProperty("user.timezone", "America/Bogota");
        
        // Configurar encoding por defecto
        System.setProperty("file.encoding", "UTF-8");
        
        // Configurar propiedades de JVM para mejor rendimiento
        System.setProperty("java.awt.headless", "true");
        
        // Configurar propiedades de seguridad
        System.setProperty("spring.main.banner-mode", "console");
    }
    
    /**
     * Configura propiedades adicionales de la aplicación Spring Boot
     * 
     * @param application instancia de SpringApplication
     */
    private static void configureApplicationProperties(SpringApplication application) {
        // Configurar propiedades adicionales si es necesario
        application.setRegisterShutdownHook(true);
        
        // Configurar listeners personalizados si es necesario
        // application.addListeners(new CustomApplicationListener());
    }
    
    /**
     * Registra un mensaje de inicio exitoso de la aplicación
     */
    private static void logApplicationStartup() {
        System.out.println("\n" +
            "============================================================\n" +
            "    INNOSISTEMAS - BACKEND API INICIADO EXITOSAMENTE      \n" +
            "============================================================\n" +
            "  Universidad de Antioquia - Facultad de Ingeniería       \n" +
            "  Programa de Ingeniería de Sistemas                      \n" +
            "  Fábrica-Escuela de Software CodeF@ctory UdeA            \n" +
            "============================================================\n" +
            "  Version: 1.0.0                                          \n" +
            "  Profile: " + System.getProperty("spring.profiles.active", "default") + "\n" +
            "  Timezone: " + System.getProperty("user.timezone") + "\n" +
            "============================================================\n");
    }
}