package com.travel.travelapp.config;

import com.travel.travelapp.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatLockCleanupConfig {

    private final BookingService bookingService;

    @Scheduled(fixedDelayString = "${app.booking.lock-cleanup-ms:60000}")
    public void cleanupExpiredLocks() {
        int releasedLocks = bookingService.cleanupExpiredLocks();
        if (releasedLocks > 0) {
            log.info("Released {} expired seat lock(s)", releasedLocks);
        }
    }
}
