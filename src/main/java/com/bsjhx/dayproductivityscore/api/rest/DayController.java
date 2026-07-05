package com.bsjhx.dayproductivityscore.api.rest;

import com.bsjhx.dayproductivityscore.api.rest.DayRestApiDto.DayRateRequest;
import com.bsjhx.dayproductivityscore.application.command.DayCommand.RateDay;
import com.bsjhx.dayproductivityscore.application.command.DayCommandHandler;
import com.bsjhx.dayproductivityscore.application.query.DayQuery;
import com.bsjhx.dayproductivityscore.application.query.DayQuery.GetDayScoreDetailsQuery;
import com.bsjhx.dayproductivityscore.application.query.DayQueryService;
import com.bsjhx.dayproductivityscore.domain.DayScore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/day")
public class DayController {

    private final DayCommandHandler dayCommandHandler;
    private final DayQueryService dayQueryService;

    public DayController(DayCommandHandler dayCommandHandler, DayQueryService dayQueryService) {
        this.dayCommandHandler = dayCommandHandler;
        this.dayQueryService = dayQueryService;
    }

    @PostMapping("/")
    public ResponseEntity<Void> post(@RequestBody DayRateRequest request) {
        dayCommandHandler.handle(new RateDay(request.day(), DayScore.withScore(request.score())));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/today")
    public ResponseEntity<?> get() {
        return ResponseEntity.ok(dayQueryService.handle(new GetDayScoreDetailsQuery(LocalDate.now())));
    }

    @GetMapping("/{day}")
    public ResponseEntity<?> getByDay(@PathVariable LocalDate day) {
        return ResponseEntity.ok(dayQueryService.handle(new GetDayScoreDetailsQuery(day)));
    }

}
