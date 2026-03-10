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

                jdbcTemplate.execute(
                        "ALTER TABLE seat_bookings ADD COLUMN IF NOT EXISTS booking_status VARCHAR(20)");
                jdbcTemplate.execute(
                        "ALTER TABLE bookings ADD COLUMN IF NOT EXISTS booking_status VARCHAR(20)");
                jdbcTemplate.execute("ALTER TABLE seat_bookings DROP CONSTRAINT IF EXISTS seat_bookings_booking_status_check");
                jdbcTemplate.execute("ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_booking_status_check");
                jdbcTemplate.execute(
                        "UPDATE seat_bookings SET booking_status = 'BOOKED' "
                                + "WHERE booking_status IS NULL OR booking_status = 'CONFIRMED'");
                jdbcTemplate.execute(
                        "UPDATE bookings SET booking_status = 'BOOKED' "
                                + "WHERE booking_status IS NULL OR booking_status = 'CONFIRMED'");
                jdbcTemplate.execute("ALTER TABLE seat_bookings ALTER COLUMN booking_status SET DEFAULT 'BOOKED'");
                jdbcTemplate.execute("ALTER TABLE seat_bookings ALTER COLUMN booking_status SET NOT NULL");
                jdbcTemplate.execute("ALTER TABLE bookings ALTER COLUMN booking_status SET DEFAULT 'BOOKED'");
                jdbcTemplate.execute("ALTER TABLE bookings ALTER COLUMN booking_status SET NOT NULL");
                jdbcTemplate.execute(
                        "ALTER TABLE seat_bookings "
                                + "ADD CONSTRAINT seat_bookings_booking_status_check "
                                + "CHECK (booking_status IN ('LOCKED','BOOKED','CONFIRMED','CANCELLED'))");
                jdbcTemplate.execute(
                        "ALTER TABLE bookings "
                                + "ADD CONSTRAINT bookings_booking_status_check "
                                + "CHECK (booking_status IN ('LOCKED','BOOKED','CONFIRMED','CANCELLED'))");
                jdbcTemplate.execute("ALTER TABLE seat_bookings DROP CONSTRAINT IF EXISTS uk_schedule_seat");
                jdbcTemplate.execute("DROP INDEX IF EXISTS uk_active_schedule_seat");
                jdbcTemplate.execute(
                        "CREATE UNIQUE INDEX IF NOT EXISTS uk_active_schedule_seat "
                                + "ON seat_bookings(trip_schedule_id, seat_number) "
                                + "WHERE booking_status IN ('BOOKED','CONFIRMED')");
                jdbcTemplate.execute(
                        "ALTER TABLE bookings ADD COLUMN IF NOT EXISTS lock_expires_at TIMESTAMP");
                jdbcTemplate.execute(
                        "CREATE INDEX IF NOT EXISTS idx_bookings_schedule_status "
                                + "ON bookings(trip_schedule_id, booking_status)");
            } catch (Exception ex) {
                log.warn("Booking schema index update skipped: {}", ex.getMessage());
            }
        };
    }
}
