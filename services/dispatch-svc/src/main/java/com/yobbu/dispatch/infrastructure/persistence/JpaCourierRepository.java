package com.yobbu.dispatch.infrastructure.persistence;

import com.yobbu.dispatch.domain.model.Courier;
import com.yobbu.dispatch.domain.model.CourierStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaCourierRepository extends JpaRepository<Courier, UUID> {

    List<Courier> findByStatus(CourierStatus status);

    /**
     * Returns couriers available within radiusMeters of (lng, lat), ordered by distance.
     * Uses PostGIS ST_DWithin on EPSG:4326 geography.
     */
    @Query(value = """
            SELECT * FROM dispatch.couriers
            WHERE status = 'AVAILABLE'
              AND current_position IS NOT NULL
              AND ST_DWithin(
                    current_position::geography,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                    :radiusMeters
                  )
            ORDER BY ST_Distance(
                        current_position::geography,
                        ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
                     )
            LIMIT :limit
            """, nativeQuery = true)
    List<Courier> findAvailableNear(@Param("lng") double lng,
                                    @Param("lat") double lat,
                                    @Param("radiusMeters") double radiusMeters,
                                    @Param("limit") int limit);
}
