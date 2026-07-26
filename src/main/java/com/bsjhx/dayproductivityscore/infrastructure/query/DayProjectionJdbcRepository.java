package com.bsjhx.dayproductivityscore.infrastructure.query;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DayProjectionJdbcRepository extends CrudRepository<DayProjection, UUID> {

    @Query("SELECT * FROM day_projection WHERE user_id = :userId AND date >= :fromDate AND date <= :toDate ORDER BY date ASC")
    List<DayProjection> findByDateRange(@Param("userId") UUID userId, @Param("fromDate") String fromDate, @Param("toDate") String toDate);

    @Query("SELECT * FROM day_projection WHERE user_id = :userId AND date >= :fromDate ORDER BY date ASC")
    List<DayProjection> findFromDate(@Param("userId") UUID userId, @Param("fromDate") String fromDate);

    @Query("SELECT * FROM day_projection WHERE user_id = :userId AND date = :date")
    Optional<DayProjection> findByUserIdAndDate(@Param("userId") UUID userId, @Param("date") LocalDate date);

}
