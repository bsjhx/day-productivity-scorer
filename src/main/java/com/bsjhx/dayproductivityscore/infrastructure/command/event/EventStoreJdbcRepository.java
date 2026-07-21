package com.bsjhx.dayproductivityscore.infrastructure.command.event;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface EventStoreJdbcRepository extends CrudRepository<EventStoreEntity, Long> {

    @Query("SELECT * FROM event_store WHERE aggregate_id = :aggregateId ORDER BY version ASC")
    List<EventStoreEntity> findByAggregateId(@Param("aggregateId") String aggregateId);

    @Query("SELECT MAX(version) FROM event_store WHERE aggregate_id = :aggregateId")
    Integer findMaxVersionByAggregateId(@Param("aggregateId") String aggregateId);
}