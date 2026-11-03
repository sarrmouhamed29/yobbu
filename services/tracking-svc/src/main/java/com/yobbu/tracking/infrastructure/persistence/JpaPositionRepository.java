package com.yobbu.tracking.infrastructure.persistence;

import com.yobbu.tracking.domain.model.CourierPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaPositionRepository extends JpaRepository<CourierPosition, Long> {

    @Query(value = """
            SELECT * FROM tracking.positions
            WHERE courier_id = :courierId
            ORDER BY recorded_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<CourierPosition> findLatestByCourierId(@Param("courierId") UUID courierId);
}
