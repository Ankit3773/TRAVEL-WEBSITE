const APP_CONFIG = window.NARAYAN_TRAVELS_CONFIG || {};
const API_BASE_URL = normalizeBaseUrl(APP_CONFIG.apiBaseUrl || '');

const elements = {
  loginForm: document.getElementById('admin-login-form'),
  loginEmail: document.getElementById('admin-login-email'),
  loginPassword: document.getElementById('admin-login-password'),
  authSummary: document.getElementById('admin-auth-summary'),
  accessTitle: document.getElementById('admin-access-title'),
  accessCopy: document.getElementById('admin-access-copy'),
  accessStatus: document.getElementById('admin-access-status'),
  logoutBtn: document.getElementById('admin-logout-btn'),
  console: document.getElementById('admin-console'),
  metrics: document.getElementById('admin-metrics'),
  trendRange: document.getElementById('admin-trend-range'),
  trendSummary: document.getElementById('admin-trend-summary'),
  trendChart: document.getElementById('admin-trend-chart'),
  trendsRefresh: document.getElementById('admin-trends-refresh'),
  routeForm: document.getElementById('admin-route-form'),
  routeId: document.getElementById('admin-route-id'),
  routeSource: document.getElementById('admin-route-source'),
  routeDestination: document.getElementById('admin-route-destination'),
  routeDistance: document.getElementById('admin-route-distance'),
  routeTourism: document.getElementById('admin-route-tourism'),
  routeSubmit: document.getElementById('admin-route-submit'),
  routeCancel: document.getElementById('admin-route-cancel'),
  routeStatus: document.getElementById('admin-route-status'),
  routeList: document.getElementById('admin-route-list'),
  routesRefresh: document.getElementById('admin-routes-refresh'),
  busForm: document.getElementById('admin-bus-form'),
  busId: document.getElementById('admin-bus-id'),
  busNumber: document.getElementById('admin-bus-number'),
  busType: document.getElementById('admin-bus-type'),
  busSeats: document.getElementById('admin-bus-seats'),
  busSubmit: document.getElementById('admin-bus-submit'),
  busCancel: document.getElementById('admin-bus-cancel'),
  busStatus: document.getElementById('admin-bus-status'),
  busList: document.getElementById('admin-bus-list'),
  busesRefresh: document.getElementById('admin-buses-refresh'),
  scheduleForm: document.getElementById('admin-schedule-form'),
  scheduleId: document.getElementById('admin-schedule-id'),
  scheduleRouteId: document.getElementById('admin-sch-route-id'),
  scheduleBusId: document.getElementById('admin-sch-bus-id'),
  scheduleDate: document.getElementById('admin-sch-date'),
  scheduleDeparture: document.getElementById('admin-sch-departure'),
  scheduleArrival: document.getElementById('admin-sch-arrival'),
  scheduleFare: document.getElementById('admin-sch-fare'),
  scheduleSubmit: document.getElementById('admin-schedule-submit'),
  scheduleCancel: document.getElementById('admin-schedule-cancel'),
  scheduleStatus: document.getElementById('admin-schedule-status'),
  scheduleList: document.getElementById('admin-schedule-list'),
  schedulesRefresh: document.getElementById('admin-schedules-refresh'),
  bookingsStatus: document.getElementById('admin-bookings-status'),
  bookingsRefresh: document.getElementById('admin-bookings-refresh'),
  bookingsSummary: document.getElementById('admin-bookings-summary'),
  bookingsStatusText: document.getElementById('admin-bookings-status-text'),
  bookingsList: document.getElementById('admin-bookings'),
  bookingsPagination: document.getElementById('admin-bookings-pagination')
};

const state = {
  routes: [],
  buses: [],
  schedules: [],
  bookingsPage: {
    status: '',
    page: 0,
    size: 8
  },
  trendRangeDays: 7
};

function normalizeBaseUrl(value) {
  if (!value) return '';
  return value.endsWith('/') ? value.slice(0, -1) : value;
}

