package com.takima.backskeleton;

import com.takima.backskeleton.support.PostgresTestConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@Import(PostgresTestConfiguration.class)
class CatalogCacheHeadersEndToEndTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @ParameterizedTest
    @ValueSource(strings = {"/seasons", "/teams", "/formations"})
    void catalogResponsesCarryCacheControlAndAWeakEtag(String path) {
        ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);

        assertThat(response.getHeaders().getCacheControl()).isEqualTo("max-age=3600, public");
        assertThat(response.getHeaders().getETag()).startsWith("W/\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/teams/13/seasons"})
    void otherEndpointsAreNotCacheable(String path) {
        ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);

        assertThat(response.getHeaders().get(HttpHeaders.CACHE_CONTROL)).isNull();
        assertThat(response.getHeaders().getETag()).isNull();
    }
}
