package com.bsjhx.dayproductivityscore.infrastructure.query;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SpringDataJdbcDayProjectionRepository extends CrudRepository<DayProjection, LocalDate> {

    @Query("SELECT * FROM day_projection WHERE id >= :fromDate AND id <= :toDate ORDER BY id ASC")
    List<DayProjection> findByDateRange(@Param("fromDate") String fromDate, @Param("toDate") String toDate);

    @Query("SELECT * FROM day_projection WHERE id >= :fromDate ORDER BY id ASC")
    List<DayProjection> findFromDate(@Param("fromDate") String fromDate);

    @Modifying
    @Query("INSERT INTO day_projection (id, score, is_locked) VALUES (:id, :score, :isLocked) ON CONFLICT (id) DO UPDATE SET score = :score")
    void upsertDayRated(@Param("id") LocalDate id, @Param("score") int score, @Param("isLocked") boolean isLocked);

    @Modifying
    @Query("UPDATE day_projection SET is_locked = true WHERE id = :id")
    void lockDay(@Param("id") LocalDate id);
}
