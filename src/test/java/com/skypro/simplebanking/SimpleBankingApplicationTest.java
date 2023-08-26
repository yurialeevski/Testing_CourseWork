package com.skypro.simplebanking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.test.database.replace=none",
        "spring.datasource.url=jdbc:tc:postgresql:15.2-alpine:///db"
})
class SimpleBankingApplicationTest {

    @Test
    void contextLoads() {
    }

}
