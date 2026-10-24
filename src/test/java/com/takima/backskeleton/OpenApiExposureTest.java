package com.takima.backskeleton;

import com.takima.backskeleton.support.PostgresTestConfiguration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiExposureTest {
    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @Testcontainers(disabledWithoutDocker = true)
    @Import(PostgresTestConfiguration.class)
    class ByDefault {
        @Autowired
        private TestRestTemplate restTemplate;

        @Test
        void apiDocsAreNotServed() {
            assertThat(restTemplate.getForEntity("/v3/api-docs", String.class).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void swaggerUiIsNotServed() {
            assertThat(restTemplate.getForEntity("/swagger-ui/index.html", String.class).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "SWAGGER_ENABLED=true")
    @Testcontainers(disabledWithoutDocker = true)
    @Import(PostgresTestConfiguration.class)
    class WhenEnabled {
        @Autowired
        private TestRestTemplate restTemplate;

        @Test
        void apiDocsAreServed() {
            assertThat(restTemplate.getForEntity("/v3/api-docs", String.class).getStatusCode())
                    .isEqualTo(HttpStatus.OK);
        }

        @Test
        void swaggerUiIsServed() {
            assertThat(restTemplate.getForEntity("/swagger-ui/index.html", String.class).getStatusCode())
                    .isEqualTo(HttpStatus.OK);
        }
    }
}
