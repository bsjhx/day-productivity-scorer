package com.bsjhx.dayproductivityscore;

import com.bsjhx.dayproductivityscore.application.command.DayCommandHandler;
import com.bsjhx.dayproductivityscore.application.query.DayQueryService;
import com.bsjhx.dayproductivityscore.application.query.QueryDayRepository;
import com.bsjhx.dayproductivityscore.domain.port.CommandDayRepository;
import com.bsjhx.dayproductivityscore.infrastructure.event.InMemoryEventStore;
import com.bsjhx.dayproductivityscore.infrastructure.query.DayProjectionUpdater;
import com.bsjhx.dayproductivityscore.infrastructure.query.InMemoryQueryDayRepository;
import com.bsjhx.dayproductivityscore.infrastructure.command.InMemoryCommandDayRepository;
import com.bsjhx.dayproductivityscore.infrastructure.query.InMemoryReadDb;
import org.flywaydb.core.Flyway;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

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
    public CommandDayRepository inMemoryDayRepository(InMemoryEventStore eventStore, ApplicationEventPublisher eventPublisher) {
        return new InMemoryCommandDayRepository(eventStore, eventPublisher);
    }

    @Bean
    public QueryDayRepository inMemoryDayReadOnlyRepository(InMemoryReadDb inMemoryDb) {
        return new InMemoryQueryDayRepository(inMemoryDb);
    }

    @Bean
    public InMemoryEventStore inMemoryEventStore() {
        return new InMemoryEventStore();
    }

    @Bean
    public InMemoryReadDb inMemoryReadDb() {
        return new InMemoryReadDb();
    }

    @Bean
    public DayProjectionUpdater dayProjectionUpdater(InMemoryReadDb inMemoryReadDb) {
        return new DayProjectionUpdater(inMemoryReadDb);
    }

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
    }

}
