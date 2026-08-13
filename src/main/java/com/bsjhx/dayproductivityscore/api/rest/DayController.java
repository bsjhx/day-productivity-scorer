package com.bsjhx.dayproductivityscore.api.rest;

import com.bsjhx.dayproductivityscore.api.rest.DayRestApiDto.DayRateRequest;
import com.bsjhx.dayproductivityscore.api.rest.DayRestApiDto.SingleDayResponse;
import com.bsjhx.dayproductivityscore.application.command.DayCommand.RateDay;
import com.bsjhx.dayproductivityscore.application.command.DayCommandHandler;
import com.bsjhx.dayproductivityscore.application.query.DayQuery.DayScoreView;
import com.bsjhx.dayproductivityscore.application.query.DayQuery.GetDaysInRangeQuery;
import com.bsjhx.dayproductivityscore.application.query.DayQueryService;
import com.bsjhx.dayproductivityscore.domain.DayScore;
import com.bsjhx.dayproductivityscore.domain.DomainException;
import com.bsjhx.dayproductivityscore.infrastructure.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/day")
public class DayController {

    private final DayCommandHandler dayCommandHandler;
    private final DayQueryService dayQueryService;

    public DayController(DayCommandHandler dayCommandHandler, DayQueryService dayQueryService) {
        this.dayCommandHandler = dayCommandHandler;
        this.dayQueryService = dayQueryService;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/")
    public ResponseEntity<Void> post(@RequestBody DayRateRequest request, Authentication authentication) {
        UUID userId = getUserId(authentication);
        dayCommandHandler.handle(new RateDay(userId, request.day(), DayScore.withScore(request.score())));
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/")
    public ResponseEntity<List<SingleDayResponse>> getByDay(
            @RequestParam("from") LocalDate from,
            @RequestParam(value = "to", required = false) LocalDate to,
            Authentication authentication) {

        UUID userId = getUserId(authentication);
        List<DayScoreView> views = dayQueryService.handle(new GetDaysInRangeQuery(userId, from, to));

        List<SingleDayResponse> responses = views.stream()
                .map(v -> new SingleDayResponse(v.date(), v.score(), v.locked()))
                .toList();

        return ResponseEntity.ok(responses);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException e) {
        ErrorResponse errorResponse = new ErrorResponse(
                "DOMAIN_ERROR",
                e.getMessage(),
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    public record ErrorResponse(String error, String message, Instant timestamp) {
    }

    private UUID getUserId(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return principal.getId();
    }

}
