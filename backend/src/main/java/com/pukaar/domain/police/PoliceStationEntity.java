package com.pukaar.domain.police;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "police_stations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PoliceStationEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String name;
    private String address;
    @Column(name = "phone_e164")
    private String phoneE164;
    @Column(name = "phone_verified", nullable = false)
    @Builder.Default
    private boolean phoneVerified = false;
    @Column(nullable = false)
    private String source;
    @Column(nullable = false)
    private Double latitude;
    @Column(nullable = false)
    private Double longitude;
    @Builder.Default
    private boolean active = true;
}
