package com.pukaar.config;

import com.pukaar.common.UserRole;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {
    private final PukaarProperties props;
    private final UserRepository userRepo;

    @Override
    public void run(ApplicationArguments args) {
        Set<String> phones = adminPhones();
        if (phones.isEmpty()) return;
        for (String normalized : phones) {
            userRepo.findByPhoneE164(normalized).ifPresentOrElse(
                    user -> promote(user, normalized),
                    () -> log.info("Admin phone {} configured but user not registered yet — will promote on first login", normalized)
            );
        }
    }

    public void promoteIfAdminPhone(UserEntity user) {
        if (user.getRole() == UserRole.ADMIN) return;
        if (adminPhones().contains(user.getPhoneE164())) {
            promote(user, user.getPhoneE164());
        }
    }

    private Set<String> adminPhones() {
        String raw = props.getAdmin().getPhone();
        if (raw == null || raw.isBlank()) return Set.of();
        return Arrays.stream(raw.split("[,;\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::normalize)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void promote(UserEntity user, String phone) {
        if (user.getRole() == UserRole.ADMIN) return;
        user.setRole(UserRole.ADMIN);
        userRepo.save(user);
        log.info("Promoted {} to ADMIN", phone);
    }

    private String normalize(String phone) {
        String p = phone.trim().replace(" ", "").replace("-", "");
        if (p.startsWith("00")) p = "+" + p.substring(2);
        if (!p.startsWith("+")) {
            if (p.length() == 10) p = "+91" + p;
            else p = "+" + p;
        }
        return p;
    }
}
