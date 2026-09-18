package com.pukaar.domain.elderly;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InactivityScheduler {
    private final InactivityService inactivityService;

    @Scheduled(fixedDelayString = "${pukaar.elderly.scan-ms:300000}")
    public void scan() {
        inactivityService.scanAll();
    }
}
