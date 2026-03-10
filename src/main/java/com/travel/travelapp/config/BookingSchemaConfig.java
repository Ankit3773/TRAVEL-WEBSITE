package com.travel.travelapp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BookingSchemaConfig {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    public CommandLineRunner bookingSchemaRunner() {
        return args -> {
            try {
                DataSource dataSource = jdbcTemplate.getDataSource();
                if (dataSource != null) {
                    try (var conn = dataSource.getConnection()) {
                        String product = conn.getMetaData().getDatabaseProductName();
                        if (product != null && product.toLowerCase().contains("h2")) {
                            return;
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Booking schema index update skipped: {}", ex.getMessage());
                return;
            }

            executeIfPossible("ALTER TABLE seat_bookings ADD COLUMN IF NOT EXISTS booking_status VARCHAR(20)");
            executeIfPossible("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS booking_status VARCHAR(20)");
            executeIfPossible("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20)");
            executeIfPossible("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS payment_gateway VARCHAR(20)");
            executeIfPossible("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS payment_session_id VARCHAR(100)");
            executeIfPossible("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(120)");
            executeIfPossible("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP");
            executeIfPossible("ALTER TABLE seat_bookings DROP CONSTRAINT IF EXISTS seat_bookings_booking_status_check");
            executeIfPossible("ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_booking_status_check");
            executeIfPossible("ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_payment_status_check");
            executeIfPossible("ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_payment_gateway_check");
            executeIfPossible(
                    "UPDATE seat_bookings SET booking_status = 'BOOKED' "
                            + "WHERE booking_status IS NULL OR booking_status = 'CONFIRMED'");
            executeIfPossible(
                    "UPDATE bookings SET booking_status = 'BOOKED' "
                            + "WHERE booking_status IS NULL OR booking_status = 'CONFIRMED'");
            executeIfPossible(
                    "UPDATE bookings SET payment_status = 'PENDING' "
                            + "WHERE payment_status IS NULL");
            executeIfPossible("ALTER TABLE seat_bookings ALTER COLUMN booking_status SET DEFAULT 'BOOKED'");
            executeIfPossible("ALTER TABLE seat_bookings ALTER COLUMN booking_status SET NOT NULL");
            executeIfPossible("ALTER TABLE bookings ALTER COLUMN booking_status SET DEFAULT 'BOOKED'");
            executeIfPossible("ALTER TABLE bookings ALTER COLUMN booking_status SET NOT NULL");
            executeIfPossible("ALTER TABLE bookings ALTER COLUMN payment_status SET DEFAULT 'PENDING'");
            executeIfPossible("ALTER TABLE bookings ALTER COLUMN payment_status SET NOT NULL");
            executeIfPossible(
                    "ALTER TABLE seat_bookings "
                            + "ADD CONSTRAINT seat_bookings_booking_status_check "
                            + "CHECK (booking_status IN ('LOCKED','BOOKED','CONFIRMED','CANCELLED'))");
            executeIfPossible(
                    "ALTER TABLE bookings "
                            + "ADD CONSTRAINT bookings_booking_status_check "
                            + "CHECK (booking_status IN ('LOCKED','BOOKED','CONFIRMED','CANCELLED'))");
            executeIfPossible(
                    "ALTER TABLE bookings "
                            + "ADD CONSTRAINT bookings_payment_status_check "
                            + "CHECK (payment_status IN ('PENDING','PAID'))");
            executeIfPossible(
                    "ALTER TABLE bookings "
                            + "ADD CONSTRAINT bookings_payment_gateway_check "
                            + "CHECK (payment_gateway IS NULL OR payment_gateway IN ('MOCK','RAZORPAY','STRIPE'))");
            executeIfPossible("ALTER TABLE seat_bookings DROP CONSTRAINT IF EXISTS uk_schedule_seat");
            executeIfPossible("DROP INDEX IF EXISTS uk_active_schedule_seat");
            executeIfPossible(
                    "CREATE UNIQUE INDEX IF NOT EXISTS uk_active_schedule_seat "
                            + "ON seat_bookings(trip_schedule_id, seat_number) "
                            + "WHERE booking_status IN ('BOOKED','CONFIRMED')");
            executeIfPossible("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS lock_expires_at TIMESTAMP");
            executeIfPossible(
                    "CREATE UNIQUE INDEX IF NOT EXISTS uk_bookings_payment_session "
                            + "ON bookings(payment_session_id) WHERE payment_session_id IS NOT NULL");
            executeIfPossible(
                    "CREATE INDEX IF NOT EXISTS idx_bookings_schedule_status "
                            + "ON bookings(trip_schedule_id, booking_status)");
        };
    }

    private void executeIfPossible(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ex) {
            log.warn("Booking schema statement skipped: {} ({})", sql, ex.getMessage());
        }
    }
}
