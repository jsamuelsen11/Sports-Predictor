package com.sportspredictor.mcpserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Verifies the Spring Boot application context loads successfully. */
@SpringBootTest
class SportsPredictorMcpServerApplicationTest {

    @Test
    void contextLoads() {}

    @Test
    void mainMethodRuns() {
        SportsPredictorMcpServerApplication.main(new String[] {});
    }
}
