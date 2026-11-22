package com.communitynexus.reservation.repo;

import com.communitynexus.reservation.entity.Reservation;
import com.communitynexus.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserIdOrderByIdDesc(Long userId);

    long countByUserIdAndStatusIn(Long userId, Collection<ReservationStatus> statuses);

    List<Reservation> findByStoreIdAndStatusInOrderByHoldExpiresAtAsc(Long storeId, Collection<ReservationStatus> statuses);

    /** For the sweeper: active holds whose time is up. */
    List<Reservation> findByStatusInAndHoldExpiresAtBefore(Collection<ReservationStatus> statuses, Instant now);
}