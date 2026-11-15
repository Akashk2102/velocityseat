package com.velocityseat.modules.event.repository;

import com.velocityseat.modules.event.model.Event;
import com.velocityseat.modules.event.model.EventCategory;
import com.velocityseat.modules.event.model.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStatus(EventStatus status);

    @Query("SELECT e FROM Event e WHERE " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:category IS NULL OR e.category = :category) AND " +
           "(:keyword IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:fromTime IS NULL OR e.startTime >= :fromTime) AND " +
           "(:toTime IS NULL OR e.startTime <= :toTime) " +
           "ORDER BY e.startTime ASC")
    List<Event> searchEvents(
            @Param("status") EventStatus status,
            @Param("category") EventCategory category,
            @Param("keyword") String keyword,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime
    );
}
