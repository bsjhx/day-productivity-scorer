package com.bsjhx.dayproductivityscore.api.rest;

import com.bsjhx.dayproductivityscore.api.rest.DayRestApiDto.DayRateRequest;
import com.bsjhx.dayproductivityscore.api.rest.DayRestApiDto.SingleDayResponse;
import com.bsjhx.dayproductivityscore.application.command.DayCommand.RateDay;
import com.bsjhx.dayproductivityscore.application.command.DayCommandHandler;
import com.bsjhx.dayproductivityscore.application.query.DayQuery.DayScoreView;
import com.bsjhx.dayproductivityscore.application.query.DayQuery.GetDaysInRangeQuery;
import com.bsjhx.dayproductivityscore.application.query.DayQueryService;
import com.bsjhx.dayproductivityscore.domain.DayScore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/day")
public class DayController {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final DayCommandHandler dayCommandHandler;
    private final DayQueryService dayQueryService;

    public DayController(DayCommandHandler dayCommandHandler, DayQueryService dayQueryService) {
        this.dayCommandHandler = dayCommandHandler;
        this.dayQueryService = dayQueryService;
    }

    @PostMapping("/")
    public ResponseEntity<Void> post(@RequestBody DayRateRequest request) {
        dayCommandHandler.handle(new RateDay(USER_ID, request.day(), DayScore.withScore(request.score())));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/")
    public ResponseEntity<List<SingleDayResponse>> getByDay(@RequestParam("from") LocalDate from, @RequestParam(value = "to", required = false) LocalDate to) {
        List<DayScoreView> views = dayQueryService.handle(new GetDaysInRangeQuery(USER_ID, from, to));

        List<SingleDayResponse> responses = views.stream()
                .map(v -> new SingleDayResponse(v.date(), v.score(), v.locked()))
                .toList();

        return ResponseEntity.ok(responses);
    }

}