function resolveApiUrl(path) {
  if (!path) return path;
  if (/^https?:\/\//i.test(path)) return path;
  if (!path.startsWith('/')) return path;
  return `${API_BASE_URL}${path}`;
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

function getToken() {
  return localStorage.getItem('authToken');
}

function isAdmin() {
  return currentUser()?.role === 'ADMIN';
}

function setAuth(data) {
  localStorage.setItem('authToken', data.token);
  localStorage.setItem('authUser', JSON.stringify(data));
}

function clearAuth() {
  localStorage.removeItem('authToken');
  localStorage.removeItem('authUser');
}

async function api(url, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (options.body) headers['Content-Type'] = 'application/json';
  if (options.auth && getToken()) headers.Authorization = `Bearer ${getToken()}`;

  const response = await fetch(resolveApiUrl(url), { ...options, headers });
  const isJson = response.headers.get('content-type')?.includes('application/json');
  const body = isJson ? await response.json() : null;
  if (!response.ok) throw new Error(body?.error || 'Request failed');
  return body;
}

function setSummary(element, message, tone = 'muted') {
  if (!element) return;
  element.textContent = message;
  element.className = 'summary';
  if (tone === 'ok') {
    element.classList.add('status-ok');
  } else if (tone === 'err') {
    element.classList.add('status-err');
  } else {
    element.classList.add('muted-summary');
  }
}

function showBookingsStatus(message, tone = 'muted') {
  setSummary(elements.bookingsStatusText, message, tone);
  elements.bookingsStatusText.classList.toggle('hidden', !message);
}

function formatAmount(amount) {
  if (amount == null) return '-';
  return `Rs ${Number(amount).toFixed(0)}`;
}

function formatDisplayDate(isoDate) {
  if (!isoDate) return '-';
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(year, month - 1, day).toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric'
  });
}

function formatTime(timeValue) {
  if (!timeValue) return '-';
  return timeValue.slice(0, 5);
}

