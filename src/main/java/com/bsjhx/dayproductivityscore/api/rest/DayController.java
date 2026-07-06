package com.bsjhx.dayproductivityscore.api.rest;

import com.bsjhx.dayproductivityscore.api.rest.DayRestApiDto.DayRateRequest;
import com.bsjhx.dayproductivityscore.application.command.DayCommand.RateDay;
import com.bsjhx.dayproductivityscore.application.command.DayCommandHandler;
import com.bsjhx.dayproductivityscore.application.query.DayQuery;
import com.bsjhx.dayproductivityscore.application.query.DayQuery.DayScoreView;
import com.bsjhx.dayproductivityscore.application.query.DayQuery.GetDayScoreDetailsQuery;
import com.bsjhx.dayproductivityscore.application.query.DayQueryService;
import com.bsjhx.dayproductivityscore.domain.DayScore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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
    public ResponseEntity<DayScoreView> get() {
        return dayQueryService.handle(new GetDayScoreDetailsQuery(LocalDate.now()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{day}")
    public ResponseEntity<DayScoreView> getByDay(@PathVariable LocalDate day) {
        return dayQueryService.handle(new GetDayScoreDetailsQuery(day))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/current-week")
    public ResponseEntity<List<DayScoreView>> getCurrentWeek() {
        return ResponseEntity.ok().build();
    }

}
