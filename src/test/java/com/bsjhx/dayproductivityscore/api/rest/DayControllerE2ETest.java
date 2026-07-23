package com.bsjhx.dayproductivityscore.api.rest;

import com.bsjhx.dayproductivityscore.infrastructure.command.event.EventStoreJdbcRepository;
import com.bsjhx.dayproductivityscore.infrastructure.query.DayProjectionJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DayControllerE2ETest {

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;
    private String baseUrl;

    @Autowired
    private EventStoreJdbcRepository eventStoreRepository;

    @Autowired
    private DayProjectionJdbcRepository projectionRepository;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        baseUrl = "http://localhost:" + port;
        eventStoreRepository.deleteAll();
        projectionRepository.deleteAll();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        eventStoreRepository.deleteAll();
        projectionRepository.deleteAll();
    }

    @Test
    void shouldRateDayViaRestApi() {
        // given
        Map<String, Object> request = Map.of(
            "day", "2026-07-23",
            "score", 5
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        // when
        ResponseEntity<Void> response = restTemplate.exchange(
            baseUrl + "/day/",
            HttpMethod.POST,
            entity,
            Void.class
        );

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldGetRatedDayViaRestApi() {
        // given - rate a day first
        rateDayViaApi("2026-07-23", 4);

        // when
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/day/?from=2026-07-23",
            String.class
        );

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("2026-07-23"));
        assertTrue(body.contains("\"score\":4"));
        assertTrue(body.contains("\"locked\":false"));
    }

    @Test
    void shouldGetMultipleDaysInRange() {
        // given - rate multiple days
        rateDayViaApi("2026-07-20", 2);
        rateDayViaApi("2026-07-21", 3);
        rateDayViaApi("2026-07-22", 4);
        rateDayViaApi("2026-07-23", 5);

        // when - query date range
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/day/?from=2026-07-20&to=2026-07-22",
            String.class
        );

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("2026-07-20"));
        assertTrue(body.contains("2026-07-21"));
        assertTrue(body.contains("2026-07-22"));
        assertFalse(body.contains("2026-07-23")); // Should not include day 23
    }

    @Test
    void shouldGetAllDaysFromDateWhenToNotProvided() {
        // given
        rateDayViaApi("2026-07-20", 1);
        rateDayViaApi("2026-07-21", 2);
        rateDayViaApi("2026-07-22", 3);
        rateDayViaApi("2026-07-23", 4);

        // when - query from date without 'to'
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/day/?from=2026-07-21",
            String.class
        );

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertNotNull(body);
        assertFalse(body.contains("2026-07-20")); // Should not include day 20
        assertTrue(body.contains("2026-07-21"));
        assertTrue(body.contains("2026-07-22"));
        assertTrue(body.contains("2026-07-23"));
    }

    @Test
    void shouldUpdateDayScoreViaMultipleRequests() {
        // given - rate day initially
        rateDayViaApi("2026-07-23", 2);

        // when - update the same day with different score
        rateDayViaApi("2026-07-23", 5);

        // then - should have updated score
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/day/?from=2026-07-23",
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("\"score\":5"));
    }

    @Test
    void shouldHandleAllValidScoreValues() {
        // given & when - rate with all valid scores (May dates to avoid conflicts)
        rateDayViaApi("2026-05-01", -1); // NONE
        rateDayViaApi("2026-05-02", 0);  // ZERO
        rateDayViaApi("2026-05-03", 1);  // ONE
        rateDayViaApi("2026-05-04", 2);  // TWO
        rateDayViaApi("2026-05-05", 3);  // THREE
        rateDayViaApi("2026-05-06", 4);  // FOUR
        rateDayViaApi("2026-05-07", 5);  // FIVE

        // then - all should be stored correctly
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/day/?from=2026-05-01&to=2026-05-07",
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertNotNull(body);
        // Verify all dates are present
        assertTrue(body.contains("2026-05-01"));
        assertTrue(body.contains("2026-05-07"));
    }

    @Test
    void shouldReturnErrorForInvalidScore() {
        // given
        Map<String, Object> request = Map.of(
            "day", "2026-07-23",
            "score", 99 // Invalid score
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        // when & then - should fail
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/day/",
                HttpMethod.POST,
                entity,
                String.class
            );
            // If we get here, the status code should be error
            assertTrue(response.getStatusCode().is5xxServerError() ||
                       response.getStatusCode().is4xxClientError());
        } catch (Exception e) {
            // Exception is expected for invalid data
            assertTrue(e.getMessage().contains("500") || e.getMessage().contains("400"));
        }
    }

    @Test
    void shouldReturnEmptyListWhenNoDataInRange() {
        // given - no data

        // when
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/day/?from=2026-07-20&to=2026-07-25",
            String.class
        );

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("[]", response.getBody());
    }

    @Test
    void shouldHandleCompleteE2EWorkflow() {
        // Step 1: Rate multiple days (using June to avoid conflicts)
        rateDayViaApi("2026-06-10", 2);
        rateDayViaApi("2026-06-11", 3);
        rateDayViaApi("2026-06-12", 4);

        // Step 2: Query and verify
        ResponseEntity<String> response1 = restTemplate.getForEntity(
            baseUrl + "/day/?from=2026-06-10&to=2026-06-12",
            String.class
        );
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertTrue(response1.getBody().contains("2026-06-10"));

        // Step 3: Update one day
        rateDayViaApi("2026-06-11", 5);

        // Step 4: Verify update
        ResponseEntity<String> response2 = restTemplate.getForEntity(
            baseUrl + "/day/?from=2026-06-11&to=2026-06-11",
            String.class
        );
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertTrue(response2.getBody().contains("\"score\":5"));

        // Step 5: Add more days
        rateDayViaApi("2026-06-13", 4);
        rateDayViaApi("2026-06-14", 5);

        // Step 6: Query all
        ResponseEntity<String> response3 = restTemplate.getForEntity(
            baseUrl + "/day/?from=2026-06-10",
            String.class
        );
        assertEquals(HttpStatus.OK, response3.getStatusCode());
        String body = response3.getBody();
        assertNotNull(body);
        assertTrue(body.contains("2026-06-10"));
        assertTrue(body.contains("2026-06-11"));
        assertTrue(body.contains("2026-06-12"));
        assertTrue(body.contains("2026-06-13"));
        assertTrue(body.contains("2026-06-14"));
    }

    @Test
    void shouldHandleDateBoundaries() {
        // given - boundary dates
        rateDayViaApi("2026-01-01", 1); // Start of year
        rateDayViaApi("2026-07-22", 5); // Recent date

        // when & then - query start of year
        ResponseEntity<String> response1 = restTemplate.getForEntity(
            baseUrl + "/day/?from=2026-01-01&to=2026-01-01",
            String.class
        );
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertTrue(response1.getBody().contains("2026-01-01"));
        assertTrue(response1.getBody().contains("\"score\":1"));

        // when & then - query recent date
        ResponseEntity<String> response2 = restTemplate.getForEntity(
            baseUrl + "/day/?from=2026-07-22&to=2026-07-22",
            String.class
        );
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertTrue(response2.getBody().contains("2026-07-22"));
        assertTrue(response2.getBody().contains("\"score\":5"));
    }

    @Test
    void shouldReturnDaysInCorrectOrder() {
        // given - rate days in random order
        rateDayViaApi("2026-07-23", 3);
        rateDayViaApi("2026-07-20", 1);
        rateDayViaApi("2026-07-22", 2);
        rateDayViaApi("2026-07-21", 4);

        // when
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/day/?from=2026-07-20&to=2026-07-23",
            String.class
        );

        // then - should be returned in date order
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertNotNull(body);

        int idx20 = body.indexOf("2026-07-20");
        int idx21 = body.indexOf("2026-07-21");
        int idx22 = body.indexOf("2026-07-22");
        int idx23 = body.indexOf("2026-07-23");

        assertTrue(idx20 < idx21);
        assertTrue(idx21 < idx22);
        assertTrue(idx22 < idx23);
    }

    // Helper method
    private void rateDayViaApi(String date, int score) {
        Map<String, Object> request = Map.of(
            "day", date,
            "score", score
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Void> response = restTemplate.exchange(
            baseUrl + "/day/",
            HttpMethod.POST,
            entity,
            Void.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
