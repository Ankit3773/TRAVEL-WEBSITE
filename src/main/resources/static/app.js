const STEP_ORDER = ['search', 'buses', 'seats', 'payment', 'confirmation'];
const POLL_INTERVAL_MS = 10000;

const elements = {
  routes: document.getElementById('route-list'),
  tourismRoutes: document.getElementById('tourism-route-list'),
  routeCount: document.getElementById('route-count'),
  tourismCount: document.getElementById('tourism-count'),
  scheduleList: document.getElementById('schedule-list'),
  seatSummary: document.getElementById('seat-summary'),
  seatGrid: document.getElementById('seat-grid'),
  searchForm: document.getElementById('search-form'),
  searchStatus: document.getElementById('search-status'),
  authStatus: document.getElementById('auth-status'),
  forgotPanel: document.getElementById('forgot-password-panel'),
  forgotStatus: document.getElementById('forgot-status'),
  bookingForm: document.getElementById('booking-form'),
  bookingStatus: document.getElementById('booking-status'),
  myBookings: document.getElementById('my-bookings'),
  adminPanel: document.getElementById('admin-panel'),
  adminStatus: document.getElementById('admin-status'),
  adminBookings: document.getElementById('admin-bookings'),
  adminMetrics: document.getElementById('admin-metrics'),
  travelDate: document.getElementById('travel-date'),
  journeyDateText: document.getElementById('journey-date-text'),
  journeyDayHint: document.getElementById('journey-day-hint'),
  todayDateBtn: document.getElementById('date-today-btn'),
  tomorrowDateBtn: document.getElementById('date-tomorrow-btn'),
  dateContent: document.querySelector('.date-content'),
  stepper: document.getElementById('stepper'),
  flowStatus: document.getElementById('flow-status'),
  busListCopy: document.getElementById('bus-list-copy'),
  journeySummary: document.getElementById('journey-summary'),
  paymentSummary: document.getElementById('payment-summary'),
  paymentActionTitle: document.getElementById('payment-action-title'),
  paymentActionCopy: document.getElementById('payment-action-copy'),
  paymentActionBtn: document.getElementById('payment-action-btn'),
  paymentStatus: document.getElementById('payment-status'),
  confirmationCard: document.getElementById('confirmation-card'),
  views: {
    search: document.getElementById('view-search'),
    buses: document.getElementById('view-buses'),
    seats: document.getElementById('view-seats'),
    payment: document.getElementById('view-payment'),
    confirmation: document.getElementById('view-confirmation')
  }
};

const state = {
  currentStep: 'search',
  routeList: [],
  tourismRouteList: [],
  schedules: [],
  selectedSchedule: null,
  selectedSeat: null,
  seatData: null,
  pendingTraveller: null,
  paymentDraft: null,
  confirmation: null,
  seatPoller: null
};

function toIsoDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function parseIsoDate(isoDate) {
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function formatJourneyDate(isoDate) {
  if (!isoDate) return '--';
  const date = parseIsoDate(isoDate);
  const text = date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  const [day, month, year] = text.split(' ');
  return `${day} ${month}, ${year}`;
}

function formatDisplayDate(isoDate) {
  if (!isoDate) return '-';
  return parseIsoDate(isoDate).toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric'
  });
}

function formatTime(timeValue) {
  if (!timeValue) return '-';
  return timeValue.slice(0, 5);
}

function formatAmount(amount) {
  if (amount == null) return '-';
  return `Rs ${Number(amount).toFixed(0)}`;
}

function journeyHint(isoDate) {
  if (!isoDate) return '(Select date)';
  const selected = parseIsoDate(isoDate);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const tomorrow = new Date(today);
  tomorrow.setDate(tomorrow.getDate() + 1);
  selected.setHours(0, 0, 0, 0);

  if (selected.getTime() === today.getTime()) return '(Today)';
  if (selected.getTime() === tomorrow.getTime()) return '(Tomorrow)';
  return `(${selected.toLocaleDateString('en-US', { weekday: 'long' })})`;
}

function refreshJourneyDateUI() {
  const selectedDate = elements.travelDate.value;
  const today = toIsoDate(new Date());
  const tomorrow = toIsoDate(new Date(Date.now() + 24 * 60 * 60 * 1000));

  elements.journeyDateText.textContent = formatJourneyDate(selectedDate);
  elements.journeyDayHint.textContent = journeyHint(selectedDate);
  elements.todayDateBtn.classList.toggle('active', selectedDate === today);
  elements.tomorrowDateBtn.classList.toggle('active', selectedDate === tomorrow);
}

function setJourneyDate(offsetDays) {
  const date = new Date();
  date.setHours(0, 0, 0, 0);
  date.setDate(date.getDate() + offsetDays);
  elements.travelDate.value = toIsoDate(date);
  refreshJourneyDateUI();
}

