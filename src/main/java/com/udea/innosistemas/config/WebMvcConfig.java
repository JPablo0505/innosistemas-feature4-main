package com.udea.innosistemas.config;

import com.udea.innosistemas.security.RoleBasedAccessInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Configuración consolidada de Web MVC para la aplicación InnoSistemas.
 *
 * Esta configuración establece:
 * - Políticas de CORS
 * - Interceptores de seguridad y roles
 * - Configuración de contenido estático
 * - Soporte para GraphQL y GraphiQL
 *
 * @author Fábrica-Escuela de Software UdeA
 * @version 2.0.0
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebMvcConfig.class);

    @Autowired
    private RoleBasedAccessInterceptor roleBasedAccessInterceptor;

    @Value("${innosistemas.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Value("${innosistemas.cors.allowed-methods}")
    private String allowedMethods;

    @Value("${innosistemas.cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${innosistemas.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${innosistemas.cors.max-age:3600}")
    private long maxAge;

    /**
     * Configuración de CORS para permitir requests desde el frontend NextJS
     * y GraphiQL. Incluye configuración específica para GraphQL.
     *
     * @param registry registro de configuraciones CORS
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("Configurando CORS - Orígenes permitidos: {}", Arrays.toString(allowedOrigins));

        // Configuración general para todos los endpoints
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods(allowedMethods.split(","))
                .allowedHeaders(allowedHeaders.equals("*") ? new String[]{"*"} : allowedHeaders.split(","))
                .allowCredentials(allowCredentials)
                .maxAge(maxAge);

        // Configuración específica para GraphQL
        registry.addMapping("/graphql")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(allowCredentials)
                .maxAge(maxAge);

        // Configuración específica para GraphiQL
        registry.addMapping("/graphiql/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(allowCredentials)
                .maxAge(maxAge);

        // Configuración para endpoints de actuator
        registry.addMapping("/actuator/**")
                .allowedOriginPatterns("http://localhost:*")
                .allowedMethods("GET", "POST")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    /**
     * Configuración de interceptores incluyendo:
     * - Interceptor de control de acceso basado en roles
     * - Interceptor de cambio de idioma
     *
     * @param registry registro de interceptores
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Interceptor para cambio de idioma
        registry.addInterceptor(localeChangeInterceptor());

        // Interceptor de control de acceso basado en roles
        // Se aplica a /api/v1/** pero excluye endpoints públicos y de GraphQL
        registry.addInterceptor(roleBasedAccessInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/auth/**",
                        "/api/v1/graphql",
                        "/api/v1/graphiql/**",
                        "/api/v1/actuator/health",
                        "/api/v1/actuator/info",
                        "/api/v1/h2-console/**"
                );
    }

    /**
     * Configuración de recursos estáticos
     *
     * @param registry registro de recursos
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Configurar directorio para archivos subidos
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/")
                .setCachePeriod(3600)
                .resourceChain(true);

        // Configurar recursos estáticos para documentación
        registry.addResourceHandler("/docs/**")
                .addResourceLocations("classpath:/static/docs/")
                .setCachePeriod(86400)
                .resourceChain(true);
    }

    /**
     * Configuración de tipos de contenido
     *
     * @param configurer configurador de tipos de contenido
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .favorParameter(false)
                .favorPathExtension(false)
                .ignoreAcceptHeader(false)
                .useRegisteredExtensionsOnly(false)
                .defaultContentType(MediaType.APPLICATION_JSON)
                .mediaType("json", MediaType.APPLICATION_JSON)
                .mediaType("xml", MediaType.APPLICATION_XML)
                .mediaType("html", MediaType.TEXT_HTML);
    }

    /**
     * Configuración de rutas de vista simple
     *
     * @param registry registro de controladores de vista
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Páginas de error personalizadas
        registry.addViewController("/error/403").setViewName("error/403");
        registry.addViewController("/error/404").setViewName("error/404");
        registry.addViewController("/error/500").setViewName("error/500");
    }

    /**
     * Bean para el resolver de locale
     *
     * @return SessionLocaleResolver configurado
     */
    @Bean("customLocaleResolver")
    public SessionLocaleResolver customLocaleResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(new Locale("es", "CO")); // Español de Colombia
        return resolver;
    }

    /**
     * Interceptor para cambio de idioma
     *
     * @return LocaleChangeInterceptor configurado
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    /**
     * Configuración de Path Matching
     *
     * @param configurer configurador de path matching
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer
                .setUseTrailingSlashMatch(false)
                .setUseSuffixPatternMatch(false);
    }

    /**
     * Configuración de Message Converters para manejo de JSON/XML
     *
     * @param converters lista de convertidores de mensaje
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Los message converters por defecto de Spring Boot son suficientes
        // para el manejo de JSON con Jackson
        WebMvcConfigurer.super.configureMessageConverters(converters);
    }

    /**
     * Configuración de manejo de excepciones personalizadas
     *
     * @param resolvers lista de resolvers de excepciones
     */
    @Override
    public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        WebMvcConfigurer.super.configureHandlerExceptionResolvers(resolvers);
    }
}
