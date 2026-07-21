package com.bsjhx.dayproductivityscore.infrastructure.query;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DayProjectionJdbcRepository extends CrudRepository<DayProjection, LocalDate> {

    @Query("SELECT * FROM day_projection WHERE id >= :fromDate AND id <= :toDate ORDER BY id ASC")
    List<DayProjection> findByDateRange(@Param("fromDate") String fromDate, @Param("toDate") String toDate);

    @Query("SELECT * FROM day_projection WHERE id >= :fromDate ORDER BY id ASC")
    List<DayProjection> findFromDate(@Param("fromDate") String fromDate);

}
