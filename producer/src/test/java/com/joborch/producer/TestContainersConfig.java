package com.joborch.producer;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers configuration for all integration tests.
 *
 * @ServiceConnection: Spring Boot 3.1+ auto-configures DataSource and
 * RabbitMQ ConnectionFactory from container properties — no manual property overrides needed.
 *
 * Containers are static (class-level) → shared across all tests in the same JVM run.
 * This avoids starting/stopping containers for every test class (very slow).
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("joborch_test")
                .withUsername("test_user")
                .withPassword("test_pass")
                .withReuse(true); // reuse between test runs (requires ~/.testcontainers.properties)
    }

    @Bean
    @ServiceConnection
    public RabbitMQContainer rabbitMQContainer() {
        return new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"))
                .withReuse(true);
    }
}
