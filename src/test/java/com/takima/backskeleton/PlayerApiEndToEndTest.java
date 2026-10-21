package com.takima.backskeleton;

import com.fasterxml.jackson.databind.JsonNode;
import com.takima.backskeleton.support.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@Import(PostgresTestConfiguration.class)
class PlayerApiEndToEndTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void playerStatsExposeTheRatingStoredInTheDatabase() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/players/11119/stats?season=2021", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("rating").asInt()).isEqualTo(86);
        assertThat(response.getBody().get("teamName").asText()).isEqualTo("Alpha FC");
    }

    @Test
    void unknownPlayerReturnsNotFoundWithJsonMessage() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/players/999/stats?season=2021", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("message").asText()).contains("999");
    }

    @Test
    void searchFindsPlayersIgnoringAccents() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/players/search?query=mbappe", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get(0).get("name").asText()).isEqualTo("Kylian Mbappé");
    }
}
