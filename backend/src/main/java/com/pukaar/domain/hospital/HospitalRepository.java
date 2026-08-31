package com.pukaar.domain.hospital;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface HospitalRepository extends JpaRepository<HospitalEntity, UUID> {
    @Query(value = """
        SELECT * FROM hospitals
        WHERE active = TRUE
        ORDER BY (POWER(latitude - :lat, 2) + POWER(longitude - :lng, 2))
        LIMIT :limit
        """, nativeQuery = true)
    List<HospitalEntity> findNearest(@Param("lat") double lat, @Param("lng") double lng, @Param("limit") int limit);
}
