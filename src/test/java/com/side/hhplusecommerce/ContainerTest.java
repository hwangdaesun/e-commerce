package com.side.hhplusecommerce;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

@ActiveProfiles("test")
public abstract class ContainerTest {
    static final String MYSQL_IMAGE = "mysql:8.0";
    static final MySQLContainer<?> MYSQL_CONTAINER;

    static {
        MYSQL_CONTAINER = new MySQLContainer<>(MYSQL_IMAGE)
                .withDatabaseName("ecommerce_test")
                .withUsername("test_user")
                .withPassword("test_pass")
                .withReuse(true);
        MYSQL_CONTAINER.start();
    }

    @DynamicPropertySource
    public static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL_CONTAINER::getDriverClassName);
    }
}