function formatDateTime(dateTimeValue) {
  if (!dateTimeValue) return '-';
  const date = new Date(dateTimeValue);
  if (Number.isNaN(date.getTime())) return '-';
  return date.toLocaleString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function normalizeBookingStatus(status) {
  return status === 'CONFIRMED' ? 'BOOKED' : status;
}

function formatSeatNumbers(booking) {
  if (Array.isArray(booking.seatNumbers) && booking.seatNumbers.length) {
    return booking.seatNumbers.join(', ');
  }
  return booking.seatNumber ?? '-';
}

function statusFilterLabel(status) {
  return status ? normalizeBookingStatus(status) : 'ALL';
}

function toIsoDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function renderPagination(container, pageData, onPageChange) {
  if (!container || !pageData || pageData.totalPages <= 1) {
    container.innerHTML = '';
    container.classList.add('hidden');
    return;
  }

  const current = pageData.page || 0;
  const totalPages = pageData.totalPages || 0;
  const start = Math.max(0, current - 1);
  const end = Math.min(totalPages - 1, current + 1);
  const pageButtons = [];

  for (let page = start; page <= end; page += 1) {
    pageButtons.push(`
      <button type="button" class="page-btn${page === current ? ' active' : ''}" data-page="${page}">
        ${page + 1}
      </button>
    `);
  }

  container.innerHTML = `
    <div class="pagination-copy">Page ${current + 1} of ${totalPages}</div>
    <div class="pagination-actions">
      <button type="button" class="page-btn" data-page="${current - 1}" ${pageData.first ? 'disabled' : ''}>Prev</button>
      ${pageButtons.join('')}
      <button type="button" class="page-btn" data-page="${current + 1}" ${pageData.last ? 'disabled' : ''}>Next</button>
    </div>
  `;
  container.classList.remove('hidden');

  container.querySelectorAll('[data-page]').forEach(button => {
    button.addEventListener('click', () => {
      const nextPage = Number(button.dataset.page);
      if (Number.isNaN(nextPage) || nextPage < 0 || nextPage >= totalPages || nextPage === current) return;
      onPageChange(nextPage);
    });
  });
}

function resetRouteForm() {
  elements.routeForm.reset();
  elements.routeId.value = '';
  elements.routeSubmit.textContent = 'Create route';
  elements.routeCancel.classList.add('hidden');
  setSummary(elements.routeStatus, 'Create a new route or edit an existing one.');
}

function editRoute(route) {
  elements.routeId.value = String(route.id);
  elements.routeSource.value = route.source;
  elements.routeDestination.value = route.destination;
  elements.routeDistance.value = String(route.distanceKm);
  elements.routeTourism.checked = Boolean(route.tourismRoute);
  elements.routeSubmit.textContent = 'Update route';
  elements.routeCancel.classList.remove('hidden');
  setSummary(elements.routeStatus, `Editing route #${route.id}.`);
  elements.routeForm.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function resetBusForm() {
  elements.busForm.reset();
  elements.busId.value = '';
  elements.busSubmit.textContent = 'Create bus';
  elements.busCancel.classList.add('hidden');
  setSummary(elements.busStatus, 'Create a new bus or edit an existing one.');
}

function editBus(bus) {
  elements.busId.value = String(bus.id);
  elements.busNumber.value = bus.busNumber;
  elements.busType.value = bus.busType;
  elements.busSeats.value = String(bus.totalSeats);
  elements.busSubmit.textContent = 'Update bus';
  elements.busCancel.classList.remove('hidden');
  setSummary(elements.busStatus, `Editing bus #${bus.id}.`);
  elements.busForm.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function setSelectOptions(select, items, labelBuilder, selectedId = '') {
  if (!select) return;
  const nextValue = selectedId || select.value;
  if (!items.length) {
    select.innerHTML = '<option value="">No options available</option>';
    select.value = '';
    return;
  }

  select.innerHTML = items
    .map(item => `<option value="${item.id}">${labelBuilder(item)}</option>`)
    .join('');

  const match = items.some(item => String(item.id) === String(nextValue));
  select.value = match ? String(nextValue) : String(items[0].id);
}

function refreshScheduleSelectors(selectedRouteId = '', selectedBusId = '') {
  setSelectOptions(
    elements.scheduleRouteId,
    state.routes.filter(route => route.active),
    route => `${route.id} | ${route.source} -> ${route.destination}${route.tourismRoute ? ' | Tourism' : ''}`,
    selectedRouteId
  );
  setSelectOptions(
    elements.scheduleBusId,
    state.buses.filter(bus => bus.active),
    bus => `${bus.id} | ${bus.busNumber} | ${bus.busType} | ${bus.totalSeats} seats`,
    selectedBusId
  );
}

function resetScheduleForm() {
  elements.scheduleForm.reset();
  elements.scheduleId.value = '';
  elements.scheduleSubmit.textContent = 'Create schedule';
  elements.scheduleCancel.classList.add('hidden');
  refreshScheduleSelectors();
  setSummary(elements.scheduleStatus, 'Leave fare blank to use the automatic route-based fare.');
}

function editSchedule(schedule) {
  refreshScheduleSelectors(schedule.route.id, schedule.bus.id);
  elements.scheduleId.value = String(schedule.id);
  elements.scheduleDate.value = schedule.travelDate;
  elements.scheduleDeparture.value = formatTime(schedule.departureTime);
  elements.scheduleArrival.value = formatTime(schedule.arrivalTime);
  elements.scheduleFare.value = schedule.baseFare ?? '';
  elements.scheduleSubmit.textContent = 'Update schedule';
  elements.scheduleCancel.classList.remove('hidden');
  setSummary(elements.scheduleStatus, `Editing schedule #${schedule.id}.`);
  elements.scheduleForm.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function renderMetrics(metrics) {
  elements.metrics.innerHTML = `
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

async function loadMetrics() {
  try {
    renderMetrics(await api('/api/admin/metrics', { auth: true }));
  } catch (err) {
    elements.metrics.innerHTML = `<div class="item muted">${err.message}</div>`;
  }
}

function renderTrends(trend) {
  const items = trend?.items || [];
  if (!items.length) {
    elements.trendSummary.innerHTML = '<div class="item muted">No booking trend data is available for the selected date range.</div>';
    elements.trendChart.innerHTML = '';
    return;
  }

  const maxVolume = Math.max(...items.map(point => point.confirmedBookings + point.cancelledBookings), 1);
  elements.trendSummary.innerHTML = `
    <div class="metric panel-metric">
      <div class="label">Window</div>
      <div class="value">${trend.fromDate} to ${trend.toDate}</div>
    </div>
    <div class="metric panel-metric">
      <div class="label">Booked</div>
      <div class="value">${trend.totalConfirmedBookings}</div>
    </div>
    <div class="metric panel-metric">
      <div class="label">Cancelled</div>
      <div class="value">${trend.totalCancelledBookings}</div>
    </div>
    <div class="metric panel-metric">
      <div class="label">Revenue</div>
      <div class="value">${formatAmount(trend.totalConfirmedRevenue)}</div>
    </div>
  `;

  elements.trendChart.innerHTML = items.map(point => {
    const confirmedWidth = ((point.confirmedBookings / maxVolume) * 100).toFixed(1);
    const cancelledWidth = ((point.cancelledBookings / maxVolume) * 100).toFixed(1);
    return `
      <div class="trend-row">
        <div class="trend-date">${formatDisplayDate(point.date)}</div>
        <div class="trend-bar-track">
          <span class="trend-bar confirmed" style="width:${confirmedWidth}%"></span>
          <span class="trend-bar cancelled" style="width:${cancelledWidth}%"></span>
        </div>
        <div class="trend-values">
          <span>B ${point.confirmedBookings}</span>
          <span>C ${point.cancelledBookings}</span>
          <strong>${formatAmount(point.confirmedRevenue)}</strong>
        </div>
      </div>
    `;
  }).join('');
}

async function loadTrends() {
  state.trendRangeDays = Number(elements.trendRange.value || state.trendRangeDays || 7);
  const toDate = new Date();
  toDate.setHours(0, 0, 0, 0);
  const fromDate = new Date(toDate);
  fromDate.setDate(fromDate.getDate() - (state.trendRangeDays - 1));
  const params = new URLSearchParams({
    fromDate: toIsoDate(fromDate),
    toDate: toIsoDate(toDate)
  });

  try {
    renderTrends(await api(`/api/admin/metrics/trends?${params.toString()}`, { auth: true }));
  } catch (err) {
    elements.trendSummary.innerHTML = `<div class="item muted">${err.message}</div>`;
    elements.trendChart.innerHTML = '';
  }
}

function renderRouteList() {
  if (!state.routes.length) {
    elements.routeList.innerHTML = '<div class="item muted">No routes found.</div>';
    return;
  }

  elements.routeList.innerHTML = state.routes.map(route => `
    <article class="item manager-item">
      <div class="row">
        <strong>${route.source} -> ${route.destination}</strong>
        <span class="info-pill">${route.active ? 'ACTIVE' : 'INACTIVE'}</span>
      </div>
      <div class="row muted">
        <span>#${route.id} | ${route.distanceKm} km</span>
        <span>${route.tourismRoute ? 'Tourism route' : 'Regular route'}</span>
      </div>
      <div class="row manager-actions">
        <button type="button" data-edit-route="${route.id}">Edit</button>
        <button type="button" class="secondary" data-delete-route="${route.id}" ${route.active ? '' : 'disabled'}>Deactivate</button>
      </div>
    </article>
  `).join('');

  elements.routeList.querySelectorAll('[data-edit-route]').forEach(button => {
    button.addEventListener('click', () => {
      const route = state.routes.find(item => item.id === Number(button.dataset.editRoute));
      if (route) editRoute(route);
    });
  });

  elements.routeList.querySelectorAll('[data-delete-route]').forEach(button => {
    button.addEventListener('click', async () => {
      const routeId = Number(button.dataset.deleteRoute);
      if (!window.confirm(`Deactivate route #${routeId}? Related active schedules will also be deactivated.`)) return;

      try {
        await api(`/api/admin/routes/${routeId}`, { method: 'DELETE', auth: true });
        setSummary(elements.routeStatus, `Route #${routeId} deactivated.`, 'ok');
        await refreshAdminData({ keepBookingPage: true });
      } catch (err) {
        setSummary(elements.routeStatus, err.message, 'err');
      }
    });
  });
}