function currentUser() {
  const raw = localStorage.getItem('authUser');
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

function isAdmin() {
  return currentUser()?.role === 'ADMIN';
}

function getToken() {
  return localStorage.getItem('authToken');
}

function setAuth(data) {
  localStorage.setItem('authToken', data.token);
  localStorage.setItem('authUser', JSON.stringify(data));
  updateAuthStatus();
}

function clearAuth() {
  localStorage.removeItem('authToken');
  localStorage.removeItem('authUser');
  updateAuthStatus();
}

function showForgotStatus(message, isError = false) {
  elements.forgotStatus.textContent = message;
  elements.forgotStatus.className = `summary ${isError ? 'status-err' : 'status-ok'}`;
}

function normalizeBookingStatus(status) {
  return status === 'CONFIRMED' ? 'BOOKED' : status;
}

function formatSeatNumbers(booking) {
  if (Array.isArray(booking.seatNumbers) && booking.seatNumbers.length > 0) {
    return booking.seatNumbers.join(', ');
  }
  return booking.seatNumber ?? '-';
}

function formatPaymentStatus(booking) {
  return booking.paymentStatus || 'PENDING';
}

async function api(url, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (options.body) headers['Content-Type'] = 'application/json';
  if (options.auth && getToken()) headers.Authorization = `Bearer ${getToken()}`;

  const res = await fetch(url, { ...options, headers });
  const isJson = res.headers.get('content-type')?.includes('application/json');
  const body = isJson ? await res.json() : null;
  if (!res.ok) throw new Error(body?.error || 'Request failed');
  return body;
}

function setFlowStep(step, statusMessage) {
  state.currentStep = step;
  const currentIndex = STEP_ORDER.indexOf(step);

  STEP_ORDER.forEach((stepName, index) => {
    const view = elements.views[stepName];
    if (view) {
      view.classList.toggle('hidden', stepName !== step);
      view.classList.toggle('active', stepName === step);
    }

    const stepItem = elements.stepper.querySelector(`[data-step="${stepName}"]`);
    if (!stepItem) return;
    stepItem.classList.remove('active', 'complete');
    if (index < currentIndex) stepItem.classList.add('complete');
    if (index === currentIndex) stepItem.classList.add('active');
  });

  elements.flowStatus.textContent = statusMessage || flowMessageForStep(step);
  if (step !== 'seats') {
    stopSeatPolling();
  }
  updateJourneySummary();
}

function flowMessageForStep(step) {
  switch (step) {
    case 'search':
      return 'Start by searching a route.';
    case 'buses':
      return 'Review matching schedules and choose one bus.';
    case 'seats':
      return 'Select a free seat while availability keeps refreshing.';
    case 'payment':
      return 'Review the trip and finish the payment step.';
    case 'confirmation':
      return 'Booking confirmed. The final summary is ready.';
    default:
      return 'Continue the booking flow.';
  }
}

function scheduleRouteLabel(schedule) {
  if (!schedule) return 'Not selected';
  return `${schedule.route.source} -> ${schedule.route.destination}`;
}

function selectedPaymentLabel() {
  if (state.confirmation) {
    return `${state.confirmation.paymentMode} / ${state.confirmation.paymentStatus}`;
  }
  if (state.pendingTraveller) {
    return state.pendingTraveller.paymentMode;
  }
  return 'Not selected';
}

function updateJourneySummary() {
  const schedule = state.selectedSchedule;
  const date = state.confirmation?.travelDate || schedule?.travelDate;
  const busLabel = state.confirmation ? `#${state.confirmation.bookingId}` : schedule?.bus?.busNumber;
  const seatLabel = state.confirmation
    ? formatSeatNumbers(state.confirmation)
    : state.selectedSeat ?? 'Not selected';

  elements.journeySummary.innerHTML = `
    <div><span>Route</span><strong>${scheduleRouteLabel(schedule)}</strong></div>
    <div><span>Date</span><strong>${formatDisplayDate(date)}</strong></div>
    <div><span>Bus</span><strong>${busLabel || 'Not selected'}</strong></div>
    <div><span>Seat</span><strong>${seatLabel}</strong></div>
    <div><span>Payment</span><strong>${selectedPaymentLabel()}</strong></div>
  `;
}

function renderRoutes(routes) {
  state.routeList = routes;
  elements.routeCount.textContent = String(routes.length);
  if (!routes.length) {
    elements.routes.innerHTML = '<div class="item muted">No routes found.</div>';
    return;
  }

  elements.routes.innerHTML = routes
    .map(route => `
      <div class="item">
        <div class="row"><strong>${route.source} -> ${route.destination}</strong><span>${route.distanceKm} km</span></div>
        <div class="row muted"><span>${route.tourismRoute ? 'Tourism route' : 'Regular route'}</span><span>${route.active ? 'Active' : 'Inactive'}</span></div>
      </div>
    `)
    .join('');
}

function renderTourismRoutes(routes) {
  state.tourismRouteList = routes;
  elements.tourismCount.textContent = String(routes.length);
  if (!routes.length) {
    elements.tourismRoutes.innerHTML = '<div class="item muted">No tourism routes available right now.</div>';
    return;
  }

  elements.tourismRoutes.innerHTML = routes
    .map(route => `
      <div class="item">
        <div class="row"><strong>${route.source} -> ${route.destination}</strong><span>${route.distanceKm} km</span></div>
        <div class="row muted"><span>Tourism Route</span><span>AC Fleet Circuit</span></div>
      </div>
    `)
    .join('');
}

function renderSchedules(schedules) {
  state.schedules = schedules;
  if (!schedules.length) {
    elements.scheduleList.innerHTML = '<div class="item muted">No schedules for selected search.</div>';
    return;
  }

  elements.scheduleList.innerHTML = schedules
    .map(schedule => `
      <article class="bus-card">
        <div class="bus-card-top">
          <div>
            <p class="section-tag">${schedule.route.source} to ${schedule.route.destination}</p>
            <h4>${schedule.bus.busNumber}</h4>
          </div>
          <strong>${formatAmount(schedule.baseFare)}</strong>
        </div>
        <div class="bus-line">
          <span class="bus-chip">${schedule.bus.busType}</span>
          <span class="bus-chip">${formatDisplayDate(schedule.travelDate)}</span>
          <span class="bus-chip">${formatTime(schedule.departureTime)} - ${formatTime(schedule.arrivalTime)}</span>
          <span class="bus-chip">${schedule.route.distanceKm} km</span>
        </div>
        <div class="summary-row summary-row-bus">
          <span class="hint">Schedule #${schedule.id}</span>
          <button type="button" data-schedule-id="${schedule.id}">Choose Seats</button>
        </div>
      </article>
    `)
    .join('');

  elements.scheduleList.querySelectorAll('[data-schedule-id]').forEach(button => {
    button.addEventListener('click', () => {
      const scheduleId = Number(button.dataset.scheduleId);
      const selected = state.schedules.find(schedule => schedule.id === scheduleId);
      if (!selected) return;
      state.selectedSchedule = selected;
      state.selectedSeat = null;
      state.pendingTraveller = null;
      state.paymentDraft = null;
      state.confirmation = null;
      loadSeats(scheduleId, true);
    });
  });
}

function renderSeats(data) {
  state.seatData = data;
  const lockedSeats = data.lockedSeats || [];
  const booked = new Set(data.bookedSeats || []);
  const locked = new Set(lockedSeats);
  const available = new Set(data.availableSeats || []);

  if (state.selectedSeat && !available.has(state.selectedSeat)) {
    state.selectedSeat = null;
    elements.bookingForm.classList.add('hidden');
    elements.bookingStatus.textContent = 'The previously selected seat is no longer available. Choose another seat.';
    elements.bookingStatus.className = 'summary status-err';
  }

  elements.seatSummary.textContent =
    `Schedule ${data.tripScheduleId}: ${data.bookedSeats.length} booked, ${lockedSeats.length} locked, ${data.availableSeats.length} available out of ${data.totalSeats}. Auto-refresh every ${POLL_INTERVAL_MS / 1000}s.`;

  elements.seatGrid.innerHTML = Array.from({ length: data.totalSeats }, (_, index) => index + 1)
    .map(num => {
      const isBooked = booked.has(num);
      const isLocked = locked.has(num);
      const cls = isBooked
        ? 'seat booked'
        : isLocked
          ? 'seat locked'
          : `seat available${state.selectedSeat === num ? ' selected' : ''}`;
      return `<div class="${cls}" data-seat="${num}">${num}</div>`;
    })
    .join('');

  elements.seatGrid.querySelectorAll('.seat.available').forEach(seatEl => {
    seatEl.addEventListener('click', () => {
      if (!getToken()) {
        elements.bookingStatus.textContent = 'Login first to continue to the seat selection step.';
        elements.bookingStatus.className = 'summary status-err';
        return;
      }
      state.selectedSeat = Number(seatEl.dataset.seat);
      document.getElementById('book-seat').value = state.selectedSeat;
      elements.bookingForm.classList.remove('hidden');
      elements.bookingStatus.textContent = `Seat ${state.selectedSeat} selected. Fill traveller details to continue.`;
      elements.bookingStatus.className = 'summary status-ok';
      renderSeats(state.seatData);
      updateJourneySummary();
    });
  });
}

function startSeatPolling() {
  stopSeatPolling();
  if (!state.selectedSchedule) return;
  state.seatPoller = window.setInterval(() => {
    if (state.currentStep !== 'seats' || !state.selectedSchedule) return;
    loadSeats(state.selectedSchedule.id, false, true);
  }, POLL_INTERVAL_MS);
}

function stopSeatPolling() {
  if (state.seatPoller) {
    window.clearInterval(state.seatPoller);
    state.seatPoller = null;
  }
}

async function loadRoutes() {
  try {
    renderRoutes(await api('/api/routes'));
  } catch {
    elements.routes.innerHTML = '<div class="item muted">Failed to load routes.</div>';
  }
}

async function loadTourismRoutes() {
  try {
    renderTourismRoutes(await api('/api/routes/tourism'));
  } catch {
    elements.tourismRoutes.innerHTML = '<div class="item muted">Failed to load tourism routes.</div>';
  }
}

async function loadSchedules(params = '') {
  try {
    const schedules = await api(`/api/schedules${params}`);
    renderSchedules(schedules);
    return schedules;
  } catch {
    elements.scheduleList.innerHTML = '<div class="item muted">Failed to load schedules.</div>';
    return null;
  }
}

async function loadSeats(scheduleId, activateStep = true, silent = false) {
  state.selectedSchedule = state.selectedSchedule || state.schedules.find(schedule => schedule.id === Number(scheduleId)) || null;
  if (!silent) {
    state.selectedSeat = null;
    state.pendingTraveller = null;
    state.paymentDraft = null;
    state.confirmation = null;
    elements.bookingForm.classList.add('hidden');
    elements.paymentStatus.textContent = '';
    elements.bookingStatus.textContent = '';
  }

  try {
    const data = await api(`/api/schedules/${scheduleId}/seats`);
    renderSeats(data);
    if (activateStep) {
      setFlowStep('seats', `Seat Selection Page ready for ${scheduleRouteLabel(state.selectedSchedule)}.`);
    }
    if (activateStep || state.currentStep === 'seats') {
      startSeatPolling();
    }
  } catch {
    elements.seatSummary.textContent = 'Failed to load seat availability.';
    elements.seatGrid.innerHTML = '';
  }
}

function renderPaymentSummary() {
  const schedule = state.selectedSchedule;
  const pending = state.pendingTraveller;
  if (!schedule || !pending) {
    elements.paymentSummary.innerHTML = '<div><span>Status</span><strong>No pending booking</strong></div>';
    return;
  }

  elements.paymentSummary.innerHTML = `
    <div><span>Route</span><strong>${schedule.route.source} -> ${schedule.route.destination}</strong></div>
    <div><span>Date</span><strong>${formatDisplayDate(schedule.travelDate)}</strong></div>
    <div><span>Bus</span><strong>${schedule.bus.busNumber} (${schedule.bus.busType})</strong></div>
    <div><span>Time</span><strong>${formatTime(schedule.departureTime)} - ${formatTime(schedule.arrivalTime)}</strong></div>
    <div><span>Seat</span><strong>${state.selectedSeat}</strong></div>
    <div><span>Passenger</span><strong>${pending.passengerName}</strong></div>
    <div><span>Phone</span><strong>${pending.passengerPhone}</strong></div>
    <div><span>Fare</span><strong>${formatAmount(schedule.baseFare)}</strong></div>
    <div><span>Payment Mode</span><strong>${pending.paymentMode}</strong></div>
  `;
}

function refreshPaymentAction() {
  const pending = state.pendingTraveller;
  if (!pending) {
    elements.paymentActionTitle.textContent = 'Payment Action';
    elements.paymentActionCopy.textContent = 'Create a payment session or confirm pay-on-board.';
    elements.paymentActionBtn.textContent = 'Continue';
    elements.paymentActionBtn.disabled = true;
    return;
  }

  elements.paymentActionBtn.disabled = false;

  if (pending.paymentMode === 'ONLINE' && !state.paymentDraft) {
    elements.paymentActionTitle.textContent = 'Create Mock Checkout';
    elements.paymentActionCopy.textContent = 'A seat lock and mock payment session will be created first.';
    elements.paymentActionBtn.textContent = 'Start payment';
    return;
  }

  if (pending.paymentMode === 'ONLINE' && state.paymentDraft) {
    elements.paymentActionTitle.textContent = 'Complete Mock Payment';
    elements.paymentActionCopy.textContent = `Session ${state.paymentDraft.paymentSessionId} is ready. Verify it to finalize the booking.`;
    elements.paymentActionBtn.textContent = 'Complete payment';
    return;
  }

  elements.paymentActionTitle.textContent = 'Confirm Pay On Board';
  elements.paymentActionCopy.textContent = 'This final step creates the booking with payment status PENDING.';
  elements.paymentActionBtn.textContent = 'Confirm booking';
}

function renderConfirmation(booking) {
  elements.confirmationCard.innerHTML = `
    <div class="confirmation-row"><span>Booking ID</span><strong>#${booking.bookingId}</strong></div>
    <div class="confirmation-row"><span>Route</span><strong>${booking.source} -> ${booking.destination}</strong></div>
    <div class="confirmation-row"><span>Travel</span><strong>${formatDisplayDate(booking.travelDate)} | ${formatTime(booking.departureTime)}</strong></div>
    <div class="confirmation-row"><span>Seats</span><strong>${formatSeatNumbers(booking)}</strong></div>
    <div class="confirmation-row"><span>Passenger</span><strong>${booking.passengerName}</strong></div>
    <div class="confirmation-row"><span>Booking Status</span><strong>${normalizeBookingStatus(booking.bookingStatus)}</strong></div>
    <div class="confirmation-row"><span>Payment</span><strong>${booking.paymentMode} / ${formatPaymentStatus(booking)}</strong></div>
    <div class="confirmation-row"><span>Amount</span><strong>${formatAmount(booking.amount)}</strong></div>
    <div class="confirmation-row"><span>Reference</span><strong>${booking.paymentReference || booking.paymentSessionId || 'Not required'}</strong></div>
  `;
}

function renderMyBookings(items) {
  if (!items || !items.length) {
    elements.myBookings.innerHTML = '<div class="item muted">No bookings yet.</div>';
    return;
  }

  elements.myBookings.innerHTML = items.map(booking => `
    <div class="item">
      <div class="row"><strong>${booking.source} -> ${booking.destination}</strong><span>${normalizeBookingStatus(booking.bookingStatus)} | ${formatPaymentStatus(booking)}</span></div>
      <div class="row muted"><span>Seat(s) ${formatSeatNumbers(booking)} | ${formatDisplayDate(booking.travelDate)} ${formatTime(booking.departureTime)}</span><span>${formatAmount(booking.amount)}</span></div>
      <div class="row">
        <span class="muted">Booking #${booking.bookingId}</span>
        ${normalizeBookingStatus(booking.bookingStatus) === 'BOOKED'
          ? `<button type="button" data-cancel="${booking.bookingId}">Cancel</button>`
          : '<span class="muted">Cancelled</span>'}
      </div>
    </div>
  `).join('');

  elements.myBookings.querySelectorAll('[data-cancel]').forEach(button => {
    button.addEventListener('click', async () => {
      try {
        await api(`/api/bookings/${button.dataset.cancel}`, { method: 'DELETE', auth: true });
        await loadMyBookings();
        if (isAdmin()) {
          await loadAdminMetrics();
          await loadAdminBookings();
        }
        if (state.selectedSchedule) await loadSeats(state.selectedSchedule.id, false, true);
      } catch (err) {
        alert(err.message);
      }
    });
  });
}

async function loadMyBookings() {
  if (!getToken()) {
    elements.myBookings.innerHTML = '<div class="item muted">Login to see your bookings.</div>';
    return;
  }

  try {
    renderMyBookings(await api('/api/my-bookings', { auth: true }));
  } catch {
    elements.myBookings.innerHTML = '<div class="item muted">Could not fetch bookings.</div>';
  }
}

function renderAdminBookings(items) {
  if (!items || !items.length) {
    elements.adminBookings.innerHTML = '<div class="item muted">No bookings found.</div>';
    return;
  }

  elements.adminBookings.innerHTML = items.map(booking => `
    <div class="item">
      <div class="row"><strong>${booking.source} -> ${booking.destination}</strong><span>${normalizeBookingStatus(booking.bookingStatus)} | ${formatPaymentStatus(booking)}</span></div>
      <div class="row muted"><span>#${booking.bookingId} | Seat(s) ${formatSeatNumbers(booking)}</span><span>${booking.bookedByEmail}</span></div>
      <div class="row muted"><span>${formatDisplayDate(booking.travelDate)} ${formatTime(booking.departureTime)}</span><span>${formatAmount(booking.amount)}</span></div>
    </div>
  `).join('');
}

async function loadAdminBookings() {
  if (!isAdmin()) return;
  try {
    const bookings = await api('/api/admin/bookings', { auth: true });
    renderAdminBookings(bookings);
  } catch (err) {
    elements.adminBookings.innerHTML = `<div class="item muted">${err.message}</div>`;
  }
}

function renderAdminMetrics(metrics) {
  elements.adminMetrics.innerHTML = `
    <div class="metric"><div class="label">Total Bookings</div><div class="value">${metrics.totalBookings}</div></div>
    <div class="metric"><div class="label">Booked</div><div class="value">${metrics.confirmedBookings}</div></div>
    <div class="metric"><div class="label">Cancelled</div><div class="value">${metrics.cancelledBookings}</div></div>
    <div class="metric"><div class="label">Active Routes</div><div class="value">${metrics.activeRoutes}</div></div>
    <div class="metric"><div class="label">Active Buses</div><div class="value">${metrics.activeBuses}</div></div>
    <div class="metric"><div class="label">Active Schedules</div><div class="value">${metrics.activeSchedules}</div></div>
    <div class="metric"><div class="label">Upcoming Schedules</div><div class="value">${metrics.upcomingSchedules}</div></div>
    <div class="metric"><div class="label">Seat Capacity</div><div class="value">${metrics.totalSeatCapacity}</div></div>
    <div class="metric"><div class="label">Occupancy %</div><div class="value">${metrics.occupancyPercent}</div></div>
  `;
}

async function loadAdminMetrics() {
  if (!isAdmin()) return;
  try {
    const metrics = await api('/api/admin/metrics', { auth: true });
    renderAdminMetrics(metrics);
  } catch (err) {
    elements.adminMetrics.innerHTML = `<div class="item muted">${err.message}</div>`;
  }
}

function updateAuthStatus() {
  const user = currentUser();
  if (!user) {
    elements.authStatus.textContent = 'Not logged in.';
    elements.bookingForm.classList.add('hidden');
    elements.adminPanel.classList.add('hidden');
    loadMyBookings();
    return;
  }

  elements.authStatus.textContent = `Logged in as ${user.name} (${user.role})`;
  loadMyBookings();
  if (user.role === 'ADMIN') {
    elements.adminPanel.classList.remove('hidden');
    loadAdminMetrics();
    loadAdminBookings();
  } else {
    elements.adminPanel.classList.add('hidden');
  }
}

async function releasePendingLock() {
  if (!state.paymentDraft?.bookingId) return;
  try {
    await api(`/api/bookings/locks/${state.paymentDraft.bookingId}`, { method: 'DELETE', auth: true });
  } catch {
    // Best-effort unlock when leaving the payment step.
  }
}

function resetBookingFlow(options = {}) {
  stopSeatPolling();
  state.selectedSchedule = null;
  state.selectedSeat = null;
  state.seatData = null;
  state.pendingTraveller = null;
  state.paymentDraft = null;
  state.confirmation = null;
  elements.bookingForm.reset();
  elements.bookingForm.classList.add('hidden');
  elements.paymentStatus.textContent = '';
  elements.bookingStatus.textContent = '';
  elements.paymentSummary.innerHTML = '<div><span>Status</span><strong>No pending booking</strong></div>';
  elements.confirmationCard.innerHTML = '';
  if (options.goToSearch !== false) {
    setFlowStep('search', 'Start by searching a route.');
  } else {
    updateJourneySummary();
  }
}

async function handlePaymentAction() {
  const pending = state.pendingTraveller;
  if (!pending || !state.selectedSchedule || !state.selectedSeat) return;

  elements.paymentActionBtn.disabled = true;
  elements.paymentStatus.textContent = 'Processing...';
  elements.paymentStatus.className = 'summary';

  const payload = {
    tripScheduleId: state.selectedSchedule.id,
    seatNumber: state.selectedSeat,
    passengerName: pending.passengerName,
    passengerPhone: pending.passengerPhone,
    paymentMode: pending.paymentMode
  };

  try {
    if (pending.paymentMode === 'ONLINE' && !state.paymentDraft) {
      const lockedBooking = await api('/api/bookings/locks', {
        method: 'POST',
        body: JSON.stringify(payload),
        auth: true
      });
      const checkout = await api(`/api/bookings/locks/${lockedBooking.bookingId}/payments/checkout`, {
        method: 'POST',
        auth: true
      });
      state.paymentDraft = {
        bookingId: lockedBooking.bookingId,
        paymentSessionId: checkout.paymentSessionId,
        amount: checkout.amount,
        paymentGateway: checkout.paymentGateway
      };
      elements.paymentStatus.textContent = `Payment session ${checkout.paymentSessionId} created for ${formatAmount(checkout.amount)}.`;
      elements.paymentStatus.className = 'summary status-ok';
      refreshPaymentAction();
      updateJourneySummary();
      return;
    }

    if (pending.paymentMode === 'ONLINE' && state.paymentDraft) {
      const response = await api(`/api/bookings/locks/${state.paymentDraft.bookingId}/payments/verify`, {
        method: 'POST',
        body: JSON.stringify({
          paymentSessionId: state.paymentDraft.paymentSessionId,
          gatewayPaymentReference: `mock-${Date.now()}`
        }),
        auth: true
      });
      state.confirmation = response;
      state.pendingTraveller = null;
      state.paymentDraft = null;
      renderConfirmation(response);
      setFlowStep('confirmation', 'Online payment completed and booking confirmed.');
    } else {
      const response = await api('/api/bookings', {
        method: 'POST',
        body: JSON.stringify(payload),
        auth: true
      });
      state.confirmation = response;
      state.pendingTraveller = null;
      renderConfirmation(response);
      setFlowStep('confirmation', 'Booking confirmed with payment on board.');
    }

    elements.bookingForm.reset();
    elements.bookingForm.classList.add('hidden');
    await loadMyBookings();
    if (isAdmin()) {
      await loadAdminMetrics();
      await loadAdminBookings();
    }
    if (state.selectedSchedule) {
      await loadSeats(state.selectedSchedule.id, false, true);
    }
  } catch (err) {
    elements.paymentStatus.textContent = err.message;
    elements.paymentStatus.className = 'summary status-err';
  } finally {
    elements.paymentActionBtn.disabled = false;
    refreshPaymentAction();
    updateJourneySummary();
  }
}

document.getElementById('register-form').addEventListener('submit', async event => {
  event.preventDefault();
  try {
    const payload = {
      name: document.getElementById('reg-name').value.trim(),
      email: document.getElementById('reg-email').value.trim(),
      password: document.getElementById('reg-password').value
    };
    const data = await api('/api/auth/register', { method: 'POST', body: JSON.stringify(payload) });
    setAuth(data);
  } catch (err) {
    alert(err.message);
  }
});

document.getElementById('login-form').addEventListener('submit', async event => {
  event.preventDefault();
  try {
    const payload = {
      email: document.getElementById('login-email').value.trim(),
      password: document.getElementById('login-password').value
    };
    const data = await api('/api/auth/login', { method: 'POST', body: JSON.stringify(payload) });
    setAuth(data);
  } catch (err) {
    alert(err.message);
  }
});

document.getElementById('forgot-toggle-btn').addEventListener('click', () => {
  elements.forgotPanel.classList.toggle('hidden');
  const loginEmail = document.getElementById('login-email').value.trim();
  if (loginEmail) document.getElementById('forgot-email').value = loginEmail;
  elements.forgotStatus.textContent = '';
});

document.getElementById('forgot-password-form').addEventListener('submit', async event => {
  event.preventDefault();
  try {
    const payload = { email: document.getElementById('forgot-email').value.trim() };
    const response = await api('/api/auth/forgot-password', { method: 'POST', body: JSON.stringify(payload) });
    if (response.resetToken) {
      document.getElementById('reset-token').value = response.resetToken;
      showForgotStatus('Reset token generated. Use it below to set a new password.');
    } else {
      showForgotStatus(response.message || 'If the email exists, reset instructions have been generated.');
    }
  } catch (err) {
    showForgotStatus(err.message, true);
  }
});

document.getElementById('reset-password-form').addEventListener('submit', async event => {
  event.preventDefault();
  try {
    const payload = {
      token: document.getElementById('reset-token').value.trim(),
      newPassword: document.getElementById('reset-new-password').value
    };
    const response = await api('/api/auth/reset-password', { method: 'POST', body: JSON.stringify(payload) });
    showForgotStatus(response.message || 'Password updated successfully. You can login now.');
    document.getElementById('login-password').value = '';
    document.getElementById('reset-new-password').value = '';
  } catch (err) {
    showForgotStatus(err.message, true);
  }
});

document.getElementById('logout-btn').addEventListener('click', async () => {
  await releasePendingLock();
  resetBookingFlow();
  clearAuth();
});

document.getElementById('admin-route-form').addEventListener('submit', async event => {
  event.preventDefault();
  try {
    const payload = {
      source: document.getElementById('admin-route-source').value.trim(),
      destination: document.getElementById('admin-route-destination').value.trim(),
      distanceKm: Number(document.getElementById('admin-route-distance').value)
    };
    await api('/api/admin/routes', { method: 'POST', body: JSON.stringify(payload), auth: true });
    elements.adminStatus.textContent = 'Route created successfully.';
    elements.adminStatus.className = 'summary status-ok';
    event.target.reset();
    await loadRoutes();
    await loadTourismRoutes();
    await loadAdminMetrics();
  } catch (err) {
    elements.adminStatus.textContent = err.message;
    elements.adminStatus.className = 'summary status-err';
  }
});

document.getElementById('admin-bus-form').addEventListener('submit', async event => {
  event.preventDefault();
  try {
    const payload = {
      busNumber: document.getElementById('admin-bus-number').value.trim(),
      busType: document.getElementById('admin-bus-type').value,
      totalSeats: Number(document.getElementById('admin-bus-seats').value)
    };
    await api('/api/admin/buses', { method: 'POST', body: JSON.stringify(payload), auth: true });
    elements.adminStatus.textContent = 'Bus created successfully.';
    elements.adminStatus.className = 'summary status-ok';
    event.target.reset();
    await loadAdminMetrics();
  } catch (err) {
    elements.adminStatus.textContent = err.message;
    elements.adminStatus.className = 'summary status-err';
  }
});

document.getElementById('admin-schedule-form').addEventListener('submit', async event => {
  event.preventDefault();
  try {
    const payload = {
      routeId: Number(document.getElementById('admin-sch-route-id').value),
      busId: Number(document.getElementById('admin-sch-bus-id').value),
      travelDate: document.getElementById('admin-sch-date').value,
      departureTime: `${document.getElementById('admin-sch-departure').value}:00`,
      arrivalTime: `${document.getElementById('admin-sch-arrival').value}:00`
    };
    await api('/api/admin/schedules', { method: 'POST', body: JSON.stringify(payload), auth: true });
    elements.adminStatus.textContent = 'Schedule created successfully. Fare was calculated from the route distance.';
    elements.adminStatus.className = 'summary status-ok';
    event.target.reset();
    await loadAdminMetrics();
  } catch (err) {
    elements.adminStatus.textContent = err.message;
    elements.adminStatus.className = 'summary status-err';
  }
});

elements.searchForm.addEventListener('submit', async event => {
  event.preventDefault();
  const source = document.getElementById('source').value.trim();
  const destination = document.getElementById('destination').value.trim();
  const date = elements.travelDate.value;
  elements.searchStatus.textContent = 'Searching schedules...';
  elements.searchStatus.className = 'summary';

  const params = new URLSearchParams();
  if (source) params.set('source', source);
  if (destination) params.set('destination', destination);
  if (date) params.set('date', date);

  let schedules = await loadSchedules(params.toString() ? `?${params.toString()}` : '');
  if (schedules === null) {
    elements.searchStatus.textContent = 'Could not search schedules. Please try again.';
    elements.searchStatus.className = 'summary status-err';
    return;
  }

  let statusText = `${schedules.length} schedule(s) found.`;
  let statusClass = 'summary status-ok';

  if (schedules.length === 0 && date) {
    const fallbackParams = new URLSearchParams();
    if (source) fallbackParams.set('source', source);
    if (destination) fallbackParams.set('destination', destination);

    const fallback = await loadSchedules(fallbackParams.toString() ? `?${fallbackParams.toString()}` : '');
    if (fallback && fallback.length > 0) {
      schedules = fallback;
      const nearestDate = fallback
        .map(schedule => schedule.travelDate)
        .sort((a, b) => Math.abs(new Date(a) - new Date(date)) - Math.abs(new Date(b) - new Date(date)))[0];
      statusText = `No buses found for ${date}. Showing nearest available date ${nearestDate}.`;
      statusClass = 'summary status-err';
    }
  }

  if (!schedules || schedules.length === 0) {
    elements.searchStatus.textContent = statusText;
    elements.searchStatus.className = statusClass;
    return;
  }

  elements.searchStatus.textContent = statusText;
  elements.searchStatus.className = statusClass;
  elements.busListCopy.textContent = `${scheduleRouteLabel(schedules[0])} | ${schedules.length} option(s) available.`;
  setFlowStep('buses', 'Bus List Page ready. Choose one schedule to continue.');
});

elements.bookingForm.addEventListener('submit', async event => {
  event.preventDefault();
  if (!state.selectedSchedule || !state.selectedSeat) return;
  state.pendingTraveller = {
    passengerName: document.getElementById('book-name').value.trim(),
    passengerPhone: document.getElementById('book-phone').value.trim(),
    paymentMode: document.getElementById('book-payment').value
  };
  state.paymentDraft = null;
  state.confirmation = null;
  renderPaymentSummary();
  refreshPaymentAction();
  setFlowStep('payment', 'Payment Page ready. Review the trip and continue.');
});

elements.paymentActionBtn.addEventListener('click', handlePaymentAction);

document.getElementById('back-to-search').addEventListener('click', () => {
  resetBookingFlow({ goToSearch: true });
});

document.getElementById('back-to-buses').addEventListener('click', () => {
  stopSeatPolling();
  setFlowStep('buses', 'Bus List Page ready. Choose another schedule if needed.');
});

document.getElementById('back-to-seats').addEventListener('click', async () => {
  if (state.pendingTraveller?.paymentMode === 'ONLINE' && state.paymentDraft?.bookingId) {
    await releasePendingLock();
    state.paymentDraft = null;
  }
  elements.paymentStatus.textContent = '';
  refreshPaymentAction();
  if (state.selectedSchedule) {
    await loadSeats(state.selectedSchedule.id, true, true);
    elements.bookingStatus.textContent = 'Returned to Seat Selection Page.';
    elements.bookingStatus.className = 'summary status-ok';
  }
});

document.getElementById('book-another-btn').addEventListener('click', () => {
  resetBookingFlow();
});

elements.todayDateBtn.addEventListener('click', () => setJourneyDate(0));

elements.tomorrowDateBtn.addEventListener('click', () => setJourneyDate(1));

elements.travelDate.addEventListener('change', refreshJourneyDateUI);

elements.dateContent.addEventListener('click', () => {
  if (typeof elements.travelDate.showPicker === 'function') {
    elements.travelDate.showPicker();
    return;
  }
  elements.travelDate.focus();
});

if (!elements.travelDate.value) {
  setJourneyDate(0);
} else {
  refreshJourneyDateUI();
}

renderPaymentSummary();
refreshPaymentAction();
updateJourneySummary();
updateAuthStatus();
loadRoutes();
loadTourismRoutes();
setFlowStep('search', 'Start by searching a route.');
