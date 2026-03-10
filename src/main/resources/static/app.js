const routesEl = document.getElementById('route-list');
const tourismRoutesEl = document.getElementById('tourism-route-list');
const scheduleEl = document.getElementById('schedule-list');
const seatSummaryEl = document.getElementById('seat-summary');
const seatGridEl = document.getElementById('seat-grid');
const searchForm = document.getElementById('search-form');
const searchStatusEl = document.getElementById('search-status');
const authStatusEl = document.getElementById('auth-status');
const forgotPanel = document.getElementById('forgot-password-panel');
const forgotStatusEl = document.getElementById('forgot-status');
const bookingForm = document.getElementById('booking-form');
const bookingStatus = document.getElementById('booking-status');
const myBookingsEl = document.getElementById('my-bookings');
const adminPanel = document.getElementById('admin-panel');
const adminStatus = document.getElementById('admin-status');
const adminBookingsEl = document.getElementById('admin-bookings');
const adminMetricsEl = document.getElementById('admin-metrics');
const travelDateEl = document.getElementById('travel-date');
const journeyDateTextEl = document.getElementById('journey-date-text');
const journeyDayHintEl = document.getElementById('journey-day-hint');
const todayDateBtn = document.getElementById('date-today-btn');
const tomorrowDateBtn = document.getElementById('date-tomorrow-btn');
const dateContentEl = document.querySelector('.date-content');

let selectedScheduleId = null;
let selectedSeat = null;

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
  const selectedDate = travelDateEl.value;
  const today = toIsoDate(new Date());
  const tomorrow = toIsoDate(new Date(Date.now() + 24 * 60 * 60 * 1000));

  journeyDateTextEl.textContent = formatJourneyDate(selectedDate);
  journeyDayHintEl.textContent = journeyHint(selectedDate);
  todayDateBtn.classList.toggle('active', selectedDate === today);
  tomorrowDateBtn.classList.toggle('active', selectedDate === tomorrow);
}

function setJourneyDate(offsetDays) {
  const date = new Date();
  date.setHours(0, 0, 0, 0);
  date.setDate(date.getDate() + offsetDays);
  travelDateEl.value = toIsoDate(date);
  refreshJourneyDateUI();
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
  forgotStatusEl.textContent = message;
  forgotStatusEl.className = `summary ${isError ? 'status-err' : 'status-ok'}`;
}

function updateAuthStatus() {
  const user = localStorage.getItem('authUser');
  if (!user) {
    authStatusEl.textContent = 'Not logged in.';
    myBookingsEl.innerHTML = '<div class="item muted">Login to see your bookings.</div>';
    bookingForm.classList.add('hidden');
    adminPanel.classList.add('hidden');
    return;
  }
  const u = JSON.parse(user);
  authStatusEl.textContent = `Logged in as ${u.name} (${u.role})`;
  loadMyBookings();
  if (u.role === 'ADMIN') {
    adminPanel.classList.remove('hidden');
    loadAdminMetrics();
    loadAdminBookings();
  } else {
    adminPanel.classList.add('hidden');
  }
}

async function api(url, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  if (options.auth && getToken()) headers.Authorization = `Bearer ${getToken()}`;

  const res = await fetch(url, { ...options, headers });
  const isJson = res.headers.get('content-type')?.includes('application/json');
  const body = isJson ? await res.json() : null;
  if (!res.ok) throw new Error(body?.error || 'Request failed');
  return body;
}

function renderRoutes(routes) {
  if (!routes.length) {
    routesEl.innerHTML = '<div class="item muted">No routes found.</div>';
    return;
  }
  routesEl.innerHTML = routes
    .map(r => `<div class="item"><div class="row"><strong>${r.source} -> ${r.destination}</strong><span>${r.distanceKm} km</span></div></div>`)
    .join('');
}

function renderTourismRoutes(routes) {
  if (!routes.length) {
    tourismRoutesEl.innerHTML = '<div class="item muted">No tourism routes available right now.</div>';
    return;
  }

  tourismRoutesEl.innerHTML = routes
    .map(r => `
      <div class="item">
        <div class="row"><strong>${r.source} -> ${r.destination}</strong><span>${r.distanceKm} km</span></div>
        <div class="row muted"><span>Tourism Route</span><span>AC Bus Service</span></div>
      </div>
    `)
    .join('');
}