async function loadRoutes() {
  try {
    state.routes = await api('/api/admin/routes?activeOnly=false', { auth: true });
    renderRouteList();
    refreshScheduleSelectors();
  } catch (err) {
    elements.routeList.innerHTML = `<div class="item muted">${err.message}</div>`;
  }
}

function renderBusList() {
  if (!state.buses.length) {
    elements.busList.innerHTML = '<div class="item muted">No buses found.</div>';
    return;
  }

  elements.busList.innerHTML = state.buses.map(bus => `
    <article class="item manager-item">
      <div class="row">
        <strong>${bus.busNumber}</strong>
        <span class="info-pill">${bus.active ? 'ACTIVE' : 'INACTIVE'}</span>
      </div>
      <div class="row muted">
        <span>#${bus.id} | ${bus.busType}</span>
        <span>${bus.totalSeats} seats</span>
      </div>
      <div class="row manager-actions">
        <button type="button" data-edit-bus="${bus.id}">Edit</button>
        <button type="button" class="secondary" data-delete-bus="${bus.id}" ${bus.active ? '' : 'disabled'}>Deactivate</button>
      </div>
    </article>
  `).join('');

  elements.busList.querySelectorAll('[data-edit-bus]').forEach(button => {
    button.addEventListener('click', () => {
      const bus = state.buses.find(item => item.id === Number(button.dataset.editBus));
      if (bus) editBus(bus);
    });
  });

  elements.busList.querySelectorAll('[data-delete-bus]').forEach(button => {
    button.addEventListener('click', async () => {
      const busId = Number(button.dataset.deleteBus);
      if (!window.confirm(`Deactivate bus #${busId}? Related active schedules will also be deactivated.`)) return;

      try {
        await api(`/api/admin/buses/${busId}`, { method: 'DELETE', auth: true });
        setSummary(elements.busStatus, `Bus #${busId} deactivated.`, 'ok');
        await refreshAdminData({ keepBookingPage: true });
      } catch (err) {
        setSummary(elements.busStatus, err.message, 'err');
      }
    });
  });
}

