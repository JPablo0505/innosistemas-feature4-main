package com.udea.innosistemas.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Configuración de la base de datos PostgreSQL para InnoSistemas
 * 
 * Esta configuración establece la conexión con la base de datos PostgreSQL
 * utilizando Supabase como servicio de base de datos en la nube, siguiendo
 * los lineamientos establecidos en el documento de arquitectura.
 * 
 * @author Fábrica-Escuela de Software UdeA
 * @version 1.0.0
 */

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "com.udea.innosistemas.repository",
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager"
)
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    @Value("${spring.datasource.driver-class-name}")
    private String databaseDriver;

    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minIdle;

    @Value("${spring.datasource.hikari.idle-timeout:300000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1200000}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.connection-timeout:20000}")
    private long connectionTimeout;

    @Value("${spring.jpa.hibernate.ddl-auto:validate}")
    private String ddlAuto;

    @Value("${spring.jpa.show-sql:false}" )
    private boolean showSql;

    @Bean
    @Primary
    @Profile("!test")
    public DataSource primaryDataSource() {
        log.info("Configurando DataSource principal para PostgreSQL");
        
        HikariConfig config = new HikariConfig();
        
        config.setJdbcUrl(databaseUrl);
        config.setUsername(databaseUsername);
        config.setPassword(databasePassword);
        config.setDriverClassName(databaseDriver);
        
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setConnectionTimeout(connectionTimeout);
        
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        config.setLeakDetectionThreshold(60000);
        
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        config.setPoolName("InnoSistemas-HikariCP");
        config.setAutoCommit(true);
        
        log.info("DataSource configurado exitosamente - Pool: {} conexiones", maxPoolSize);
        
        return new HikariDataSource(config);
    }

    /**
     * Configuración del EntityManagerFactory para JPA/Hibernate
     * 
     * @param dataSource el DataSource configurado
     * @return LocalContainerEntityManagerFactoryBean
     */
    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        log.info("Configurando EntityManagerFactory para JPA");
        
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.udea.innosistemas.entity");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(false);
        vendorAdapter.setShowSql(showSql);
        em.setJpaVendorAdapter(vendorAdapter);

        em.setJpaProperties(hibernateProperties());

        em.afterPropertiesSet();

        return em;
    }

    /**
     * Configuración del TransactionManager
     * 
     * @param entityManagerFactory el EntityManagerFactory configurado
     * @return PlatformTransactionManager
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean emf) {
        log.info("Configurando TransactionManager para JPA");
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf.getObject());
        return transactionManager;
    }

    private Properties hibernateProperties() {
        Properties props = new Properties();
        props.setProperty("hibernate.hbm2ddl.auto", ddlAuto != null ? ddlAuto : "validate");
        props.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        props.setProperty("hibernate.jdbc.time_zone", "America/Bogota");
        props.setProperty("hibernate.show_sql", String.valueOf(showSql));
        props.setProperty("hibernate.jdbc.batch_size", "20");
        props.setProperty("hibernate.cache.use_second_level_cache", "false");
        props.setProperty("hibernate.generate_statistics", "false");
        props.setProperty("hibernate.order_inserts", "true");
        props.setProperty("hibernate.order_updates", "true");
        return props;
    }

    @Configuration
    @Profile("dev")
    static class DevelopmentDatabaseConfig {
        
        @Bean
        public Properties developmentHibernateProperties() {
            Properties properties = new Properties();
            properties.setProperty("hibernate.generate_statistics", "true");
            properties.setProperty("hibernate.session.events.log", "true");
            properties.setProperty("hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS", "1000");
            
            return properties;
        }
    }

    @Configuration
    @Profile("prod")
    static class ProductionDatabaseConfig {
        
        @Bean
        public Properties productionHibernateProperties() {
            Properties properties = new Properties();
            properties.setProperty("hibernate.generate_statistics", "false");
            properties.setProperty("hibernate.show_sql", "false");
            properties.setProperty("hibernate.format_sql", "false");
            
            return properties;
        }
    }
}