package io.github.ddmfuhrmann.ledgerpoc.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class AbstractIntegrationTest {

    // Single container for the entire JVM. The Testcontainers JUnit extension
    // (@Container) stops/restarts the container between test classes, which
    // invalidates the HikariCP pool held by Spring's cached ApplicationContext.
    // Starting it once here keeps the port stable across all test classes.
    static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres")
                .withDatabaseName("ledgerpoc")
                .withUsername("test")
                .withPassword("test");
        postgres.start();
    }

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

}