package com.velocityseat.modules.event.repository;

import com.velocityseat.modules.event.model.EventSeat;
import com.velocityseat.modules.event.model.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventSeatRepository extends JpaRepository<EventSeat, Long> {

    List<EventSeat> findByEventId(Long eventId);

    List<EventSeat> findByEventIdAndStatus(Long eventId, SeatStatus status);

    long countByEventIdAndStatus(Long eventId, SeatStatus status);

    long countByEventId(Long eventId);

    boolean existsByEventId(Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT es FROM EventSeat es WHERE es.id = :id")
    Optional<EventSeat> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT es FROM EventSeat es WHERE es.id IN :ids")
    List<EventSeat> findAllByIdWithLock(@Param("ids") List<Long> ids);
}
