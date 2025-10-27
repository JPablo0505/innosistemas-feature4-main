package com.udea.innosistemas.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {

    private DatabaseConfig databaseConfig;

    @BeforeEach
    void setUp() {
        databaseConfig = new DatabaseConfig();
        // Configure in-memory H2 so primaryDataSource() can create a working pool
        ReflectionTestUtils.setField(databaseConfig, "databaseUrl", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        ReflectionTestUtils.setField(databaseConfig, "databaseUsername", "sa");
        ReflectionTestUtils.setField(databaseConfig, "databasePassword", "");
        ReflectionTestUtils.setField(databaseConfig, "databaseDriver", "org.h2.Driver");
        ReflectionTestUtils.setField(databaseConfig, "maxPoolSize", 5);
        ReflectionTestUtils.setField(databaseConfig, "minIdle", 1);
        ReflectionTestUtils.setField(databaseConfig, "ddlAuto", "none");
    }

    @AfterEach
    void tearDown() {
        // nothing to do here; individual tests close datasources if created
    }

    @Test
    void shouldCreatePrimaryDataSource_and_getConnection() throws Exception {
        DataSource ds = databaseConfig.primaryDataSource();
        assertNotNull(ds);
        assertTrue(ds instanceof HikariDataSource);

        try (Connection conn = ds.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
        } finally {
            // close Hikari if possible to release threads
            if (ds instanceof HikariDataSource) {
                ((HikariDataSource) ds).close();
            }
        }
    }

    @Test
    void shouldCreateTransactionManager() {
        DataSource ds = databaseConfig.primaryDataSource();
        LocalContainerEntityManagerFactoryBean emf = databaseConfig.entityManagerFactory(ds);
        assertNotNull(emf);
        JpaTransactionManager tm = (JpaTransactionManager) databaseConfig.transactionManager(emf);
        assertNotNull(tm);
        assertNotNull(tm.getEntityManagerFactory());

        // cleanup
        if (ds instanceof HikariDataSource) {
            ((HikariDataSource) ds).close();
        }
    }

    @Test
    void shouldReturnHibernateProperties() {
        Properties props = ReflectionTestUtils.invokeMethod(databaseConfig, "hibernateProperties");
        assertNotNull(props);
        assertTrue(props.containsKey("hibernate.dialect"));
        assertEquals("org.hibernate.dialect.PostgreSQLDialect", props.getProperty("hibernate.dialect"));
    }

    @Test
    void shouldContainTimezoneConfiguration_and_batchAnd_cache() {
        Properties props = ReflectionTestUtils.invokeMethod(databaseConfig, "hibernateProperties");
        assertEquals("America/Bogota", props.getProperty("hibernate.jdbc.time_zone"));
        assertEquals("20", props.getProperty("hibernate.jdbc.batch_size"));
        assertEquals("false", props.getProperty("hibernate.cache.use_second_level_cache"));
    }

    @Test
    void developmentConfigBean_exists() {
        DatabaseConfig.DevelopmentDatabaseConfig dev = new DatabaseConfig.DevelopmentDatabaseConfig();
        Properties p = dev.developmentHibernateProperties();
        assertNotNull(p);
        assertEquals("true", p.getProperty("hibernate.generate_statistics"));
    }

    @Test
    void productionConfigBean_exists() {
        DatabaseConfig.ProductionDatabaseConfig prod = new DatabaseConfig.ProductionDatabaseConfig();
        Properties p = prod.productionHibernateProperties();
        assertNotNull(p);
        assertEquals("false", p.getProperty("hibernate.generate_statistics"));
    }
}