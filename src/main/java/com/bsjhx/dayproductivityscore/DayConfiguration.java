package com.bsjhx.dayproductivityscore;

import com.bsjhx.dayproductivityscore.application.command.DayCommandHandler;
import com.bsjhx.dayproductivityscore.application.query.DayQueryService;
import com.bsjhx.dayproductivityscore.domain.port.DayReadOnlyRepository;
import com.bsjhx.dayproductivityscore.domain.port.DayRepository;
import com.bsjhx.dayproductivityscore.infrastructure.InMemoryDayRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DayConfiguration {

    @Bean
    public DayCommandHandler dayCommandHandler(DayRepository dayRepository) {
        return new DayCommandHandler(dayRepository);
    }

    @Bean
    public DayQueryService dayQueryService(DayReadOnlyRepository repository) {
        return new DayQueryService(repository);
    }

    @Bean
    public InMemoryDayRepository inMemoryDayRepository() {
        return new InMemoryDayRepository();
    }

}
