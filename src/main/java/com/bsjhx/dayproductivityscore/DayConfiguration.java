package com.bsjhx.dayproductivityscore;

import com.bsjhx.dayproductivityscore.application.command.DayCommandHandler;
import com.bsjhx.dayproductivityscore.application.query.DayQueryService;
import com.bsjhx.dayproductivityscore.application.query.QueryDayRepository;
import com.bsjhx.dayproductivityscore.domain.port.CommandDayRepository;
import com.bsjhx.dayproductivityscore.infrastructure.db.query.DayProjectionUpdater;
import com.bsjhx.dayproductivityscore.infrastructure.db.query.InMemoryQueryDayRepository;
import com.bsjhx.dayproductivityscore.infrastructure.db.command.InMemoryCommandDayRepository;
import com.bsjhx.dayproductivityscore.infrastructure.db.command.InMemoryWriteMemoryDb;
import com.bsjhx.dayproductivityscore.infrastructure.db.query.InMemoryReadDb;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DayConfiguration {

    @Bean
    public DayCommandHandler dayCommandHandler(CommandDayRepository dayRepository) {
        return new DayCommandHandler(dayRepository);
    }

    @Bean
    public DayQueryService dayQueryService(QueryDayRepository repository) {
        return new DayQueryService(repository);
    }

    @Bean
    public CommandDayRepository inMemoryDayRepository(InMemoryWriteMemoryDb inMemoryDb, ApplicationEventPublisher eventPublisher) {
        return new InMemoryCommandDayRepository(inMemoryDb, eventPublisher);
    }

    @Bean
    public QueryDayRepository inMemoryDayReadOnlyRepository(InMemoryReadDb inMemoryDb) {
        return new InMemoryQueryDayRepository(inMemoryDb);
    }

    @Bean
    public InMemoryWriteMemoryDb inMemoryWriteMemoryDb() {
        return new InMemoryWriteMemoryDb();
    }

    @Bean
    public InMemoryReadDb inMemoryReadDb() {
        return new InMemoryReadDb();
    }

    @Bean
    public DayProjectionUpdater dayProjectionUpdater(InMemoryReadDb inMemoryReadDb) {
        return new DayProjectionUpdater(inMemoryReadDb);
    }

}