async function loadBuses() {
  try {
    state.buses = await api('/api/admin/buses?activeOnly=false', { auth: true });
    renderBusList();
    refreshScheduleSelectors();
  } catch (err) {
    elements.busList.innerHTML = `<div class="item muted">${err.message}</div>`;
  }
}

function renderScheduleList() {
  if (!state.schedules.length) {
    elements.scheduleList.innerHTML = '<div class="item muted">No schedules found.</div>';
    return;
  }

  elements.scheduleList.innerHTML = state.schedules.map(schedule => `
    <article class="item manager-item">
      <div class="row">
        <strong>${schedule.route.source} -> ${schedule.route.destination}</strong>
        <span class="info-pill">${schedule.active ? 'ACTIVE' : 'INACTIVE'}</span>
      </div>
      <div class="row muted">
        <span>#${schedule.id} | ${schedule.bus.busNumber} | ${schedule.bus.busType}</span>
        <span>${formatDisplayDate(schedule.travelDate)} | ${formatTime(schedule.departureTime)} - ${formatTime(schedule.arrivalTime)}</span>
      </div>
      <div class="row muted">
        <span>${schedule.route.distanceKm} km</span>
        <span>Fare ${formatAmount(schedule.baseFare)}</span>
      </div>
      <div class="row manager-actions">
        <button type="button" data-edit-schedule="${schedule.id}">Edit fare</button>
        <button type="button" class="secondary" data-delete-schedule="${schedule.id}" ${schedule.active ? '' : 'disabled'}>Deactivate</button>
      </div>
    </article>
  `).join('');

  elements.scheduleList.querySelectorAll('[data-edit-schedule]').forEach(button => {
    button.addEventListener('click', () => {
      const schedule = state.schedules.find(item => item.id === Number(button.dataset.editSchedule));
      if (schedule) editSchedule(schedule);
    });
  });

  elements.scheduleList.querySelectorAll('[data-delete-schedule]').forEach(button => {
    button.addEventListener('click', async () => {
      const scheduleId = Number(button.dataset.deleteSchedule);
      if (!window.confirm(`Deactivate schedule #${scheduleId}?`)) return;

      try {
        await api(`/api/admin/schedules/${scheduleId}`, { method: 'DELETE', auth: true });
        setSummary(elements.scheduleStatus, `Schedule #${scheduleId} deactivated.`, 'ok');
        await refreshAdminData({ keepBookingPage: true });
      } catch (err) {
        setSummary(elements.scheduleStatus, err.message, 'err');
      }
    });
  });
}

