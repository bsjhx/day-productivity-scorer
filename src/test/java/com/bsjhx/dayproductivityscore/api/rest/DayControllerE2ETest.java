package com.bsjhx.dayproductivityscore.api.rest;

import com.bsjhx.dayproductivityscore.api.rest.DayRestApiDto.DayRateRequest;
import com.bsjhx.dayproductivityscore.api.rest.DayRestApiDto.SingleDayResponse;
import com.bsjhx.dayproductivityscore.infrastructure.command.event.EventStoreJdbcRepository;
import com.bsjhx.dayproductivityscore.infrastructure.query.DayProjectionJdbcRepository;
import com.bsjhx.dayproductivityscore.infrastructure.security.TestSecurityConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfiguration.class)
class DayControllerE2ETest {

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;

    @Autowired
    private EventStoreJdbcRepository eventStoreRepository;

    @Autowired
    private DayProjectionJdbcRepository projectionRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        baseUrl = "http://localhost:" + port + "/day/";
        eventStoreRepository.deleteAll();
        projectionRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        eventStoreRepository.deleteAll();
        projectionRepository.deleteAll();
    }

    @Test
    void shouldRateDayViaRestApi() {
        // given
        DayRateRequest request = new DayRateRequest(LocalDate.of(2026, 7, 23), 5);

        // when
        ResponseEntity<Void> response = restTemplate.postForEntity(baseUrl, request, Void.class);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldGetRatedDayViaRestApi() {
        // given
        rateDay(LocalDate.of(2026, 7, 23), 4);

        // when
        List<SingleDayResponse> days = getDaysInRange(LocalDate.of(2026, 7, 23), null);

        // then
        assertEquals(1, days.size());
        SingleDayResponse day = days.get(0);
        assertEquals(LocalDate.of(2026, 7, 23), day.day());
        assertEquals(4, day.score());
        assertFalse(day.isLocked());
    }

    @Test
    void shouldGetMultipleDaysInRange() {
        // given
        rateDay(LocalDate.of(2026, 7, 20), 2);
        rateDay(LocalDate.of(2026, 7, 21), 3);
        rateDay(LocalDate.of(2026, 7, 22), 4);
        rateDay(LocalDate.of(2026, 7, 23), 5);

        // when
        List<SingleDayResponse> days = getDaysInRange(
            LocalDate.of(2026, 7, 20),
            LocalDate.of(2026, 7, 22)
        );

        // then
        assertEquals(3, days.size());
        assertEquals(LocalDate.of(2026, 7, 20), days.get(0).day());
        assertEquals(LocalDate.of(2026, 7, 21), days.get(1).day());
        assertEquals(LocalDate.of(2026, 7, 22), days.get(2).day());
    }

    @Test
    void shouldGetAllDaysFromDateWhenToNotProvided() {
        // given
        rateDay(LocalDate.of(2026, 7, 20), 1);
        rateDay(LocalDate.of(2026, 7, 21), 2);
        rateDay(LocalDate.of(2026, 7, 22), 3);
        rateDay(LocalDate.of(2026, 7, 23), 4);

        // when
        List<SingleDayResponse> days = getDaysInRange(LocalDate.of(2026, 7, 21), null);

        // then
        assertEquals(3, days.size());
        assertEquals(LocalDate.of(2026, 7, 21), days.get(0).day());
        assertEquals(LocalDate.of(2026, 7, 22), days.get(1).day());
        assertEquals(LocalDate.of(2026, 7, 23), days.get(2).day());
    }

    @Test
    void shouldUpdateDayScoreViaMultipleRequests() {
        // given
        rateDay(LocalDate.of(2026, 7, 23), 2);

        // when
        rateDay(LocalDate.of(2026, 7, 23), 5);

        // then
        List<SingleDayResponse> days = getDaysInRange(LocalDate.of(2026, 7, 23), null);
        assertEquals(1, days.size());
        assertEquals(5, days.get(0).score());
    }

    @Test
    void shouldHandleAllValidScoreValues() {
        // given & when
        rateDay(LocalDate.of(2026, 5, 1), -1); // NONE
        rateDay(LocalDate.of(2026, 5, 2), 0);  // ZERO
        rateDay(LocalDate.of(2026, 5, 3), 1);  // ONE
        rateDay(LocalDate.of(2026, 5, 4), 2);  // TWO
        rateDay(LocalDate.of(2026, 5, 5), 3);  // THREE
        rateDay(LocalDate.of(2026, 5, 6), 4);  // FOUR
        rateDay(LocalDate.of(2026, 5, 7), 5);  // FIVE

        // then
        List<SingleDayResponse> days = getDaysInRange(
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 7)
        );

        assertEquals(7, days.size());
        assertEquals(-1, days.get(0).score());
        assertEquals(0, days.get(1).score());
        assertEquals(1, days.get(2).score());
        assertEquals(5, days.get(6).score());
    }

    @Test
    void shouldReturnErrorForInvalidScore() {
        // given
        DayRateRequest request = new DayRateRequest(LocalDate.of(2026, 7, 23), 99);

        // when & then
        assertThrows(
            HttpServerErrorException.class,
            () -> restTemplate.postForEntity(baseUrl, request, Void.class)
        );
    }

    @Test
    void shouldReturnEmptyListWhenNoDataInRange() {
        // when
        List<SingleDayResponse> days = getDaysInRange(
            LocalDate.of(2026, 7, 20),
            LocalDate.of(2026, 7, 25)
        );

        // then
        assertTrue(days.isEmpty());
    }

    @Test
    void shouldHandleCompleteE2EWorkflow() {
        // Step 1: Rate multiple days
        rateDay(LocalDate.of(2026, 6, 10), 2);
        rateDay(LocalDate.of(2026, 6, 11), 3);
        rateDay(LocalDate.of(2026, 6, 12), 4);

        // Step 2: Query and verify
        List<SingleDayResponse> days1 = getDaysInRange(
            LocalDate.of(2026, 6, 10),
            LocalDate.of(2026, 6, 12)
        );
        assertEquals(3, days1.size());

        // Step 3: Update one day
        rateDay(LocalDate.of(2026, 6, 11), 5);

        // Step 4: Verify update
        List<SingleDayResponse> days2 = getDaysInRange(
            LocalDate.of(2026, 6, 11),
            LocalDate.of(2026, 6, 11)
        );
        assertEquals(1, days2.size());
        assertEquals(5, days2.get(0).score());

        // Step 5: Add more days
        rateDay(LocalDate.of(2026, 6, 13), 4);
        rateDay(LocalDate.of(2026, 6, 14), 5);

        // Step 6: Query all
        List<SingleDayResponse> days3 = getDaysInRange(LocalDate.of(2026, 6, 10), null);
        assertEquals(5, days3.size());
    }

    @Test
    void shouldHandleDateBoundaries() {
        // given
        rateDay(LocalDate.of(2026, 1, 1), 1);
        rateDay(LocalDate.of(2026, 7, 22), 5);

        // when & then
        List<SingleDayResponse> startOfYear = getDaysInRange(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 1)
        );
        assertEquals(1, startOfYear.size());
        assertEquals(1, startOfYear.get(0).score());

        List<SingleDayResponse> recentDate = getDaysInRange(
            LocalDate.of(2026, 7, 22),
            LocalDate.of(2026, 7, 22)
        );
        assertEquals(1, recentDate.size());
        assertEquals(5, recentDate.get(0).score());
    }

    @Test
    void shouldReturnDaysInCorrectOrder() {
        // given - rate days in random order
        rateDay(LocalDate.of(2026, 7, 23), 3);
        rateDay(LocalDate.of(2026, 7, 20), 1);
        rateDay(LocalDate.of(2026, 7, 22), 2);
        rateDay(LocalDate.of(2026, 7, 21), 4);

        // when
        List<SingleDayResponse> days = getDaysInRange(
            LocalDate.of(2026, 7, 20),
            LocalDate.of(2026, 7, 23)
        );

        // then - should be in ascending date order
        assertEquals(4, days.size());
        assertEquals(LocalDate.of(2026, 7, 20), days.get(0).day());
        assertEquals(LocalDate.of(2026, 7, 21), days.get(1).day());
        assertEquals(LocalDate.of(2026, 7, 22), days.get(2).day());
        assertEquals(LocalDate.of(2026, 7, 23), days.get(3).day());
    }

    // Helper methods

    private void rateDay(LocalDate date, int score) {
        DayRateRequest request = new DayRateRequest(date, score);
        ResponseEntity<Void> response = restTemplate.postForEntity(baseUrl, request, Void.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private List<SingleDayResponse> getDaysInRange(LocalDate from, LocalDate to) {
        String url = baseUrl + "?from=" + from;
        if (to != null) {
            url += "&to=" + to;
        }

        ResponseEntity<List<SingleDayResponse>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }
}
