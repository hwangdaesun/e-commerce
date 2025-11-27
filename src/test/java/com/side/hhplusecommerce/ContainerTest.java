package com.side.hhplusecommerce;

import com.side.hhplusecommerce.support.DatabaseClearExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

@ActiveProfiles("test")
@ExtendWith(DatabaseClearExtension.class)
public abstract class ContainerTest {
    static final String MYSQL_IMAGE = "mysql:8.0";
    static final String REDIS_IMAGE = "redis:7-alpine";
    static final MySQLContainer<?> MYSQL_CONTAINER;
    static final GenericContainer REDIS_CONTAINER;

    static {
        MYSQL_CONTAINER = new MySQLContainer<>(MYSQL_IMAGE)
                .withDatabaseName("ecommerce_test")
                .withUsername("test_user")
                .withPassword("test_pass")
                .withReuse(true);
        MYSQL_CONTAINER.start();

        REDIS_CONTAINER = new GenericContainer<>(REDIS_IMAGE).withExposedPorts(6379).withReuse(true);
        REDIS_CONTAINER.start();
    }

    @DynamicPropertySource
    public static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL_CONTAINER::getDriverClassName);

        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379).toString());
        registry.add("spring.data.redis.password", () -> "");
    }
}
