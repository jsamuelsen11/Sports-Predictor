package com.sportspredictor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Verifies the Spring Boot application context loads successfully. */
@SpringBootTest
class SportsPredictorApplicationTest {

    @Test
    void contextLoads() {}

    @Test
    void mainMethodRuns() {
        SportsPredictorApplication.main(new String[] {});
    }
}
