package com.bsjhx.dayproductivityscore;

import com.bsjhx.dayproductivityscore.application.command.DayCommandHandler;
import com.bsjhx.dayproductivityscore.application.query.DayQueryService;
import com.bsjhx.dayproductivityscore.application.query.QueryDayRepository;
import com.bsjhx.dayproductivityscore.domain.port.CommandDayRepository;
import com.bsjhx.dayproductivityscore.infrastructure.command.EventStoreRepository;
import com.bsjhx.dayproductivityscore.infrastructure.command.event.SpringDataJdbcEventStoreRepository;
import com.bsjhx.dayproductivityscore.infrastructure.query.DayProjectionUpdater;
import com.bsjhx.dayproductivityscore.infrastructure.query.DayQueryRepository;
import com.bsjhx.dayproductivityscore.infrastructure.query.SpringDataJdbcDayProjectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
    public DayProjectionUpdater dayProjectionUpdater(SpringDataJdbcDayProjectionRepository inMemoryReadDb) {
        return new DayProjectionUpdater(inMemoryReadDb);
    }

    @Bean
    public DayQueryService dayQueryService(QueryDayRepository repository) {
        return new DayQueryService(repository);
    }

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
    }

    @Bean
    public CommandDayRepository sqlEventSourcedDayRepository(SpringDataJdbcEventStoreRepository repository, ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher) {
        return new EventStoreRepository(repository, objectMapper, eventPublisher);
    }

    @Bean
    public DayQueryRepository dayQueryRepository(SpringDataJdbcDayProjectionRepository repository) {
        return new DayQueryRepository(repository);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
