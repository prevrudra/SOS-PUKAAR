package com.pukaar.config;

import com.pukaar.common.UserRole;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {
    private final PukaarProperties props;
    private final UserRepository userRepo;

    @Override
    public void run(ApplicationArguments args) {
        String adminPhone = props.getAdmin().getPhone();
        if (adminPhone == null || adminPhone.isBlank()) return;
        String normalized = normalize(adminPhone);
        userRepo.findByPhoneE164(normalized).ifPresentOrElse(user -> promote(user, normalized), () ->
                log.info("Admin phone {} configured but user not registered yet — will promote on first login", normalized));
    }

    public void promoteIfAdminPhone(UserEntity user) {
        String adminPhone = props.getAdmin().getPhone();
        if (adminPhone == null || adminPhone.isBlank()) return;
        if (normalize(adminPhone).equals(user.getPhoneE164()) && user.getRole() != UserRole.ADMIN) {
            promote(user, user.getPhoneE164());
        }
    }

    private void promote(UserEntity user, String phone) {
        user.setRole(UserRole.ADMIN);
        userRepo.save(user);
        log.info("Promoted {} to ADMIN", phone);
    }

    private String normalize(String phone) {
        String p = phone.trim().replace(" ", "");
        if (!p.startsWith("+")) {
            if (p.length() == 10) p = "+91" + p;
            else p = "+" + p;
        }
        return p;
    }
}
