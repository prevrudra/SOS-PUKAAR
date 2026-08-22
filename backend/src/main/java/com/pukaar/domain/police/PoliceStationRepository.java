package com.pukaar.domain.police;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PoliceStationRepository extends JpaRepository<PoliceStationEntity, UUID> {
    @Query(value = """
        SELECT * FROM police_stations
        WHERE active = TRUE
        ORDER BY (POWER(latitude - :lat, 2) + POWER(longitude - :lng, 2))
        LIMIT :limit
        """, nativeQuery = true)
    List<PoliceStationEntity> findNearest(@Param("lat") double lat, @Param("lng") double lng, @Param("limit") int limit);
}
