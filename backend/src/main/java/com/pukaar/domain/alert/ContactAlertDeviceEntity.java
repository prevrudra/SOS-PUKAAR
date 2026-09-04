package com.pukaar.domain.alert;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contact_alert_devices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContactAlertDeviceEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "phone_e164", nullable = false, length = 20)
    private String phoneE164;
    @Column(name = "fcm_token", length = 512)
    private String fcmToken;
    @Column(name = "device_id", length = 128)
    private String deviceId;
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String platform = "ANDROID";
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at")
    private Instant updatedAt;
}