function renderSchedules(schedules) {
  if (!schedules.length) {
    scheduleEl.innerHTML = '<div class="item muted">No schedules for selected search.</div>';
    return;
  }

  scheduleEl.innerHTML = schedules
    .map(s => `
      <div class="item">
        <div class="row"><strong>${s.route.source} -> ${s.route.destination}</strong><span>Rs ${s.baseFare}</span></div>
        <div class="row muted"><span>${s.travelDate} | ${s.departureTime} - ${s.arrivalTime}</span><span>${s.bus.busNumber}</span></div>
        <div class="row"><button data-id="${s.id}">View Seats</button></div>
      </div>
    `)
    .join('');

  scheduleEl.querySelectorAll('button[data-id]').forEach(btn => {
    btn.addEventListener('click', () => loadSeats(btn.dataset.id));
  });
}

function renderSeats(data) {
  const lockedSeats = data.lockedSeats || [];
  seatSummaryEl.textContent =
    `Schedule ${data.tripScheduleId}: ${data.bookedSeats.length} booked, ${lockedSeats.length} locked / ${data.totalSeats} total`;
  const booked = new Set(data.bookedSeats);
  const locked = new Set(lockedSeats);

  seatGridEl.innerHTML = Array.from({ length: data.totalSeats }, (_, i) => i + 1)
    .map(num => {
      const isBooked = booked.has(num);
      const isLocked = locked.has(num);
      const cls = isBooked
        ? 'seat booked'
        : isLocked
          ? 'seat locked'
          : `seat available${selectedSeat === num ? ' selected' : ''}`;
      return `<div class="${cls}" data-seat="${num}">${num}</div>`;
    })
    .join('');

  seatGridEl.querySelectorAll('.seat.available').forEach(el => {
    el.addEventListener('click', () => {
      if (!getToken()) {
        bookingStatus.textContent = 'Please login first to book seats.';
        bookingStatus.className = 'summary status-err';
        return;
      }
      selectedSeat = Number(el.dataset.seat);
      document.getElementById('book-seat').value = selectedSeat;
      bookingForm.classList.remove('hidden');
      Array.from(seatGridEl.children).forEach(c => c.classList.remove('selected'));
      el.classList.add('selected');
    });
  });
}

function formatSeatNumbers(booking) {
  if (Array.isArray(booking.seatNumbers) && booking.seatNumbers.length > 0) {
    return booking.seatNumbers.join(', ');
  }
  return booking.seatNumber ?? '-';
}

function normalizeBookingStatus(status) {
  return status === 'CONFIRMED' ? 'BOOKED' : status;
}

function renderMyBookings(items) {
  if (!items || !items.length) {
    myBookingsEl.innerHTML = '<div class="item muted">No bookings yet.</div>';
    return;
  }

  myBookingsEl.innerHTML = items.map(b => `
    <div class="item">
      <div class="row"><strong>${b.source} -> ${b.destination}</strong><span>${normalizeBookingStatus(b.bookingStatus)}</span></div>
      <div class="row muted"><span>Seat(s) ${formatSeatNumbers(b)} | ${b.travelDate} ${b.departureTime}</span><span>Rs ${b.amount}</span></div>
      <div class="row">
        <span class="muted">Booking #${b.bookingId}</span>
        ${normalizeBookingStatus(b.bookingStatus) === 'BOOKED'
          ? `<button data-cancel="${b.bookingId}">Cancel</button>`
          : '<span class="muted">Cancelled</span>'}
      </div>
    </div>
  `).join('');

  myBookingsEl.querySelectorAll('button[data-cancel]').forEach(btn => {
    btn.addEventListener('click', async () => {
      try {
        await api(`/api/bookings/${btn.dataset.cancel}`, { method: 'DELETE', auth: true });
        await loadMyBookings();
        await loadAdminMetrics();
        await loadAdminBookings();
        if (selectedScheduleId) await loadSeats(selectedScheduleId);
      } catch (err) {
        alert(err.message);
      }
    });
  });
}

async function loadRoutes() {
  try {
    renderRoutes(await api('/api/routes'));
  } catch {
    routesEl.innerHTML = '<div class="item muted">Failed to load routes.</div>';
  }
}

async function loadTourismRoutes() {
  try {
    renderTourismRoutes(await api('/api/routes/tourism'));
  } catch {
    tourismRoutesEl.innerHTML = '<div class="item muted">Failed to load tourism routes.</div>';
  }
}

async function loadSchedules(params = '') {
  try {
    const schedules = await api(`/api/schedules${params}`);
    renderSchedules(schedules);
    return schedules;
  } catch {
    scheduleEl.innerHTML = '<div class="item muted">Failed to load schedules.</div>';
    return null;
  }
}