async function loadSchedules() {
  try {
    state.schedules = await api('/api/admin/schedules?activeOnly=false', { auth: true });
    renderScheduleList();
  } catch (err) {
    elements.scheduleList.innerHTML = `<div class="item muted">${err.message}</div>`;
  }
}

function renderBookings(pageData) {
  const items = pageData?.items || [];
  const statusLabel = statusFilterLabel(state.bookingsPage.status);
  const startIndex = items.length ? state.bookingsPage.page * state.bookingsPage.size + 1 : 0;
  const endIndex = items.length ? startIndex + items.length - 1 : 0;

  if (!items.length) {
    elements.bookingsSummary.textContent = `No bookings found for ${statusLabel}.`;
    elements.bookingsList.innerHTML = '<div class="item muted">No bookings matched the current admin filter.</div>';
    renderPagination(elements.bookingsPagination, null, () => {});
    return;
  }

  elements.bookingsSummary.textContent =
    `Showing ${startIndex}-${endIndex} of ${pageData.totalElements} booking(s) | Filter: ${statusLabel}`;

  elements.bookingsList.innerHTML = items.map(booking => `
    <div class="item">
      <div class="row">
        <strong>${booking.source} -> ${booking.destination}</strong>
        <span class="info-pill">${normalizeBookingStatus(booking.bookingStatus)} | ${booking.paymentStatus || 'PENDING'}</span>
      </div>
      <div class="row muted">
        <span>#${booking.bookingId} | ${booking.busNumber} | Seat(s) ${formatSeatNumbers(booking)}</span>
        <span>${booking.bookedByEmail || 'Unknown customer'}</span>
      </div>
      <div class="row muted">
        <span>${formatDisplayDate(booking.travelDate)} ${formatTime(booking.departureTime)} | Booked ${formatDateTime(booking.bookedAt)}</span>
        <span>${formatAmount(booking.amount)}</span>
      </div>
      <div class="row muted">
        <span>${booking.paymentMode} / ${booking.paymentStatus || 'PENDING'}${booking.paymentReference ? ` / Ref ${booking.paymentReference}` : ''}</span>
        <span>${booking.cancelledAt ? `Cancelled ${formatDateTime(booking.cancelledAt)}` : 'Active booking record'}</span>
      </div>
      <div class="row manager-actions">
        <span class="muted">Passenger ${booking.passengerName}</span>
        ${normalizeBookingStatus(booking.bookingStatus) === 'BOOKED'
          ? `<button type="button" class="secondary" data-cancel-booking="${booking.bookingId}">Cancel booking</button>`
          : '<span class="muted">No admin action required</span>'}
      </div>
    </div>
  `).join('');

  renderPagination(elements.bookingsPagination, pageData, nextPage => {
    state.bookingsPage.page = nextPage;
    loadBookings();
  });

  elements.bookingsList.querySelectorAll('[data-cancel-booking]').forEach(button => {
    button.addEventListener('click', async () => {
      const bookingId = Number(button.dataset.cancelBooking);
      if (!window.confirm(`Cancel booking #${bookingId}?`)) return;

      try {
        showBookingsStatus(`Cancelling booking #${bookingId}...`);
        await api(`/api/bookings/${bookingId}`, { method: 'DELETE', auth: true });
        showBookingsStatus(`Booking #${bookingId} cancelled.`, 'ok');
        await Promise.all([loadBookings(), loadMetrics(), loadTrends()]);
      } catch (err) {
        showBookingsStatus(err.message, 'err');
      }
    });
  });
}

