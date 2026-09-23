package com.pukaar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "pukaar")
public class PukaarProperties {
    private Jwt jwt = new Jwt();
    private Otp otp = new Otp();
    private Subscription subscription = new Subscription();
    private Emergency emergency = new Emergency();
    private Elderly elderly = new Elderly();
    private Storage storage = new Storage();
    private Notification notification = new Notification();
    private Razorpay razorpay = new Razorpay();
    private Admin admin = new Admin();
    private Alerts alerts = new Alerts();
    private Google google = new Google();
    private DefaultTrustedContact defaultTrustedContact = new DefaultTrustedContact();
    /** Public site origin for View More links (e.g. https://pukaar.app or http://host/pukaar). */
    private String publicBaseUrl = "https://pukaaralert.com/pukaar";

    @Data
    public static class Google {
        private String mapsApiKey = "";
    }

    @Data
    public static class DefaultTrustedContact {
        private boolean enabled = false;
        private String phone = "";
        private String name = "PUKAAR Support";
        private String relationship = "Support";
    }

    @Data
    public static class Jwt {
        private String secret;
        private long accessTokenMinutes = 60;
        private long refreshTokenDays = 30;
    }

    @Data
    public static class Otp {
        private int length = 6;
        private int ttlSeconds = 300;
        private boolean mockEnabled = false;
        private String mockCode = "123456";
    }

    @Data
    public static class Subscription {
        private int individualPriceInr = 499;
        private int familyPriceInr = 699;
        private int familyMemberLimit = 5;
        private int referralFamilyPriceInr = 499;
        private int referralsRequired = 3;
        private int graceDays = 7;
    }

    @Data
    public static class Emergency {
        private int audioSegmentSeconds = 60;
        private int sessionTimeoutHours = 4;
        private int evidenceRetentionDays = 90;
    }

    @Data
    public static class Elderly {
        private int softInactivityHours = 6;
        private int mediumInactivityHours = 10;
        private int urgentInactivityHours = 12;
        private int acknowledgmentTimeoutMinutes = 5;
    }

    @Data
    public static class Storage {
        private String type = "local";
        private String localPath = "./data/evidence";
        private String s3Bucket;
        private String s3Region = "ap-south-1";
    }

    @Data
    public static class Notification {
        private int retryMax = 5;
        private long retryBackoffMs = 2000;
        private boolean smsFallbackEnabled = false;
        private boolean voiceEscalationEnabled = false;
        /** Seconds after SOS before placing AuthKey voice call (default 25). */
        private int voiceEscalateDelaySeconds = 25;
    }

    @Data
    public static class Razorpay {
        private String keyId = "";
        private String keySecret = "";
        private String webhookSecret = "";
    }

    @Data
    public static class Admin {
        /** Comma-separated E.164 or 10-digit Indian numbers promoted to ADMIN on login. */
        private String phone = "";
    }

    @Data
    public static class Alerts {
        private Whatsapp whatsapp = new Whatsapp();
        private Sms sms = new Sms();
        private Voice voice = new Voice();
        private Fcm fcm = new Fcm();

        @Data
        public static class Whatsapp {
            private String token = "";
            private String phoneNumberId = "";
            private String templateName = "emergency";
            private String templateLanguage = "en";
            /** Meta template for I'm Safe closure (e.g. pukaar_safe). Falls back to text if blank. */
            private String safeTemplateName = "pukaar_safe";
            private String safeTemplateLanguage = "en";
            /** Optional Meta template for inactivity welfare alerts. Blank → free-form text. */
            private String inactivityTemplateName = "";
            private String inactivityTemplateLanguage = "en";
            private String webhookVerifyToken = "";
            /** Send live location updates to alerted contacts during active SOS. */
            private boolean locationUpdatesEnabled = true;
            /** Minimum seconds between location WhatsApp updates per event (testing default: 120). */
            private int locationUpdateIntervalSeconds = 120;
            /** Stop live location pings this many hours after SOS start (even if still open). */
            private int locationUpdateMaxHours = 3;
        }

        @Data
        public static class Sms {
            private String endpoint = "https://control.yourbulksms.net/api/sendhttp.php";
            private String authKey = "";
            private String senderId = "AXPONT";
            private String route = "2";
            private String country = "0";
            private String dltTeId = "";
            private String otpTemplate = "Dear user , Your OTP is {#var#}. Use this to verify your Axispoint account within 10 minutes. For your security, do not share this code with anyone.";
        }

        @Data
        public static class Voice {
            private String endpoint = "https://api.authkey.io/request";
            private String authKey = "";
            private String countryCode = "91";
            /** Optional AuthKey template id (vid). When set, raw voice text is not sent. */
            private String templateId = "";
            private String messageTemplate =
                    "PUKAAR alert. {userName} needs help. Check WhatsApp now for location and details.";
            /** Outbound caller ID number shown on contact phones (for contact-book display name). */
            private String displayNumber = "+918037126014";
            private boolean enabled = false;
        }

        @Data
        public static class Fcm {
            private String serverKey = "";
            /** Path to Firebase Admin SDK JSON for HTTP v1 push. */
            private String serviceAccountPath = "";
            private String projectId = "sos-pukar-c74d9";
        }
    }
}
