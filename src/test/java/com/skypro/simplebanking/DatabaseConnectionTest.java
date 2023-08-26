package com.skypro.simplebanking;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = SimpleBankingApplication.class)
@TestPropertySource(properties = {
        "spring.test.database.replace=none",
        "spring.datasource.url=jdbc:tc:postgresql:15.2-alpine:///db"
})
public class DatabaseConnectionTest {
    @Autowired
    private DataSource dataSource;

    @Test
    void testPostgresql() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            assertThat(conn).isNotNull();
        }
    }
    @Test
    void testDatabaseConnection() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            AssertionsForClassTypes.assertThat(conn).isNotNull();
        }
    }
}