async function loadSeats(scheduleId) {
  selectedScheduleId = Number(scheduleId);
  selectedSeat = null;
  bookingForm.classList.add('hidden');
  bookingStatus.textContent = '';
  try {
    renderSeats(await api(`/api/schedules/${scheduleId}/seats`));
  } catch {
    seatSummaryEl.textContent = 'Failed to load seat availability.';
    seatGridEl.innerHTML = '';
  }
}

async function loadMyBookings() {
  if (!getToken()) return;
  try {
    renderMyBookings(await api('/api/my-bookings', { auth: true }));
  } catch {
    myBookingsEl.innerHTML = '<div class="item muted">Could not fetch bookings.</div>';
  }
}

function renderAdminBookings(items) {
  if (!items || !items.length) {
    adminBookingsEl.innerHTML = '<div class="item muted">No bookings found.</div>';
    return;
  }

  adminBookingsEl.innerHTML = items.map(b => `
    <div class="item">
      <div class="row"><strong>${b.source} -> ${b.destination}</strong><span>${normalizeBookingStatus(b.bookingStatus)}</span></div>
      <div class="row muted"><span>#${b.bookingId} | Seat(s) ${formatSeatNumbers(b)}</span><span>${b.bookedByEmail}</span></div>
      <div class="row muted"><span>${b.travelDate} ${b.departureTime}</span><span>Rs ${b.amount}</span></div>
    </div>
  `).join('');
}

async function loadAdminBookings() {
  try {
    const bookings = await api('/api/admin/bookings', { auth: true });
    renderAdminBookings(bookings);
  } catch (err) {
    adminBookingsEl.innerHTML = `<div class="item muted">${err.message}</div>`;
  }
}

function renderAdminMetrics(m) {
  adminMetricsEl.innerHTML = `
    <div class="metric"><div class="label">Total Bookings</div><div class="value">${m.totalBookings}</div></div>
    <div class="metric"><div class="label">Booked</div><div class="value">${m.confirmedBookings}</div></div>
    <div class="metric"><div class="label">Cancelled</div><div class="value">${m.cancelledBookings}</div></div>
    <div class="metric"><div class="label">Active Routes</div><div class="value">${m.activeRoutes}</div></div>
    <div class="metric"><div class="label">Active Buses</div><div class="value">${m.activeBuses}</div></div>
    <div class="metric"><div class="label">Active Schedules</div><div class="value">${m.activeSchedules}</div></div>
    <div class="metric"><div class="label">Upcoming Schedules</div><div class="value">${m.upcomingSchedules}</div></div>
    <div class="metric"><div class="label">Seat Capacity</div><div class="value">${m.totalSeatCapacity}</div></div>
    <div class="metric"><div class="label">Occupancy %</div><div class="value">${m.occupancyPercent}</div></div>
  `;
}

async function loadAdminMetrics() {
  try {
    const m = await api('/api/admin/metrics', { auth: true });
    renderAdminMetrics(m);
  } catch (err) {
    adminMetricsEl.innerHTML = `<div class=\"item muted\">${err.message}</div>`;
  }
}

document.getElementById('register-form').addEventListener('submit', async (e) => {
  e.preventDefault();
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

document.getElementById('login-form').addEventListener('submit', async (e) => {
  e.preventDefault();
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
  forgotPanel.classList.toggle('hidden');
  const loginEmail = document.getElementById('login-email').value.trim();
  if (loginEmail) {
    document.getElementById('forgot-email').value = loginEmail;
  }
  forgotStatusEl.textContent = '';
});

document.getElementById('forgot-password-form').addEventListener('submit', async (e) => {
  e.preventDefault();
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

document.getElementById('reset-password-form').addEventListener('submit', async (e) => {
  e.preventDefault();
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

document.getElementById('logout-btn').addEventListener('click', () => {
  clearAuth();
});

document.getElementById('admin-route-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    const payload = {
      source: document.getElementById('admin-route-source').value.trim(),
      destination: document.getElementById('admin-route-destination').value.trim(),
      distanceKm: Number(document.getElementById('admin-route-distance').value)
    };
    await api('/api/admin/routes', { method: 'POST', body: JSON.stringify(payload), auth: true });
    adminStatus.textContent = 'Route created successfully.';
    adminStatus.className = 'summary status-ok';
    e.target.reset();
    await loadRoutes();
    await loadTourismRoutes();
    await loadAdminMetrics();
  } catch (err) {
    adminStatus.textContent = err.message;
    adminStatus.className = 'summary status-err';
  }
});

document.getElementById('admin-bus-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    const payload = {
      busNumber: document.getElementById('admin-bus-number').value.trim(),
      busType: document.getElementById('admin-bus-type').value,
      totalSeats: Number(document.getElementById('admin-bus-seats').value)
    };
    await api('/api/admin/buses', { method: 'POST', body: JSON.stringify(payload), auth: true });
    adminStatus.textContent = 'Bus created successfully.';
    adminStatus.className = 'summary status-ok';
    e.target.reset();
    await loadAdminMetrics();
  } catch (err) {
    adminStatus.textContent = err.message;
    adminStatus.className = 'summary status-err';
  }
});