async function loadBookings(options = {}) {
  if (options.resetPage) {
    state.bookingsPage.page = 0;
  }

  state.bookingsPage.status = elements.bookingsStatus.value || '';
  const params = new URLSearchParams({
    page: String(state.bookingsPage.page),
    size: String(state.bookingsPage.size)
  });
  if (state.bookingsPage.status) {
    params.set('status', state.bookingsPage.status);
  }

  try {
    const pageData = await api(`/api/admin/bookings/paged?${params.toString()}`, { auth: true });
    if (pageData.totalPages > 0 && !pageData.items.length && state.bookingsPage.page >= pageData.totalPages) {
      state.bookingsPage.page = pageData.totalPages - 1;
      return loadBookings();
    }
    renderBookings(pageData);
  } catch (err) {
    elements.bookingsSummary.textContent = 'Could not fetch admin bookings.';
    elements.bookingsList.innerHTML = `<div class="item muted">${err.message}</div>`;
    renderPagination(elements.bookingsPagination, null, () => {});
  }
}

async function refreshAdminData(options = {}) {
  if (!isAdmin()) return;
  await Promise.all([
    loadMetrics(),
    loadTrends(),
    loadRoutes(),
    loadBuses(),
    loadSchedules(),
    loadBookings(options.keepBookingPage ? {} : { resetPage: true })
  ]);
}

function renderAccessState() {
  const user = currentUser();
  const loggedIn = Boolean(user);

  elements.logoutBtn.classList.toggle('hidden', !loggedIn);

  if (!user) {
    elements.authSummary.textContent = 'Not logged in.';
    elements.accessTitle.textContent = 'Console locked';
    elements.accessCopy.textContent = 'Login with an admin account to manage routes, bookings, buses, schedules, and fares.';
    setSummary(elements.accessStatus, 'Waiting for admin authentication.');
    elements.console.classList.add('hidden');
    elements.loginForm.classList.remove('hidden');
    return;
  }

  elements.authSummary.textContent = `Logged in as ${user.name} (${user.role})`;

  if (!isAdmin()) {
    elements.accessTitle.textContent = 'Admin role required';
    elements.accessCopy.textContent = 'This account is logged in, but it does not have admin access. Use an admin account to unlock the console.';
    setSummary(elements.accessStatus, 'Current session does not have admin privileges.', 'err');
    elements.console.classList.add('hidden');
    elements.loginForm.classList.remove('hidden');
    return;
  }

  elements.accessTitle.textContent = 'Console unlocked';
  elements.accessCopy.textContent = 'Routes, buses, schedules, fares, and bookings can now be managed from this page.';
  setSummary(elements.accessStatus, 'Admin access confirmed.', 'ok');
  elements.console.classList.remove('hidden');
  elements.loginForm.classList.add('hidden');
}

async function syncAccessState() {
  renderAccessState();
  if (isAdmin()) {
    await refreshAdminData();
    resetRouteForm();
    resetBusForm();
    resetScheduleForm();
  }
}

elements.loginForm.addEventListener('submit', async event => {
  event.preventDefault();
  try {
    const payload = {
      email: elements.loginEmail.value.trim(),
      password: elements.loginPassword.value
    };
    const data = await api('/api/auth/login', { method: 'POST', body: JSON.stringify(payload) });
    setAuth(data);
    elements.loginForm.reset();
    await syncAccessState();
  } catch (err) {
    setSummary(elements.accessStatus, err.message, 'err');
  }
});

elements.logoutBtn.addEventListener('click', async () => {
  clearAuth();
  showBookingsStatus('');
  resetRouteForm();
  resetBusForm();
  resetScheduleForm();
  await syncAccessState();
});

elements.trendRange.addEventListener('change', () => {
  if (isAdmin()) loadTrends();
});

elements.trendsRefresh.addEventListener('click', () => {
  if (isAdmin()) loadTrends();
});

