package com.pukaar.domain.elderly;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "elderly_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ElderlySettingsEntity {
    @Id
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "soft_hours", nullable = false)
    @Builder.Default
    private int softHours = 6;
    @Column(name = "medium_hours", nullable = false)
    @Builder.Default
    private int mediumHours = 10;
    @Column(name = "urgent_hours", nullable = false)
    @Builder.Default
    private int urgentHours = 12;
    @Column(name = "escalation_minutes", nullable = false)
    @Builder.Default
    private int escalationMinutes = 5;
    @Column(name = "inactivity_monitoring_enabled", nullable = false)
    @Builder.Default
    private boolean inactivityMonitoringEnabled = true;
    @Column(name = "ambulance_number")
    @Builder.Default
    private String ambulanceNumber = "108";
    @Column(name = "doctor_name")
    private String doctorName;
    @Column(name = "doctor_phone")
    private String doctorPhone;
    @UpdateTimestamp @Column(name = "updated_at")
    private Instant updatedAt;
}