document.getElementById('admin-schedule-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    const payload = {
      routeId: Number(document.getElementById('admin-sch-route-id').value),
      busId: Number(document.getElementById('admin-sch-bus-id').value),
      travelDate: document.getElementById('admin-sch-date').value,
      departureTime: `${document.getElementById('admin-sch-departure').value}:00`,
      arrivalTime: `${document.getElementById('admin-sch-arrival').value}:00`,
      baseFare: Number(document.getElementById('admin-sch-fare').value)
    };
    await api('/api/admin/schedules', { method: 'POST', body: JSON.stringify(payload), auth: true });
    adminStatus.textContent = 'Schedule created successfully.';
    adminStatus.className = 'summary status-ok';
    e.target.reset();
    await loadSchedules();
    await loadAdminMetrics();
  } catch (err) {
    adminStatus.textContent = err.message;
    adminStatus.className = 'summary status-err';
  }
});

bookingForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  if (!selectedScheduleId || !selectedSeat) return;
  try {
    const payload = {
      tripScheduleId: selectedScheduleId,
      seatNumber: selectedSeat,
      passengerName: document.getElementById('book-name').value.trim(),
      passengerPhone: document.getElementById('book-phone').value.trim(),
      paymentMode: document.getElementById('book-payment').value
    };
    await api('/api/bookings', { method: 'POST', body: JSON.stringify(payload), auth: true });
    bookingStatus.textContent = 'Booking successful.';
    bookingStatus.className = 'summary status-ok';
    bookingForm.reset();
    bookingForm.classList.add('hidden');
    await loadSeats(selectedScheduleId);
    await loadMyBookings();
    await loadAdminMetrics();
    await loadAdminBookings();
  } catch (err) {
    bookingStatus.textContent = err.message;
    bookingStatus.className = 'summary status-err';
  }
});

searchForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const source = document.getElementById('source').value.trim();
  const destination = document.getElementById('destination').value.trim();
  const date = document.getElementById('travel-date').value;
  searchStatusEl.textContent = 'Searching schedules...';
  searchStatusEl.className = 'summary';

  const q = new URLSearchParams();
  if (source) q.set('source', source);
  if (destination) q.set('destination', destination);
  if (date) q.set('date', date);

  let schedules = await loadSchedules(q.toString() ? `?${q.toString()}` : '');
  if (schedules === null) {
    searchStatusEl.textContent = 'Could not search schedules. Please try again.';
    searchStatusEl.className = 'summary status-err';
    return;
  }

  if (schedules.length === 0 && date) {
    const fallbackQ = new URLSearchParams();
    if (source) fallbackQ.set('source', source);
    if (destination) fallbackQ.set('destination', destination);

    const fallback = await loadSchedules(fallbackQ.toString() ? `?${fallbackQ.toString()}` : '');
    if (fallback && fallback.length > 0) {
      const nearestDate = fallback
        .map(s => s.travelDate)
        .sort((a, b) => Math.abs(new Date(a) - new Date(date)) - Math.abs(new Date(b) - new Date(date)))[0];
      searchStatusEl.textContent =
        `No buses found for ${date}. Showing available schedules. Nearest date: ${nearestDate}.`;
      searchStatusEl.className = 'summary status-err';
      return;
    }
  }

  searchStatusEl.textContent = `${schedules.length} schedule(s) found.`;
  searchStatusEl.className = 'summary status-ok';
});

todayDateBtn.addEventListener('click', () => setJourneyDate(0));
tomorrowDateBtn.addEventListener('click', () => setJourneyDate(1));
travelDateEl.addEventListener('change', refreshJourneyDateUI);
dateContentEl.addEventListener('click', () => {
  if (typeof travelDateEl.showPicker === 'function') {
    travelDateEl.showPicker();
    return;
  }
  travelDateEl.focus();
});

if (!travelDateEl.value) {
  setJourneyDate(0);
} else {
  refreshJourneyDateUI();
}

updateAuthStatus();
loadRoutes();
loadTourismRoutes();
loadSchedules();
