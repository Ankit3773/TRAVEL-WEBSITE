package com.travel.travelapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.travelapp.entity.BookingStatus;
import com.travel.travelapp.entity.Bus;
import com.travel.travelapp.entity.BusType;
import com.travel.travelapp.entity.PaymentGateway;
import com.travel.travelapp.entity.PaymentMode;
import com.travel.travelapp.entity.PaymentStatus;
import com.travel.travelapp.entity.Route;
import com.travel.travelapp.entity.Seat;
import com.travel.travelapp.entity.TripSchedule;
import com.travel.travelapp.entity.UserRole;
import com.travel.travelapp.repository.BookingRepository;
import com.travel.travelapp.repository.BookingSeatRepository;
import com.travel.travelapp.repository.BusRepository;
import com.travel.travelapp.repository.RouteRepository;
import com.travel.travelapp.repository.RouteReviewRepository;
import com.travel.travelapp.repository.SeatRepository;
import com.travel.travelapp.repository.TripScheduleRepository;
import com.travel.travelapp.repository.UserRepository;
import com.travel.travelapp.util.FarePolicy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BookingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private TripScheduleRepository tripScheduleRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingSeatRepository bookingSeatRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private RouteReviewRepository routeReviewRepository;

    @Autowired
    private UserRepository userRepository;

    private Long tripScheduleId;

    @BeforeEach
    void setUp() {
        routeReviewRepository.deleteAll();
        bookingSeatRepository.deleteAll();
        bookingRepository.deleteAll();
        tripScheduleRepository.deleteAll();
        seatRepository.deleteAll();
        busRepository.deleteAll();
        routeRepository.deleteAll();

        Route route = new Route();
        route.setSource("Patna");
        route.setDestination("Gaya");
        route.setDistanceKm(110);
        route.setActive(true);
        route = routeRepository.save(route);

        Bus bus = new Bus();
        bus.setBusNumber("BR01-NT-777");
        bus.setBusType(BusType.NON_AC);
        bus.setTotalSeats(40);
        bus.setActive(true);
        bus = busRepository.save(bus);

        for (int seatNumber = 1; seatNumber <= bus.getTotalSeats(); seatNumber++) {
            Seat seat = new Seat();
            seat.setBus(bus);
            seat.setSeatNumber(seatNumber);
            seat.setActive(true);
            seatRepository.save(seat);
        }

        TripSchedule schedule = new TripSchedule();
        schedule.setRoute(route);
        schedule.setBus(bus);
        schedule.setTravelDate(LocalDate.now().plusDays(1));
        schedule.setDepartureTime(LocalTime.of(8, 0));
        schedule.setArrivalTime(LocalTime.of(11, 0));
        schedule.setBoardingPoint("Patna Gandhi Maidan Gate 2");
        schedule.setBoardingNotes("Reach 20 minutes before departure near the main gate.");
        schedule.setDroppingPoint("Gaya Bus Stand Platform 1");
        schedule.setDroppingNotes("Crew confirms the exact drop lane shortly before arrival.");
        schedule.setBaseFare(new BigDecimal("200.00"));
        schedule.setActive(true);
        schedule = tripScheduleRepository.save(schedule);
        tripScheduleId = schedule.getId();
    }

    @Test
    void bookingWithoutAuthShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest(1))))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiDocsShouldBePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Narayan Travels API"));
    }

    @Test
    void healthEndpointShouldBePublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void tourismRoutesEndpointShouldBePublic() throws Exception {
        mockMvc.perform(get("/api/routes/tourism"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void requestIdHeaderShouldBeEchoedWhenProvided() throws Exception {
        mockMvc.perform(get("/api/routes").header("X-Request-Id", "trace-test-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "trace-test-1"));
    }

    @Test
    void duplicateSeatBookingShouldReturnConflict() throws Exception {
        String customerToken = registerCustomerAndGetToken("ankit@example.com");

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest(5))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seatNumber").value(5))
                .andExpect(jsonPath("$.bookingStatus").value(BookingStatus.BOOKED.name()))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.PENDING.name()));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest(5))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Seat already booked for this schedule"));
    }

    @Test
    void multiSeatBookingShouldBookAllSelectedSeats() throws Exception {
        String customerToken = registerCustomerAndGetToken("multiseat-" + UUID.randomUUID() + "@example.com");

        Map<String, Object> payload = new HashMap<>();
        payload.put("tripScheduleId", tripScheduleId);
        payload.put("seatNumbers", List.of(2, 3, 4));
        payload.put("passengerName", "Ankit Kumar");
        payload.put("passengerPhone", "9876543210");
        payload.put("paymentMode", PaymentMode.PAY_ON_BOARD.name());

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingStatus").value(BookingStatus.BOOKED.name()))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.PENDING.name()))
                .andExpect(jsonPath("$.seatNumbers[0]").value(2))
                .andExpect(jsonPath("$.seatNumbers[1]").value(3))
                .andExpect(jsonPath("$.seatNumbers[2]").value(4))
                .andExpect(jsonPath("$.amount").value(600.0));

        mockMvc.perform(get("/api/schedules/{scheduleId}/seats", tripScheduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookedSeats[0]").value(2))
                .andExpect(jsonPath("$.bookedSeats[1]").value(3))
                .andExpect(jsonPath("$.bookedSeats[2]").value(4));
    }

    @Test
    void legacyConfirmedStatusFilterShouldReturnBookedRecords() throws Exception {
        String customerToken = registerCustomerAndGetToken("legacy-filter-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest(15))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingStatus").value(BookingStatus.BOOKED.name()));

        mockMvc.perform(get("/api/my-bookings/paged")
                        .param("status", BookingStatus.CONFIRMED.name())
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].bookingStatus").value(BookingStatus.BOOKED.name()));
    }

    @Test
    void onlineDirectBookingShouldRequireCheckoutFlow() throws Exception {
        String customerToken = registerCustomerAndGetToken("online-direct-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest(16, PaymentMode.ONLINE))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ONLINE payments must use seat lock and payment checkout"));
    }

    @Test
    void onlinePaymentFlowShouldMarkBookingPaidAfterVerification() throws Exception {
        String customerToken = registerCustomerAndGetToken("online-flow-" + UUID.randomUUID() + "@example.com");

        String lockResponse = mockMvc.perform(post("/api/bookings/locks")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest(18, PaymentMode.ONLINE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingStatus").value(BookingStatus.LOCKED.name()))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.PENDING.name()))
                .andExpect(jsonPath("$.paymentGateway").value(PaymentGateway.MOCK.name()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long bookingId = objectMapper.readTree(lockResponse).get("bookingId").asLong();

        String checkoutResponse = mockMvc.perform(post("/api/bookings/locks/{bookingId}/payments/checkout", bookingId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.PENDING.name()))
                .andExpect(jsonPath("$.paymentGateway").value(PaymentGateway.MOCK.name()))
                .andExpect(jsonPath("$.paymentSessionId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String paymentSessionId = objectMapper.readTree(checkoutResponse).get("paymentSessionId").asText();

        Map<String, Object> verifyPayload = new HashMap<>();
        verifyPayload.put("paymentSessionId", paymentSessionId);
        verifyPayload.put("paymentId", "mock-payment-123");

        mockMvc.perform(post("/api/bookings/locks/{bookingId}/payments/verify", bookingId)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingStatus").value(BookingStatus.BOOKED.name()))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.PAID.name()))
                .andExpect(jsonPath("$.paymentGateway").value(PaymentGateway.MOCK.name()))
                .andExpect(jsonPath("$.paymentReference").value("mock-payment-123"));

        mockMvc.perform(get("/api/schedules/{scheduleId}/seats", tripScheduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookedSeats[0]").value(18));
    }

    @Test
    void seatLockShouldBlockOtherUsersUntilConfirmed() throws Exception {
        String firstUserToken = registerCustomerAndGetToken("lock-a-" + UUID.randomUUID() + "@example.com");
        String secondUserToken = registerCustomerAndGetToken("lock-b-" + UUID.randomUUID() + "@example.com");

        String lockResponse = mockMvc.perform(post("/api/bookings/locks")
                        .header("Authorization", "Bearer " + firstUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest(12))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingStatus").value(BookingStatus.LOCKED.name()))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.PENDING.name()))
                .andExpect(jsonPath("$.seatNumber").value(12))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long lockBookingId = objectMapper.readTree(lockResponse).get("bookingId").asLong();

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest(12))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Seat already booked for this schedule"));

        mockMvc.perform(post("/api/bookings/locks/{bookingId}/confirm", lockBookingId)
                        .header("Authorization", "Bearer " + firstUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingStatus").value(BookingStatus.BOOKED.name()))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.PENDING.name()))
                .andExpect(jsonPath("$.seatNumber").value(12));
    }

    @Test
    void customerBookingFlowShouldSearchBookFetchAndListHistory() throws Exception {
        String customerToken = registerCustomerAndGetToken("booking-flow-" + UUID.randomUUID() + "@example.com");
        String travelDate = LocalDate.now().plusDays(1).toString();

        String schedulesResponse = mockMvc.perform(get("/api/schedules")
                        .param("source", "Patna")
                        .param("destination", "Gaya")
                        .param("date", travelDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(tripScheduleId))
                .andExpect(jsonPath("$[0].route.source").value("Patna"))
                .andExpect(jsonPath("$[0].route.destination").value("Gaya"))
                .andExpect(jsonPath("$[0].boardingPoint").value("Patna Gandhi Maidan Gate 2"))
                .andExpect(jsonPath("$[0].droppingPoint").value("Gaya Bus Stand Platform 1"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode schedules = objectMapper.readTree(schedulesResponse);
        assertThat(schedules.size()).isEqualTo(1);

        mockMvc.perform(get("/api/schedules/{scheduleId}/seats", tripScheduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSeats").value(40))
                .andExpect(jsonPath("$.availableSeats[0]").value(1));

        String bookingResponse = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest(6))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tripScheduleId").value(tripScheduleId))
                .andExpect(jsonPath("$.bookingStatus").value(BookingStatus.BOOKED.name()))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.PENDING.name()))
                .andExpect(jsonPath("$.seatNumber").value(6))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long bookingId = objectMapper.readTree(bookingResponse).get("bookingId").asLong();

        mockMvc.perform(get("/api/bookings/{bookingId}", bookingId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.seatNumbers[0]").value(6));

        mockMvc.perform(get("/api/my-bookings")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(bookingId))
                .andExpect(jsonPath("$[0].bookingStatus").value(BookingStatus.BOOKED.name()))
                .andExpect(jsonPath("$[0].seatNumbers[0]").value(6));
    }

    @Test
    void customerCanReviewCompletedTripAndPublicReviewEndpointsExposeIt() throws Exception {
        TripSchedule completedSchedule = tripScheduleRepository.findById(tripScheduleId).orElseThrow();
        completedSchedule.setTravelDate(LocalDate.now().minusDays(1));
        tripScheduleRepository.save(completedSchedule);

        String customerToken = registerCustomerAndGetToken("review-flow-" + UUID.randomUUID() + "@example.com");
        String bookingResponse = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest(8))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long bookingId = objectMapper.readTree(bookingResponse).get("bookingId").asLong();

        Map<String, Object> reviewPayload = new HashMap<>();
        reviewPayload.put("bookingId", bookingId);
        reviewPayload.put("rating", 5);
        reviewPayload.put("title", "Smooth and reliable");
        reviewPayload.put("comment", "Boarding was organized and the route timing felt dependable throughout the trip.");

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.routeId").exists())
                .andExpect(jsonPath("$.rating").value(5));

        mockMvc.perform(get("/api/reviews/highlights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(bookingId))
                .andExpect(jsonPath("$[0].comment").value("Boarding was organized and the route timing felt dependable throughout the trip."));

        mockMvc.perform(get("/api/reviews/route/{routeId}", completedSchedule.getRoute().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(completedSchedule.getRoute().getId()))
                .andExpect(jsonPath("$.totalReviews").value(1))
                .andExpect(jsonPath("$.averageRating").value(5.0))
                .andExpect(jsonPath("$.reviews[0].bookingId").value(bookingId));

        mockMvc.perform(get("/api/my-bookings")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].routeId").value(completedSchedule.getRoute().getId()))
                .andExpect(jsonPath("$[0].reviewId").exists())
                .andExpect(jsonPath("$[0].reviewRating").value(5))
                .andExpect(jsonPath("$[0].reviewEligible").value(true));
    }

    @Test
    void releasingSeatLockShouldMakeSeatAvailableToAnotherCustomer() throws Exception {
        String firstUserToken = registerCustomerAndGetToken("release-lock-a-" + UUID.randomUUID() + "@example.com");
        String secondUserToken = registerCustomerAndGetToken("release-lock-b-" + UUID.randomUUID() + "@example.com");

        String lockResponse = mockMvc.perform(post("/api/bookings/locks")
                        .header("Authorization", "Bearer " + firstUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest(13))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingStatus").value(BookingStatus.LOCKED.name()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long bookingId = objectMapper.readTree(lockResponse).get("bookingId").asLong();

        String lockedSeatResponse = mockMvc.perform(get("/api/schedules/{scheduleId}/seats", tripScheduleId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode lockedSeatJson = objectMapper.readTree(lockedSeatResponse);
        assertThat(intList(lockedSeatJson, "lockedSeats")).contains(13);

        mockMvc.perform(delete("/api/bookings/locks/{bookingId}", bookingId)
                        .header("Authorization", "Bearer " + firstUserToken))
                .andExpect(status().isNoContent());

        String releasedSeatResponse = mockMvc.perform(get("/api/schedules/{scheduleId}/seats", tripScheduleId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode releasedSeatJson = objectMapper.readTree(releasedSeatResponse);
        assertThat(intList(releasedSeatJson, "lockedSeats")).isEmpty();
        assertThat(intList(releasedSeatJson, "availableSeats")).contains(13);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest(13))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingStatus").value(BookingStatus.BOOKED.name()))
                .andExpect(jsonPath("$.seatNumber").value(13));
    }

    @Test
    void cancellingBookingShouldFreeSeatAndAppearInCancelledHistory() throws Exception {
        String customerToken = registerCustomerAndGetToken("cancel-flow-" + UUID.randomUUID() + "@example.com");

        String bookingResponse = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest(14))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long bookingId = objectMapper.readTree(bookingResponse).get("bookingId").asLong();

        mockMvc.perform(delete("/api/bookings/{bookingId}", bookingId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/my-bookings")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(bookingId))
                .andExpect(jsonPath("$[0].bookingStatus").value(BookingStatus.CANCELLED.name()));

        mockMvc.perform(get("/api/my-bookings/paged")
                        .param("status", BookingStatus.CANCELLED.name())
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].bookingId").value(bookingId))
                .andExpect(jsonPath("$.items[0].bookingStatus").value(BookingStatus.CANCELLED.name()));

        String seatResponse = mockMvc.perform(get("/api/schedules/{scheduleId}/seats", tripScheduleId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode seatJson = objectMapper.readTree(seatResponse);
        assertThat(intList(seatJson, "bookedSeats")).doesNotContain(14);
        assertThat(intList(seatJson, "availableSeats")).contains(14);
    }

    @Test
    void adminPagedBookingsAndMetricsShouldIncludeCancelledRecords() throws Exception {
        String customerToken = registerCustomerAndGetToken("admin-cancel-view-" + UUID.randomUUID() + "@example.com");

        String bookingResponse = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest(17))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long bookingId = objectMapper.readTree(bookingResponse).get("bookingId").asLong();

        mockMvc.perform(delete("/api/bookings/{bookingId}", bookingId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNoContent());

        String adminToken = loginAndGetToken("admin@narayantravels.in", "Admin123!");

        mockMvc.perform(get("/api/admin/bookings/paged")
                        .param("status", BookingStatus.CANCELLED.name())
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].bookingId").value(bookingId))
                .andExpect(jsonPath("$.items[0].bookingStatus").value(BookingStatus.CANCELLED.name()));

        mockMvc.perform(get("/api/admin/metrics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBookings").value(1))
                .andExpect(jsonPath("$.confirmedBookings").value(0))
                .andExpect(jsonPath("$.cancelledBookings").value(1));
    }

    @Test
    void adminBookingsEndpointShouldRequireAdminRole() throws Exception {
        String customerToken = registerCustomerAndGetToken("customer2@example.com");

        mockMvc.perform(get("/api/admin/bookings").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        String adminToken = loginAndGetToken("admin@narayantravels.in", "Admin123!");
        String response = mockMvc.perform(get("/api/admin/bookings").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        assertThat(jsonNode.isArray()).isTrue();
    }

    @Test
    void adminMetricsTrendEndpointShouldRequireAdminAndReturnDailyData() throws Exception {
        String customerToken = registerCustomerAndGetToken("trend-customer-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest(7))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/metrics/trends")
                        .param("fromDate", LocalDate.now().toString())
                        .param("toDate", LocalDate.now().toString())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        String adminToken = loginAndGetToken("admin@narayantravels.in", "Admin123!");
        mockMvc.perform(get("/api/admin/metrics/trends")
                        .param("fromDate", LocalDate.now().toString())
                        .param("toDate", LocalDate.now().toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromDate").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.toDate").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.totalConfirmedBookings").value(1))
                .andExpect(jsonPath("$.items[0].date").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.items[0].confirmedBookings").value(1));
    }

    @Test
    void concurrentSameSeatBookingShouldAllowOnlyOneConfirmation() throws Exception {
        String tokenOne = registerCustomerAndGetToken("race-1-" + UUID.randomUUID() + "@example.com");
        String tokenTwo = registerCustomerAndGetToken("race-2-" + UUID.randomUUID() + "@example.com");

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<Integer> attemptOne = () -> performConcurrentBooking(tokenOne, 9, readyLatch, startLatch);
        Callable<Integer> attemptTwo = () -> performConcurrentBooking(tokenTwo, 9, readyLatch, startLatch);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> futureOne = executorService.submit(attemptOne);
            Future<Integer> futureTwo = executorService.submit(attemptTwo);

            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();

            List<Integer> statuses = new ArrayList<>();
            statuses.add(futureOne.get(10, TimeUnit.SECONDS));
            statuses.add(futureTwo.get(10, TimeUnit.SECONDS));

            long createdCount = statuses.stream().filter(code -> code == CREATED.value()).count();
            long conflictCount = statuses.stream().filter(code -> code == CONFLICT.value()).count();

            assertThat(createdCount).isEqualTo(1);
            assertThat(conflictCount).isEqualTo(1);
            assertThat(bookingRepository.findAll()).hasSize(1);
            Long bookingId = bookingRepository.findAll().get(0).getId();
            var booking = bookingRepository.findDetailedById(bookingId).orElseThrow();
            assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.BOOKED);
            assertThat(booking.getBookingSeats())
                    .extracting(bookingSeat -> bookingSeat.getSeat().getSeatNumber())
                    .containsExactly(9);
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void forgotPasswordFlowShouldResetPasswordAndAllowLogin() throws Exception {
        String email = "forgot-" + UUID.randomUUID() + "@example.com";
        registerCustomerAndGetToken(email);

        Map<String, Object> forgotPayload = new HashMap<>();
        forgotPayload.put("email", email);

        String forgotResponse = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String resetToken = objectMapper.readTree(forgotResponse).get("resetToken").asText();
        assertThat(resetToken).isNotBlank();

        Map<String, Object> resetPayload = new HashMap<>();
        resetPayload.put("token", resetToken);
        resetPayload.put("newPassword", "UpdatedSecret123");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successful"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload(email, "Secret123"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload(email, "UpdatedSecret123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void firstAdminSetupShouldWorkOnlyWhenNoAdminExists() throws Exception {
        userRepository.deleteAll(userRepository.findAll().stream()
                .filter(user -> user.getRole() == UserRole.ADMIN)
                .toList());

        mockMvc.perform(get("/api/auth/admin-setup-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminExists").value(false))
                .andExpect(jsonPath("$.setupAllowed").value(true));

        Map<String, Object> setupPayload = new HashMap<>();
        setupPayload.put("name", "Owner");
        setupPayload.put("email", "owner@example.com");
        setupPayload.put("password", "Owner123!");

        mockMvc.perform(post("/api/auth/setup-first-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setupPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.email").value("owner@example.com"));

        mockMvc.perform(get("/api/auth/admin-setup-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminExists").value(true))
                .andExpect(jsonPath("$.setupAllowed").value(false));

        mockMvc.perform(post("/api/auth/setup-first-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setupPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Admin account already exists"));
    }

    @Test
    void adminCrudEndpointsShouldSupportUpdateDeleteAndSeatAutoGeneration() throws Exception {
        String adminToken = loginAndGetToken("admin@narayantravels.in", "Admin123!");

        Map<String, Object> routePayload = new HashMap<>();
        routePayload.put("source", "Patna");
        routePayload.put("destination", "Muzaffarpur");
        routePayload.put("distanceKm", 80);
        routePayload.put("description", "A busy Patna to Muzaffarpur corridor for quick intercity travel.");
        routePayload.put("travelHighlights", "Distance: 80 km\nReliable daily corridor\nUseful for same-day travel");
        routePayload.put("travelTips", "Reach early for boarding\nCompare morning departures first");

        String routeResponse = mockMvc.perform(post("/api/admin/routes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(routePayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.destination").value("Muzaffarpur"))
                .andExpect(jsonPath("$.description").value("A busy Patna to Muzaffarpur corridor for quick intercity travel."))
                .andExpect(jsonPath("$.travelHighlights").value("Distance: 80 km\nReliable daily corridor\nUseful for same-day travel"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long routeId = objectMapper.readTree(routeResponse).get("id").asLong();

        Map<String, Object> updateRoutePayload = new HashMap<>();
        updateRoutePayload.put("source", "Patna");
        updateRoutePayload.put("destination", "Muzaffarpur");
        updateRoutePayload.put("distanceKm", 85);
        updateRoutePayload.put("tourismRoute", true);
        updateRoutePayload.put("description", "A stronger premium route summary for Muzaffarpur travellers.");
        updateRoutePayload.put("travelHighlights", "Distance: 85 km\nTourism-ready positioning\nFlexible day planning");
        updateRoutePayload.put("travelTips", "Prefer AC options\nBoard 20 minutes before departure");

        mockMvc.perform(put("/api/admin/routes/{routeId}", routeId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRoutePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceKm").value(85))
                .andExpect(jsonPath("$.tourismRoute").value(true))
                .andExpect(jsonPath("$.description").value("A stronger premium route summary for Muzaffarpur travellers."));

        mockMvc.perform(get("/api/admin/routes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        Map<String, Object> busPayload = new HashMap<>();
        busPayload.put("busNumber", "BR01-NT-901");
        busPayload.put("busType", "AC");
        busPayload.put("totalSeats", 6);

        String busResponse = mockMvc.perform(post("/api/admin/buses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(busPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.busNumber").value("BR01-NT-901"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long busId = objectMapper.readTree(busResponse).get("id").asLong();

        assertThat(seatRepository.countByBusIdAndActiveTrue(busId)).isEqualTo(6);

        Map<String, Object> updateBusPayload = new HashMap<>();
        updateBusPayload.put("busNumber", "BR01-NT-901");
        updateBusPayload.put("busType", "NON_AC");
        updateBusPayload.put("totalSeats", 8);

        mockMvc.perform(put("/api/admin/buses/{busId}", busId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBusPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.busType").value("NON_AC"))
                .andExpect(jsonPath("$.totalSeats").value(8));

        assertThat(seatRepository.countByBusIdAndActiveTrue(busId)).isEqualTo(8);

        mockMvc.perform(post("/api/admin/seats/generate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        Map<String, Object> schedulePayload = new HashMap<>();
        schedulePayload.put("routeId", routeId);
        schedulePayload.put("busId", busId);
        schedulePayload.put("travelDate", LocalDate.now().plusDays(3).toString());
        schedulePayload.put("departureTime", "09:00:00");
        schedulePayload.put("arrivalTime", "11:00:00");
        schedulePayload.put("boardingPoint", "Patna Mithapur Stand Bay 4");
        schedulePayload.put("boardingNotes", "Reach by 08:40 for platform confirmation.");
        schedulePayload.put("droppingPoint", "Muzaffarpur Imlichatti Drop Zone");
        schedulePayload.put("droppingNotes", "Final drop lane is shared by the driver before arrival.");
        schedulePayload.put("baseFare", new BigDecimal("260.00"));

        String scheduleResponse = mockMvc.perform(post("/api/admin/schedules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(schedulePayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.baseFare").value(260.0))
                .andExpect(jsonPath("$.boardingPoint").value("Patna Mithapur Stand Bay 4"))
                .andExpect(jsonPath("$.droppingPoint").value("Muzaffarpur Imlichatti Drop Zone"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long scheduleId = objectMapper.readTree(scheduleResponse).get("id").asLong();

        Map<String, Object> updateSchedulePayload = new HashMap<>();
        updateSchedulePayload.put("routeId", routeId);
        updateSchedulePayload.put("busId", busId);
        updateSchedulePayload.put("travelDate", LocalDate.now().plusDays(4).toString());
        updateSchedulePayload.put("departureTime", "10:00:00");
        updateSchedulePayload.put("arrivalTime", "12:30:00");
        updateSchedulePayload.put("boardingPoint", "Patna ISBT Gate 1");
        updateSchedulePayload.put("boardingNotes", "Reach by 09:40 for baggage tagging.");
        updateSchedulePayload.put("droppingPoint", "Muzaffarpur Zero Mile Stop");
        updateSchedulePayload.put("droppingNotes", "Drop side depends on traffic control instructions.");
        updateSchedulePayload.put("baseFare", new BigDecimal("280.00"));

        mockMvc.perform(put("/api/admin/schedules/{scheduleId}", scheduleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateSchedulePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departureTime").value("10:00:00"))
                .andExpect(jsonPath("$.arrivalTime").value("12:30:00"))
                .andExpect(jsonPath("$.boardingPoint").value("Patna ISBT Gate 1"))
                .andExpect(jsonPath("$.droppingPoint").value("Muzaffarpur Zero Mile Stop"))
                .andExpect(jsonPath("$.baseFare").value(280.0));

        mockMvc.perform(get("/api/admin/schedules")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(delete("/api/admin/schedules/{scheduleId}", scheduleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Schedule deactivated successfully"));

        mockMvc.perform(delete("/api/admin/buses/{busId}", busId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Bus deactivated successfully"));
        assertThat(seatRepository.countByBusIdAndActiveTrue(busId)).isEqualTo(0);

        mockMvc.perform(delete("/api/admin/routes/{routeId}", routeId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Route deactivated successfully"));

        Route route = routeRepository.findById(routeId).orElseThrow();
        Bus bus = busRepository.findById(busId).orElseThrow();
        TripSchedule schedule = tripScheduleRepository.findById(scheduleId).orElseThrow();
        assertThat(route.getActive()).isFalse();
        assertThat(bus.getActive()).isFalse();
        assertThat(schedule.getActive()).isFalse();
    }

    @Test
    void farePolicyShouldPriceRegularAcAndNonAcRoutesDifferently() {
        Route route = new Route();
        route.setDistanceKm(100);
        route.setTourismRoute(false);

        Bus acBus = new Bus();
        acBus.setBusType(BusType.AC);

        Bus nonAcBus = new Bus();
        nonAcBus.setBusType(BusType.NON_AC);

        assertThat(FarePolicy.fareFor(route, acBus)).isEqualByComparingTo("300.00");
        assertThat(FarePolicy.fareFor(route, nonAcBus)).isEqualByComparingTo("250.00");
    }

    private String registerCustomerAndGetToken(String email) throws Exception {
        Map<String, Object> registerPayload = new HashMap<>();
        registerPayload.put("name", "Test Customer");
        registerPayload.put("email", email);
        registerPayload.put("password", "Secret123");

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload(email, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private Map<String, Object> bookingRequest(int seatNumber) {
        return bookingRequest(seatNumber, PaymentMode.PAY_ON_BOARD);
    }

    private Map<String, Object> bookingRequest(int seatNumber, PaymentMode paymentMode) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tripScheduleId", tripScheduleId);
        payload.put("seatNumber", seatNumber);
        payload.put("passengerName", "Ankit Kumar");
        payload.put("passengerPhone", "9876543210");
        payload.put("paymentMode", paymentMode.name());
        return payload;
    }

    private Map<String, Object> loginPayload(String email, String password) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("password", password);
        return payload;
    }

    private List<Integer> intList(JsonNode jsonNode, String fieldName) {
        List<Integer> values = new ArrayList<>();
        for (JsonNode item : jsonNode.withArray(fieldName)) {
            values.add(item.asInt());
        }
        return values;
    }

    private Integer performConcurrentBooking(
            String token,
            int seatNumber,
            CountDownLatch readyLatch,
            CountDownLatch startLatch
    ) throws Exception {
        readyLatch.countDown();
        assertThat(startLatch.await(5, TimeUnit.SECONDS)).isTrue();

        return mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest(seatNumber))))
                .andReturn()
                .getResponse()
                .getStatus();
    }
}
