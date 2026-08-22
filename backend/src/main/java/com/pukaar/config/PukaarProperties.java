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
        private boolean mockEnabled = true;
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
    }
}
