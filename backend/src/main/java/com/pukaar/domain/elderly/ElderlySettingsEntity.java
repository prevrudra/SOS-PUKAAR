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
    private int softHours = 12;
    @Column(name = "medium_hours", nullable = false)
    @Builder.Default
    private int mediumHours = 12;
    @Column(name = "urgent_hours", nullable = false)
    @Builder.Default
    private int urgentHours = 12;
    /** Single inactivity threshold (hours). Spec: 12 / 18 / 24 / 30 / 36. */
    @Column(name = "duration_hours", nullable = false)
    @Builder.Default
    private int durationHours = 12;
    /**
     * Exact threshold in minutes. When &gt; 0 (admin/test), this overrides duration_hours.
     * App onboarding keeps this at 0 and uses duration_hours only.
     */
    @Column(name = "duration_minutes", nullable = false)
    @Builder.Default
    private int durationMinutes = 0;
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
    @Column(name = "blood_group", length = 10)
    private String bloodGroup;
    @Column(columnDefinition = "TEXT")
    private String allergies;
    @Column(name = "medical_conditions", columnDefinition = "TEXT")
    private String medicalConditions;
    @Column(columnDefinition = "TEXT")
    private String medications;
    @Column(name = "medication_reminder_enabled", nullable = false)
    @Builder.Default
    private boolean medicationReminderEnabled = true;
    @UpdateTimestamp @Column(name = "updated_at")
    private Instant updatedAt;
}