elements.routesRefresh.addEventListener('click', () => {
  if (isAdmin()) loadRoutes();
});

elements.busesRefresh.addEventListener('click', () => {
  if (isAdmin()) loadBuses();
});

elements.schedulesRefresh.addEventListener('click', () => {
  if (isAdmin()) loadSchedules();
});

elements.bookingsStatus.addEventListener('change', () => {
  if (isAdmin()) loadBookings({ resetPage: true });
});

elements.bookingsRefresh.addEventListener('click', () => {
  if (isAdmin()) loadBookings();
});

elements.routeCancel.addEventListener('click', () => {
  resetRouteForm();
});

elements.busCancel.addEventListener('click', () => {
  resetBusForm();
});

elements.scheduleCancel.addEventListener('click', () => {
  resetScheduleForm();
});

elements.routeForm.addEventListener('submit', async event => {
  event.preventDefault();
  try {
    const routeId = elements.routeId.value.trim();
    const payload = {
      source: elements.routeSource.value.trim(),
      destination: elements.routeDestination.value.trim(),
      distanceKm: Number(elements.routeDistance.value),
      tourismRoute: elements.routeTourism.checked
    };

    if (routeId) {
      await api(`/api/admin/routes/${routeId}`, { method: 'PUT', body: JSON.stringify(payload), auth: true });
      resetRouteForm();
      setSummary(elements.routeStatus, `Route #${routeId} updated.`, 'ok');
    } else {
      await api('/api/admin/routes', { method: 'POST', body: JSON.stringify(payload), auth: true });
      resetRouteForm();
      setSummary(elements.routeStatus, 'Route created successfully.', 'ok');
    }

    await Promise.all([loadRoutes(), loadSchedules(), loadMetrics()]);
  } catch (err) {
    setSummary(elements.routeStatus, err.message, 'err');
  }
});

elements.busForm.addEventListener('submit', async event => {
  event.preventDefault();
  try {
    const busId = elements.busId.value.trim();
    const payload = {
      busNumber: elements.busNumber.value.trim(),
      busType: elements.busType.value,
      totalSeats: Number(elements.busSeats.value)
    };

    if (busId) {
      await api(`/api/admin/buses/${busId}`, { method: 'PUT', body: JSON.stringify(payload), auth: true });
      resetBusForm();
      setSummary(elements.busStatus, `Bus #${busId} updated.`, 'ok');
    } else {
      await api('/api/admin/buses', { method: 'POST', body: JSON.stringify(payload), auth: true });
      resetBusForm();
      setSummary(elements.busStatus, 'Bus created successfully.', 'ok');
    }

    await Promise.all([loadBuses(), loadSchedules(), loadMetrics()]);
  } catch (err) {
    setSummary(elements.busStatus, err.message, 'err');
  }
});

elements.scheduleForm.addEventListener('submit', async event => {
  event.preventDefault();
  try {
    const scheduleId = elements.scheduleId.value.trim();
    const fareValue = elements.scheduleFare.value.trim();
    const payload = {
      routeId: Number(elements.scheduleRouteId.value),
      busId: Number(elements.scheduleBusId.value),
      travelDate: elements.scheduleDate.value,
      departureTime: `${elements.scheduleDeparture.value}:00`,
      arrivalTime: `${elements.scheduleArrival.value}:00`,
      baseFare: fareValue ? Number(fareValue) : null
    };

    if (scheduleId) {
      await api(`/api/admin/schedules/${scheduleId}`, { method: 'PUT', body: JSON.stringify(payload), auth: true });
      resetScheduleForm();
      setSummary(elements.scheduleStatus, `Schedule #${scheduleId} updated.`, 'ok');
    } else {
      await api('/api/admin/schedules', { method: 'POST', body: JSON.stringify(payload), auth: true });
      resetScheduleForm();
      setSummary(elements.scheduleStatus, 'Schedule created successfully.', 'ok');
    }

    await Promise.all([loadSchedules(), loadMetrics(), loadTrends()]);
  } catch (err) {
    setSummary(elements.scheduleStatus, err.message, 'err');
  }
});

syncAccessState();
