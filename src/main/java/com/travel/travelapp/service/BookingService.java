package com.travel.travelapp.service;

import com.travel.travelapp.dto.BookingHistoryResponse;
import com.travel.travelapp.dto.BookingPageResponse;
import com.travel.travelapp.dto.BookingResponse;
import com.travel.travelapp.dto.CreateBookingRequest;
import com.travel.travelapp.dto.SeatAvailabilityResponse;
import com.travel.travelapp.entity.AppUser;
import com.travel.travelapp.entity.Booking;
import com.travel.travelapp.entity.BookingSeat;
import com.travel.travelapp.entity.BookingStatus;
import com.travel.travelapp.entity.Seat;
import com.travel.travelapp.entity.TripSchedule;
import com.travel.travelapp.exception.BadRequestException;
import com.travel.travelapp.exception.ResourceNotFoundException;
import com.travel.travelapp.exception.SeatAlreadyBookedException;
import com.travel.travelapp.repository.BookingRepository;
import com.travel.travelapp.repository.BookingSeatRepository;
import com.travel.travelapp.repository.SeatRepository;
import com.travel.travelapp.repository.TripScheduleRepository;
import com.travel.travelapp.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final String SEAT_CONFLICT_MESSAGE = "Seat already booked for this schedule";
    private static final List<BookingStatus> ACTIVE_SEAT_BLOCKING_STATUSES = List.of(
            BookingStatus.BOOKED, BookingStatus.CONFIRMED, BookingStatus.LOCKED);

    private final TripScheduleRepository tripScheduleRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    @Value("${app.booking.lock-minutes:5}")
    private long seatLockMinutes;

    @Transactional
    public SeatAvailabilityResponse getSeatAvailability(Long tripScheduleId) {
        LocalDateTime now = LocalDateTime.now();
        purgeExpiredLocks(now);

        TripSchedule schedule = tripScheduleRepository.findById(tripScheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        int totalSeats = schedule.getBus().getTotalSeats();
        List<Booking> activeBookings = bookingRepository.findByTripScheduleIdAndBookingStatusIn(
                tripScheduleId, ACTIVE_SEAT_BLOCKING_STATUSES);

        Set<Integer> bookedSeats = new HashSet<>();
        Set<Integer> lockedSeats = new HashSet<>();

        for (Booking booking : activeBookings) {
            if (booking.getBookingStatus().isBookedState()) {
                bookedSeats.addAll(extractActiveSeatNumbers(booking));
                continue;
            }

            if (booking.getBookingStatus() == BookingStatus.LOCKED
                    && booking.getLockExpiresAt() != null
                    && booking.getLockExpiresAt().isAfter(now)) {
                lockedSeats.addAll(extractActiveSeatNumbers(booking));
            }
        }

        lockedSeats.removeAll(bookedSeats);

        List<Integer> availableSeats = new ArrayList<>();
        for (int seat = 1; seat <= totalSeats; seat++) {
            if (!bookedSeats.contains(seat) && !lockedSeats.contains(seat)) {
                availableSeats.add(seat);
            }
        }

        return SeatAvailabilityResponse.builder()
                .tripScheduleId(tripScheduleId)
                .totalSeats(totalSeats)
                .bookedSeats(bookedSeats.stream().sorted().toList())
                .lockedSeats(lockedSeats.stream().sorted().toList())
                .availableSeats(availableSeats)
                .build();
    }

    @Transactional
    public BookingResponse createSeatLock(CreateBookingRequest request, String userEmail) {
        LocalDateTime now = LocalDateTime.now();
        purgeExpiredLocks(now);

        TripSchedule schedule = tripScheduleRepository.findByIdForUpdate(request.getTripScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        AppUser user = requireUser(userEmail);

        List<Integer> requestedSeatNumbers = validateSeatNumbers(
                normalizeSeatNumbers(request.getSeatNumber(), request.getSeatNumbers()),
                schedule.getBus().getTotalSeats());
        Map<Integer, Seat> seatByNumber = loadScheduleSeats(schedule, requestedSeatNumbers);
        ensureSeatsAreAvailable(schedule.getId(), requestedSeatNumbers, now, null);

        Booking lockBooking = new Booking();
        lockBooking.setTripSchedule(schedule);
        lockBooking.setBookedByUser(user);
        lockBooking.setPassengerName(request.getPassengerName().trim());
        lockBooking.setPassengerPhone(request.getPassengerPhone().trim());
        lockBooking.setPaymentMode(request.getPaymentMode());
        lockBooking.setAmount(calculateAmount(schedule.getBaseFare(), requestedSeatNumbers.size()));
        lockBooking.setBookedAt(now);
        lockBooking.setBookingStatus(BookingStatus.LOCKED);
        lockBooking.setLockExpiresAt(now.plusMinutes(seatLockMinutes));
        lockBooking.setBookingSeats(buildBookingSeats(lockBooking, requestedSeatNumbers, seatByNumber));

        Booking saved = bookingRepository.save(lockBooking);
        return toBookingResponse(saved);
    }

    @Transactional
    public BookingResponse confirmSeatLock(Long lockBookingId, String userEmail, boolean isAdmin) {
        LocalDateTime now = LocalDateTime.now();
        purgeExpiredLocks(now);

        Booking booking = bookingRepository.findDetailedById(lockBookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (booking.getBookingStatus() != BookingStatus.LOCKED) {
            throw new BadRequestException("Booking is not in locked state");
        }

        if (!isAdmin && !booking.getBookedByUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("You are not allowed to confirm this booking lock");
        }

        if (booking.getLockExpiresAt() == null || !booking.getLockExpiresAt().isAfter(now)) {
            releaseLockInternal(booking);
            throw new BadRequestException("Seat lock expired. Please lock seats again");
        }

        tripScheduleRepository.findByIdForUpdate(booking.getTripSchedule().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        booking.setBookingStatus(BookingStatus.BOOKED);
        booking.setLockExpiresAt(null);
        Booking saved = bookingRepository.save(booking);
        return toBookingResponse(saved);
    }

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, String userEmail) {
        LocalDateTime now = LocalDateTime.now();
        purgeExpiredLocks(now);

        TripSchedule schedule = tripScheduleRepository.findByIdForUpdate(request.getTripScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        AppUser user = requireUser(userEmail);

        List<Integer> requestedSeatNumbers = validateSeatNumbers(
                normalizeSeatNumbers(request.getSeatNumber(), request.getSeatNumbers()),
                schedule.getBus().getTotalSeats());
        Map<Integer, Seat> seatByNumber = loadScheduleSeats(schedule, requestedSeatNumbers);
        ensureSeatsAreAvailable(schedule.getId(), requestedSeatNumbers, now, null);

        Booking booking = new Booking();
        booking.setTripSchedule(schedule);
        booking.setBookedByUser(user);
        booking.setPassengerName(request.getPassengerName().trim());
        booking.setPassengerPhone(request.getPassengerPhone().trim());
        booking.setPaymentMode(request.getPaymentMode());
        booking.setAmount(calculateAmount(schedule.getBaseFare(), requestedSeatNumbers.size()));
        booking.setBookedAt(now);
        booking.setBookingStatus(BookingStatus.BOOKED);
        booking.setLockExpiresAt(null);
        booking.setBookingSeats(buildBookingSeats(booking, requestedSeatNumbers, seatByNumber));

        Booking saved = bookingRepository.save(booking);
        return toBookingResponse(saved);
    }

    @Transactional
    public BookingResponse getBookingById(Long bookingId, String userEmail, boolean isAdmin) {
        LocalDateTime now = LocalDateTime.now();
        purgeExpiredLocks(now);

        Booking booking = bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!isAdmin && !booking.getBookedByUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("You are not allowed to view this booking");
        }
        if (booking.getBookingStatus() == BookingStatus.LOCKED
                && (booking.getLockExpiresAt() == null || !booking.getLockExpiresAt().isAfter(now))) {
            releaseLockInternal(booking);
            throw new ResourceNotFoundException("Booking not found");
        }
        return toBookingResponse(booking);
    }

    @Transactional
    public void releaseSeatLock(Long bookingId, String userEmail, boolean isAdmin) {
        LocalDateTime now = LocalDateTime.now();
        purgeExpiredLocks(now);

        Booking booking = bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (booking.getBookingStatus() != BookingStatus.LOCKED) {
            throw new BadRequestException("Only locked bookings can be released");
        }
        if (!isAdmin && !booking.getBookedByUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("You are not allowed to release this booking lock");
        }
        releaseLockInternal(booking);
    }

    @Transactional
    public void cancelBooking(Long bookingId, String userEmail, boolean isAdmin) {
        LocalDateTime now = LocalDateTime.now();
        purgeExpiredLocks(now);

        Booking booking = bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        AppUser cancelledBy = requireUser(userEmail);

        if (!isAdmin && !booking.getBookedByUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("You are not allowed to cancel this booking");
        }

        if (booking.getBookingStatus() == BookingStatus.LOCKED) {
            releaseLockInternal(booking);
            return;
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking already cancelled");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);
        booking.setCancelledByUser(cancelledBy);
        bookingRepository.save(booking);
    }

    @Transactional
    public List<BookingHistoryResponse> getMyBookings(String userEmail) {
        purgeExpiredLocks(LocalDateTime.now());
        userRepository.findByEmailIgnoreCase(userEmail).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return bookingRepository
                .findByBookedByUserEmailIgnoreCaseAndBookingStatusNotOrderByBookedAtDesc(userEmail, BookingStatus.LOCKED)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    @Transactional
    public List<BookingHistoryResponse> getAllBookingsForAdmin() {
        purgeExpiredLocks(LocalDateTime.now());
        return bookingRepository.findByBookingStatusNotOrderByBookedAtDesc(BookingStatus.LOCKED).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    @Transactional
    public BookingPageResponse getMyBookingsPaged(String userEmail, BookingStatus status, int page, int size) {
        purgeExpiredLocks(LocalDateTime.now());
        userRepository.findByEmailIgnoreCase(userEmail).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        BookingStatus normalizedStatus = BookingStatus.normalizeFilter(status);
        if (normalizedStatus == BookingStatus.LOCKED) {
            throw new BadRequestException("LOCKED status is transient and not available for history");
        }

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Booking> bookingsPage = normalizedStatus == null
                ? bookingRepository.findByBookedByUserEmailIgnoreCaseAndBookingStatusNotOrderByBookedAtDesc(
                        userEmail, BookingStatus.LOCKED, pageRequest)
                : bookingRepository.findByBookedByUserEmailIgnoreCaseAndBookingStatusOrderByBookedAtDesc(
                        userEmail, normalizedStatus, pageRequest);
        return toPageResponse(bookingsPage, page, size);
    }

    @Transactional
    public BookingPageResponse getAllBookingsForAdminPaged(BookingStatus status, int page, int size) {
        purgeExpiredLocks(LocalDateTime.now());
        BookingStatus normalizedStatus = BookingStatus.normalizeFilter(status);
        if (normalizedStatus == BookingStatus.LOCKED) {
            throw new BadRequestException("LOCKED status is transient and not available for history");
        }

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Booking> bookingsPage = normalizedStatus == null
                ? bookingRepository.findByBookingStatusNotOrderByBookedAtDesc(BookingStatus.LOCKED, pageRequest)
                : bookingRepository.findByBookingStatusOrderByBookedAtDesc(normalizedStatus, pageRequest);
        return toPageResponse(bookingsPage, page, size);
    }

    @Transactional
    public int cleanupExpiredLocks() {
        return purgeExpiredLocks(LocalDateTime.now());
    }

    private int purgeExpiredLocks(LocalDateTime now) {
        List<Booking> expiredLocks = bookingRepository.findByBookingStatusAndLockExpiresAtBefore(
                BookingStatus.LOCKED, now.plusNanos(1));
        if (expiredLocks.isEmpty()) {
            return 0;
        }

        List<Long> expiredLockIds = expiredLocks.stream().map(Booking::getId).toList();
        bookingSeatRepository.deleteByBookingIdIn(expiredLockIds);
        bookingRepository.deleteAllByIdInBatch(expiredLockIds);
        return expiredLockIds.size();
    }

    private AppUser requireUser(String userEmail) {
        return userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private List<Integer> normalizeSeatNumbers(Integer seatNumber, List<Integer> seatNumbers) {
        LinkedHashSet<Integer> uniqueSeats = new LinkedHashSet<>();
        if (seatNumbers != null) {
            for (Integer seat : seatNumbers) {
                if (seat != null) {
                    uniqueSeats.add(seat);
                }
            }
        }
        if (seatNumber != null) {
            uniqueSeats.add(seatNumber);
        }

        if (uniqueSeats.isEmpty()) {
            throw new BadRequestException("At least one seat number is required");
        }
        return new ArrayList<>(uniqueSeats);
    }

    private List<Integer> validateSeatNumbers(List<Integer> seatNumbers, int totalSeats) {
        for (Integer seatNumber : seatNumbers) {
            if (seatNumber == null || seatNumber < 1) {
                throw new BadRequestException("Seat number must be at least 1");
            }
            if (seatNumber > totalSeats) {
                throw new BadRequestException("Seat number exceeds bus capacity");
            }
        }
        return seatNumbers;
    }

    private Map<Integer, Seat> loadScheduleSeats(TripSchedule schedule, List<Integer> requestedSeatNumbers) {
        List<Seat> seats = seatRepository.findByBusIdAndSeatNumberInAndActiveTrueOrderBySeatNumberAsc(
                schedule.getBus().getId(), requestedSeatNumbers);
        if (seats.size() < requestedSeatNumbers.size()) {
            ensureSeatInventoryForBus(schedule);
            seats = seatRepository.findByBusIdAndSeatNumberInAndActiveTrueOrderBySeatNumberAsc(
                    schedule.getBus().getId(), requestedSeatNumbers);
        }
        Map<Integer, Seat> seatByNumber = new HashMap<>();
        for (Seat seat : seats) {
            seatByNumber.put(seat.getSeatNumber(), seat);
        }

        List<Integer> missingSeats = requestedSeatNumbers.stream()
                .filter(seatNumber -> !seatByNumber.containsKey(seatNumber))
                .sorted()
                .toList();
        if (!missingSeats.isEmpty()) {
            throw new BadRequestException("Seat not available on this bus: " + missingSeats);
        }
        return seatByNumber;
    }

    private void ensureSeatInventoryForBus(TripSchedule schedule) {
        Long busId = schedule.getBus().getId();
        int totalSeats = schedule.getBus().getTotalSeats();
        List<Seat> currentSeats = seatRepository.findByBusIdOrderBySeatNumberAsc(busId);
        Map<Integer, Seat> seatByNumber = new HashMap<>();
        for (Seat seat : currentSeats) {
            seatByNumber.put(seat.getSeatNumber(), seat);
        }

        for (int seatNumber = 1; seatNumber <= totalSeats; seatNumber++) {
            Seat seat = seatByNumber.get(seatNumber);
            if (seat == null) {
                Seat newSeat = new Seat();
                newSeat.setBus(schedule.getBus());
                newSeat.setSeatNumber(seatNumber);
                newSeat.setActive(true);
                seatRepository.save(newSeat);
            } else if (!Boolean.TRUE.equals(seat.getActive())) {
                seat.setActive(true);
                seatRepository.save(seat);
            }
        }
    }

    private void ensureSeatsAreAvailable(
            Long tripScheduleId, List<Integer> requestedSeatNumbers, LocalDateTime now, Long ignoredBookingId) {
        Set<Integer> requestedSet = new HashSet<>(requestedSeatNumbers);
        List<Booking> activeBookings = bookingRepository.findByTripScheduleIdAndBookingStatusIn(
                tripScheduleId, ACTIVE_SEAT_BLOCKING_STATUSES);

        for (Booking booking : activeBookings) {
            if (ignoredBookingId != null && ignoredBookingId.equals(booking.getId())) {
                continue;
            }
            if (booking.getBookingStatus() == BookingStatus.LOCKED
                    && (booking.getLockExpiresAt() == null || !booking.getLockExpiresAt().isAfter(now))) {
                continue;
            }

            for (Integer seatNumber : extractActiveSeatNumbers(booking)) {
                if (requestedSet.contains(seatNumber)) {
                    throw new SeatAlreadyBookedException(SEAT_CONFLICT_MESSAGE);
                }
            }
        }
    }

    private List<BookingSeat> buildBookingSeats(
            Booking booking, List<Integer> requestedSeatNumbers, Map<Integer, Seat> seatByNumber) {
        return requestedSeatNumbers.stream()
                .sorted()
                .map(seatNumber -> {
                    BookingSeat bookingSeat = new BookingSeat();
                    bookingSeat.setBooking(booking);
                    bookingSeat.setSeat(seatByNumber.get(seatNumber));
                    bookingSeat.setActive(true);
                    return bookingSeat;
                })
                .toList();
    }

    private BigDecimal calculateAmount(BigDecimal baseFare, int seatsCount) {
        return baseFare.multiply(BigDecimal.valueOf(seatsCount));
    }

    private void releaseLockInternal(Booking booking) {
        bookingSeatRepository.deleteByBookingId(booking.getId());
        bookingRepository.deleteById(booking.getId());
    }

    private BookingPageResponse toPageResponse(Page<Booking> bookingsPage, int page, int size) {
        return BookingPageResponse.builder()
                .items(bookingsPage.getContent().stream().map(this::toHistoryResponse).toList())
                .page(page)
                .size(size)
                .totalElements(bookingsPage.getTotalElements())
                .totalPages(bookingsPage.getTotalPages())
                .first(bookingsPage.isFirst())
                .last(bookingsPage.isLast())
                .build();
    }

    private BookingResponse toBookingResponse(Booking booking) {
        List<Integer> seatNumbers = extractActiveSeatNumbers(booking);
        return BookingResponse.builder()
                .bookingId(booking.getId())
                .tripScheduleId(booking.getTripSchedule().getId())
                .seatNumbers(seatNumbers)
                .seatNumber(seatNumbers.isEmpty() ? null : seatNumbers.get(0))
                .passengerName(booking.getPassengerName())
                .passengerPhone(booking.getPassengerPhone())
                .paymentMode(booking.getPaymentMode())
                .amount(booking.getAmount())
                .bookedAt(booking.getBookedAt())
                .bookingStatus(normalizeResponseStatus(booking.getBookingStatus()))
                .lockExpiresAt(booking.getLockExpiresAt())
                .build();
    }

    private BookingHistoryResponse toHistoryResponse(Booking booking) {
        List<Integer> seatNumbers = extractActiveSeatNumbers(booking);
        return BookingHistoryResponse.builder()
                .bookingId(booking.getId())
                .tripScheduleId(booking.getTripSchedule().getId())
                .travelDate(booking.getTripSchedule().getTravelDate())
                .departureTime(booking.getTripSchedule().getDepartureTime())
                .arrivalTime(booking.getTripSchedule().getArrivalTime())
                .source(booking.getTripSchedule().getRoute().getSource())
                .destination(booking.getTripSchedule().getRoute().getDestination())
                .busNumber(booking.getTripSchedule().getBus().getBusNumber())
                .seatNumbers(seatNumbers)
                .seatNumber(seatNumbers.isEmpty() ? null : seatNumbers.get(0))
                .passengerName(booking.getPassengerName())
                .passengerPhone(booking.getPassengerPhone())
                .paymentMode(booking.getPaymentMode())
                .amount(booking.getAmount())
                .bookedAt(booking.getBookedAt())
                .bookingStatus(normalizeResponseStatus(booking.getBookingStatus()))
                .cancelledAt(booking.getCancelledAt())
                .cancelledByUserId(
                        booking.getCancelledByUser() != null ? booking.getCancelledByUser().getId() : null)
                .cancelledByName(
                        booking.getCancelledByUser() != null ? booking.getCancelledByUser().getName() : null)
                .cancelledByEmail(
                        booking.getCancelledByUser() != null ? booking.getCancelledByUser().getEmail() : null)
                .bookedByUserId(booking.getBookedByUser().getId())
                .bookedByName(booking.getBookedByUser().getName())
                .bookedByEmail(booking.getBookedByUser().getEmail())
                .build();
    }

    private BookingStatus normalizeResponseStatus(BookingStatus status) {
        return status == BookingStatus.CONFIRMED ? BookingStatus.BOOKED : status;
    }

    private List<Integer> extractActiveSeatNumbers(Booking booking) {
        if (booking.getBookingSeats() == null) {
            return List.of();
        }
        return booking.getBookingSeats().stream()
                .filter(bookingSeat -> Boolean.TRUE.equals(bookingSeat.getActive()))
                .map(bookingSeat -> bookingSeat.getSeat().getSeatNumber())
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
