const STEP_ORDER = ['search', 'buses', 'seats', 'payment', 'confirmation'];
const POLL_INTERVAL_MS = 10000;
const APP_CONFIG = window.NARAYAN_TRAVELS_CONFIG || {};
const API_BASE_URL = normalizeBaseUrl(APP_CONFIG.apiBaseUrl || '');
const PAGE_MODE = document.body.dataset.page || 'home';
const RESULTS_PAGE_PATH = '/results.html';

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
  accountMenuBtn: document.getElementById('account-menu-btn'),
  accountMenu: document.getElementById('account-menu'),
  accountPanelTabs: Array.from(document.querySelectorAll('[data-account-panel-tab]')),
  accountPanels: Array.from(document.querySelectorAll('[data-account-panel]')),
  guestOnlyBlocks: Array.from(document.querySelectorAll('.auth-guest-only')),
  userOnlyBlocks: Array.from(document.querySelectorAll('.auth-user-only')),
  accountAdminLink: document.getElementById('account-admin-link'),
  googleAuthBlock: document.getElementById('google-auth-block'),
  googleSigninButton: document.getElementById('google-signin-button'),
  googleAuthStatus: document.getElementById('google-auth-status'),
  forgotPanel: document.getElementById('forgot-password-panel'),
  forgotStatus: document.getElementById('forgot-status'),
  bookingForm: document.getElementById('booking-form'),
  bookingStatus: document.getElementById('booking-status'),
  myBookings: document.getElementById('my-bookings'),
  myBookingsSummary: document.getElementById('my-bookings-summary'),
  myBookingsPagination: document.getElementById('my-bookings-pagination'),
  myBookingsStatus: document.getElementById('my-bookings-status'),
  myBookingsRefresh: document.getElementById('my-bookings-refresh'),
  adminPanel: document.getElementById('admin-panel'),
  adminStatus: document.getElementById('admin-status'),
  adminBookings: document.getElementById('admin-bookings'),
  adminBookingsSummary: document.getElementById('admin-bookings-summary'),
  adminBookingsPagination: document.getElementById('admin-bookings-pagination'),
  adminBookingsStatus: document.getElementById('admin-bookings-status'),
  adminBookingsRefresh: document.getElementById('admin-bookings-refresh'),
  adminMetrics: document.getElementById('admin-metrics'),
  adminTrendRange: document.getElementById('admin-trend-range'),
  adminTrendSummary: document.getElementById('admin-trend-summary'),
  adminTrendChart: document.getElementById('admin-trend-chart'),
  adminTrendsRefresh: document.getElementById('admin-trends-refresh'),
  adminMonitoring: document.getElementById('admin-monitoring'),
  adminMonitorRefresh: document.getElementById('admin-monitor-refresh'),
  travelDate: document.getElementById('travel-date'),
  journeyDateText: document.getElementById('journey-date-text'),
  journeyDayHint: document.getElementById('journey-day-hint'),
  todayDateBtn: document.getElementById('date-today-btn'),
  tomorrowDateBtn: document.getElementById('date-tomorrow-btn'),
  dateContent: document.querySelector('.date-content'),
  stepper: document.getElementById('stepper'),
  flowStatus: document.getElementById('flow-status'),
  busListCopy: document.getElementById('bus-list-copy'),
  busResultsRoute: document.getElementById('bus-results-route'),
  busResultsDate: document.getElementById('bus-results-date'),
  busResultsMeta: document.getElementById('bus-results-meta'),
  busResultsStatus: document.getElementById('bus-results-status'),
  journeySummary: document.getElementById('journey-summary'),
  paymentSummary: document.getElementById('payment-summary'),
  paymentMethodTitle: document.getElementById('payment-method-title'),
  paymentModeBadge: document.getElementById('payment-mode-badge'),
  paymentGatewayBadge: document.getElementById('payment-gateway-badge'),
  paymentStageBadge: document.getElementById('payment-stage-badge'),
  paymentMethodSummary: document.getElementById('payment-method-summary'),
  paymentTimeline: document.getElementById('payment-timeline'),
  paymentSessionPanel: document.getElementById('payment-session-panel'),
  paymentSessionId: document.getElementById('payment-session-id'),
  paymentSessionAmount: document.getElementById('payment-session-amount'),
  paymentSessionExpiry: document.getElementById('payment-session-expiry'),
  paymentActionTitle: document.getElementById('payment-action-title'),
  paymentActionCopy: document.getElementById('payment-action-copy'),
  paymentIdField: document.getElementById('payment-id-field'),
  paymentIdLabel: document.getElementById('payment-id-label'),
  paymentIdHint: document.getElementById('payment-id-hint'),
  paymentIdInput: document.getElementById('payment-id-input'),
  paymentActionBtn: document.getElementById('payment-action-btn'),
  paymentStatus: document.getElementById('payment-status'),
  confirmationCard: document.getElementById('confirmation-card'),
  workspaceTabs: document.getElementById('workspace-tabs'),
  workspaceHint: document.getElementById('workspace-hint'),
  workspaceTabButtons: Array.from(document.querySelectorAll('[data-workspace-tab]')),
  workspacePanels: Array.from(document.querySelectorAll('[data-workspace-panel]')),
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
  seatPoller: null,
  myBookingsPage: {
    status: '',
    page: 0,
    size: 5
  },
  adminBookingsPage: {
    status: '',
    page: 0,
    size: 6
  },
  adminTrendRangeDays: 7,
  workspacePanel: 'routes',
  accountPanel: 'login',
  googleInitialized: false,
  googleInitAttempts: 0
};

function isResultsPage() {
  return PAGE_MODE === 'results';
}

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

function buildScheduleSearchParams({ source, destination, date }) {
  const params = new URLSearchParams();
  if (source) params.set('source', source);
  if (destination) params.set('destination', destination);
  if (date) params.set('date', date);
  return params;
}

function updateSearchInputs({ source, destination, date }) {
  const sourceInput = document.getElementById('source');
  const destinationInput = document.getElementById('destination');

  if (sourceInput) sourceInput.value = source || '';
  if (destinationInput) destinationInput.value = destination || '';
  if (date) {
    elements.travelDate.value = date;
  }
  refreshJourneyDateUI();
}

function searchCriteriaLabel({ source, destination, date }) {
  const routeLabel = source || destination
    ? `${source || 'Any source'} -> ${destination || 'Any destination'}`
    : 'All routes';
  const dateLabel = date ? formatDisplayDate(date) : 'Any date';
  return `${routeLabel} | ${dateLabel}`;
}

function renderBusResultsHeader({ source, destination, date }, result = null) {
  if (elements.busResultsRoute) {
    elements.busResultsRoute.textContent = source || destination
      ? `${source || 'Any source'} -> ${destination || 'Any destination'}`
      : 'All routes';
  }
  if (elements.busResultsDate) {
    elements.busResultsDate.textContent = date ? formatDisplayDate(date) : 'Any date';
  }
  if (elements.busResultsMeta) {
    if (!result) {
      elements.busResultsMeta.textContent = 'Loading matching buses and fare details.';
    } else if (result.schedules === null) {
      elements.busResultsMeta.textContent = 'Search could not be completed.';
    } else if (!result.schedules.length) {
      elements.busResultsMeta.textContent = 'No buses matched this route and date.';
    } else if (result.fallbackDate) {
      elements.busResultsMeta.textContent =
        `${result.schedules.length} bus option(s) found on the nearest available date ${formatDisplayDate(result.fallbackDate)}.`;
    } else {
      elements.busResultsMeta.textContent = `${result.schedules.length} bus option(s) available for this search.`;
    }
  }
}

function navigateToResultsPage({ source, destination, date }) {
  const params = buildScheduleSearchParams({ source, destination, date });
  const target = params.toString() ? `${RESULTS_PAGE_PATH}?${params.toString()}` : RESULTS_PAGE_PATH;
  window.location.assign(target);
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

function showGoogleAuthStatus(message, isError = false) {
  if (!elements.googleAuthStatus) return;
  if (!message) {
    elements.googleAuthStatus.textContent = '';
    elements.googleAuthStatus.className = 'summary google-auth-status hidden';
    return;
  }
  elements.googleAuthStatus.textContent = message;
  elements.googleAuthStatus.className = `summary google-auth-status ${isError ? 'status-err' : 'status-ok'}`;
}

async function handleGoogleCredentialResponse(response) {
  if (!response?.credential) {
    showGoogleAuthStatus('Google did not return a valid credential.', true);
    return;
  }

  showGoogleAuthStatus('Signing in with Google...');

  try {
    const data = await api('/api/auth/google', {
      method: 'POST',
      body: JSON.stringify({ credential: response.credential })
    });
    setAuth(data);
    showGoogleAuthStatus('Google sign-in successful.');
  } catch (err) {
    showGoogleAuthStatus(err.message, true);
  }
}

function initializeGoogleAuth() {
  if (!elements.googleAuthBlock || !elements.googleSigninButton || state.googleInitialized) return;

  const clientId = APP_CONFIG.googleClientId?.trim();
  if (!clientId) {
    elements.googleAuthBlock.classList.add('hidden');
    return;
  }

  if (!window.google?.accounts?.id) {
    state.googleInitAttempts += 1;
    if (state.googleInitAttempts <= 20) {
      window.setTimeout(initializeGoogleAuth, 250);
    }
    return;
  }

  window.google.accounts.id.initialize({
    client_id: clientId,
    callback: handleGoogleCredentialResponse,
    auto_select: false,
    cancel_on_tap_outside: true
  });

  elements.googleSigninButton.innerHTML = '';
  window.google.accounts.id.renderButton(elements.googleSigninButton, {
    theme: 'filled_blue',
    size: 'large',
    shape: 'rectangular',
    text: 'continue_with',
    width: 320
  });

  elements.googleAuthBlock.classList.remove('hidden');
  state.googleInitialized = true;
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

function setPaymentBadge(element, text, tone = 'muted') {
  if (!element) return;
  element.textContent = text;
  element.className = `payment-badge payment-badge-${tone}`;
}

function renderPaymentTimeline(items) {
  if (!elements.paymentTimeline) return;
  elements.paymentTimeline.innerHTML = items.map(item => `
    <div class="payment-timeline-item payment-timeline-${item.state}">
      <span class="payment-timeline-dot" aria-hidden="true"></span>
      <div>
        <strong>${item.title}</strong>
        <p>${item.copy}</p>
      </div>
    </div>
  `).join('');
}

function renderPaymentMethodState() {
  const pending = state.pendingTraveller;
  const draft = state.paymentDraft;

  if (!pending) {
    if (elements.paymentMethodTitle) {
      elements.paymentMethodTitle.textContent = 'Payment method';
    }
    setPaymentBadge(elements.paymentModeBadge, 'Not selected');
    setPaymentBadge(elements.paymentGatewayBadge, 'Gateway pending');
    setPaymentBadge(elements.paymentStageBadge, 'Awaiting traveller details');
    if (elements.paymentMethodSummary) {
      elements.paymentMethodSummary.textContent = 'Select a seat and add traveller details to unlock the payment step.';
    }
    if (elements.paymentSessionPanel) {
      elements.paymentSessionPanel.classList.add('hidden');
    }
    renderPaymentTimeline([
      {
        title: 'Choose seat',
        copy: 'Select a bus seat before moving to payment.',
        state: 'current'
      },
      {
        title: 'Add traveller details',
        copy: 'Passenger name, phone number, and payment mode appear here next.',
        state: 'upcoming'
      },
      {
        title: 'Confirm booking',
        copy: 'Pay on board or finish the mock online verification flow.',
        state: 'upcoming'
      }
    ]);
    return;
  }

  if (pending.paymentMode === 'ONLINE') {
    if (elements.paymentMethodTitle) {
      elements.paymentMethodTitle.textContent = 'Online payment';
    }
    setPaymentBadge(elements.paymentModeBadge, 'ONLINE', 'info');
    setPaymentBadge(elements.paymentGatewayBadge, draft?.paymentGateway || 'MOCK gateway', draft ? 'info' : 'muted');
    setPaymentBadge(elements.paymentStageBadge, draft ? 'Verification pending' : 'Checkout pending', draft ? 'warn' : 'info');
    if (elements.paymentMethodSummary) {
      elements.paymentMethodSummary.textContent = draft
        ? 'Your seat is locked and the mock checkout session is ready. Add the gateway payment ID to finalize the booking.'
        : 'This flow first locks the seat, then creates a mock checkout session, and finally verifies the payment reference.';
    }
    renderPaymentTimeline([
      {
        title: 'Traveller captured',
        copy: `Seat ${state.selectedSeat} saved for ${pending.passengerName}.`,
        state: 'complete'
      },
      {
        title: 'Checkout session',
        copy: draft ? `Session ${draft.paymentSessionId} created with ${draft.paymentGateway}.` : 'Create the session to hold the seat temporarily.',
        state: draft ? 'complete' : 'current'
      },
      {
        title: 'Verify payment ID',
        copy: draft ? 'Enter the mock payment ID to mark the booking as PAID.' : 'Available after the mock session is created.',
        state: draft ? 'current' : 'upcoming'
      }
    ]);
    if (elements.paymentSessionPanel) {
      elements.paymentSessionPanel.classList.toggle('hidden', !draft);
    }
    if (elements.paymentSessionId) {
      elements.paymentSessionId.textContent = draft?.paymentSessionId || '-';
    }
    if (elements.paymentSessionAmount) {
      elements.paymentSessionAmount.textContent = draft ? formatAmount(draft.amount) : '-';
    }
    if (elements.paymentSessionExpiry) {
      elements.paymentSessionExpiry.textContent = draft?.payableUntil ? formatDateTime(draft.payableUntil) : '-';
    }
    return;
  }

  if (elements.paymentMethodTitle) {
    elements.paymentMethodTitle.textContent = 'Pay on board';
  }
  setPaymentBadge(elements.paymentModeBadge, 'PAY_ON_BOARD', 'success');
  setPaymentBadge(elements.paymentGatewayBadge, 'No gateway required', 'success');
  setPaymentBadge(elements.paymentStageBadge, 'Ready to confirm', 'success');
  if (elements.paymentMethodSummary) {
    elements.paymentMethodSummary.textContent = 'The booking is confirmed now and the payment stays pending until the passenger boards.';
  }
  if (elements.paymentSessionPanel) {
    elements.paymentSessionPanel.classList.add('hidden');
  }
  renderPaymentTimeline([
    {
      title: 'Traveller captured',
      copy: `Seat ${state.selectedSeat} saved for ${pending.passengerName}.`,
      state: 'complete'
    },
    {
      title: 'Booking confirmation',
      copy: 'Confirm once to create the booking record immediately.',
      state: 'current'
    },
    {
      title: 'Collect fare on board',
      copy: 'The trip remains booked while payment status stays PENDING.',
      state: 'upcoming'
    }
  ]);
}

function statusFilterLabel(status) {
  return status ? normalizeBookingStatus(status) : 'ALL';
}

async function api(url, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (options.body) headers['Content-Type'] = 'application/json';
  if (options.auth && getToken()) headers.Authorization = `Bearer ${getToken()}`;

  const res = await fetch(resolveApiUrl(url), { ...options, headers });
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

function workspaceMessageFor(panel) {
  switch (panel) {
    case 'history':
      return 'Review bookings and filter by status.';
    case 'routes':
      return 'Browse active route coverage.';
    case 'tourism':
      return 'Check the tourism-focused AC circuits.';
    case 'admin':
      return 'Admin tools are grouped in one workspace tab.';
    default:
      return 'Use Account in the top right for login or signup.';
  }
}

function setAccountPanel(panel) {
  if (!elements.accountPanels.length) return;

  state.accountPanel = panel;
  elements.accountPanelTabs.forEach(button => {
    button.classList.toggle('active', button.dataset.accountPanelTab === panel);
  });
  elements.accountPanels.forEach(panelEl => {
    panelEl.classList.toggle('hidden', panelEl.dataset.accountPanel !== panel);
  });
}

function setAccountMenu(open) {
  if (!elements.accountMenu || !elements.accountMenuBtn) return;

  elements.accountMenu.classList.toggle('hidden', !open);
  elements.accountMenuBtn.setAttribute('aria-expanded', String(open));
}

function setWorkspacePanel(panel) {
  if (!elements.workspaceTabButtons.length || !elements.workspacePanels.length) return;

  const requestedTab = elements.workspaceTabButtons.find(
    tab => tab.dataset.workspaceTab === panel && !tab.classList.contains('hidden')
  );
  const fallbackTab = elements.workspaceTabButtons.find(tab => !tab.classList.contains('hidden'));
  const activeTabId = requestedTab?.dataset.workspaceTab || fallbackTab?.dataset.workspaceTab;

  if (!activeTabId) return;
  state.workspacePanel = activeTabId;

  elements.workspaceTabButtons.forEach(tab => {
    tab.classList.toggle('active', tab.dataset.workspaceTab === activeTabId);
  });
  elements.workspacePanels.forEach(panelEl => {
    panelEl.classList.toggle('workspace-hidden', panelEl.dataset.workspacePanel !== activeTabId);
  });
  if (elements.workspaceHint) {
    elements.workspaceHint.textContent = workspaceMessageFor(activeTabId);
  }
}

function refreshWorkspaceAccess() {
  if (!elements.workspaceTabButtons.length) return;

  const adminTab = elements.workspaceTabButtons.find(tab => tab.dataset.workspaceTab === 'admin');
  if (adminTab) {
    adminTab.classList.add('hidden');
  }

  if (state.workspacePanel === 'admin') {
    state.workspacePanel = 'routes';
  }

  setWorkspacePanel(state.workspacePanel);
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

async function searchSchedulesForCriteria({ source, destination, date }) {
  const params = buildScheduleSearchParams({ source, destination, date });
  let schedules = await loadSchedules(params.toString() ? `?${params.toString()}` : '');
  if (schedules === null) {
    return {
      schedules: null,
      statusText: 'Could not search schedules. Please try again.',
      statusClass: 'summary status-err'
    };
  }

  let statusText = `${schedules.length} schedule(s) found.`;
  let statusClass = 'summary status-ok';
  let fallbackDate = null;

  if (schedules.length === 0 && date) {
    const fallbackParams = buildScheduleSearchParams({ source, destination, date: '' });
    const fallback = await loadSchedules(fallbackParams.toString() ? `?${fallbackParams.toString()}` : '');
    if (fallback && fallback.length > 0) {
      schedules = fallback;
      fallbackDate = fallback
        .map(schedule => schedule.travelDate)
        .sort((a, b) => Math.abs(new Date(a) - new Date(date)) - Math.abs(new Date(b) - new Date(date)))[0];
      statusText = `No buses found for ${date}. Showing nearest available date ${fallbackDate}.`;
      statusClass = 'summary status-err';
    }
  }

  return { schedules, statusText, statusClass, fallbackDate };
}

async function showBusResults({ source, destination, date }) {
  const criteria = { source, destination, date };
  const criteriaLabel = searchCriteriaLabel(criteria);
  elements.searchStatus.textContent = 'Searching schedules...';
  elements.searchStatus.className = 'summary';
  if (elements.busResultsStatus) {
    elements.busResultsStatus.textContent = 'Searching schedules...';
    elements.busResultsStatus.className = 'summary';
  }
  elements.busListCopy.textContent = `Loading results for ${criteriaLabel}.`;
  renderBusResultsHeader(criteria);

  const result = await searchSchedulesForCriteria(criteria);
  if (result.schedules === null) {
    elements.searchStatus.textContent = result.statusText;
    elements.searchStatus.className = result.statusClass;
    if (elements.busResultsStatus) {
      elements.busResultsStatus.textContent = result.statusText;
      elements.busResultsStatus.className = result.statusClass;
    }
    elements.busListCopy.textContent = result.statusText;
    renderBusResultsHeader(criteria, result);
    setFlowStep('buses', 'Bus List Page could not be loaded.');
    return;
  }

  elements.searchStatus.textContent = result.statusText;
  elements.searchStatus.className = result.statusClass;
  if (elements.busResultsStatus) {
    elements.busResultsStatus.textContent = result.statusText;
    elements.busResultsStatus.className = result.statusClass;
  }
  renderBusResultsHeader(criteria, result);

  if (!result.schedules.length) {
    elements.busListCopy.textContent = `No buses found for ${criteriaLabel}.`;
    setFlowStep('buses', 'No schedules matched the selected search.');
    return;
  }

  if (result.fallbackDate) {
    elements.busListCopy.textContent =
      `${criteriaLabel} | Showing nearest travel date ${formatDisplayDate(result.fallbackDate)}.`;
  } else {
    elements.busListCopy.textContent = `${criteriaLabel} | ${result.schedules.length} option(s) available.`;
  }

  setFlowStep('buses', 'Bus List Page ready. Choose one schedule to continue.');
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

function togglePaymentIdField(visible) {
  if (!elements.paymentIdField || !elements.paymentIdInput) return;
  elements.paymentIdField.classList.toggle('hidden', !visible);
  elements.paymentIdInput.required = visible;
  if (!visible) {
    elements.paymentIdInput.value = '';
  }
}

function refreshPaymentAction() {
  const pending = state.pendingTraveller;
  renderPaymentMethodState();
  if (!pending) {
    togglePaymentIdField(false);
    if (elements.paymentIdLabel) {
      elements.paymentIdLabel.textContent = 'Payment ID';
    }
    if (elements.paymentIdHint) {
      elements.paymentIdHint.textContent = 'Use the reference returned by the payment gateway.';
    }
    elements.paymentActionTitle.textContent = 'Payment Action';
    elements.paymentActionCopy.textContent = 'Create a payment session or confirm pay-on-board.';
    elements.paymentActionBtn.textContent = 'Continue';
    elements.paymentActionBtn.disabled = true;
    return;
  }

  if (pending.paymentMode === 'ONLINE' && !state.paymentDraft) {
    togglePaymentIdField(false);
    elements.paymentActionTitle.textContent = 'Create Mock Checkout';
    elements.paymentActionCopy.textContent = 'A temporary seat lock and mock payment session will be created first.';
    elements.paymentActionBtn.textContent = 'Start payment';
    elements.paymentActionBtn.disabled = false;
    return;
  }

  if (pending.paymentMode === 'ONLINE' && state.paymentDraft) {
    togglePaymentIdField(true);
    if (elements.paymentIdLabel) {
      elements.paymentIdLabel.textContent = 'Mock Payment ID';
    }
    if (elements.paymentIdHint) {
      elements.paymentIdHint.textContent = 'Enter any mock gateway reference, for example mock-payment-123.';
    }
    elements.paymentActionTitle.textContent = 'Complete Mock Payment';
    elements.paymentActionCopy.textContent = `Session ${state.paymentDraft.paymentSessionId} is ready. Verify it to finalize the booking.`;
    elements.paymentActionBtn.disabled = !elements.paymentIdInput.value.trim();
    elements.paymentActionBtn.textContent = 'Complete payment';
    return;
  }

  togglePaymentIdField(false);
  elements.paymentActionTitle.textContent = 'Confirm Pay On Board';
  elements.paymentActionCopy.textContent = 'This final step creates the booking with payment status PENDING.';
  elements.paymentActionBtn.disabled = false;
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

function renderPagination(container, pageData, onPageChange) {
  if (!container || !pageData || pageData.totalPages <= 1) {
    if (container) {
      container.innerHTML = '';
      container.classList.add('hidden');
    }
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

function renderMyBookings(pageData) {
  const items = pageData?.items || [];
  const statusLabel = statusFilterLabel(state.myBookingsPage.status);
  const startIndex = items.length ? state.myBookingsPage.page * state.myBookingsPage.size + 1 : 0;
  const endIndex = items.length ? startIndex + items.length - 1 : 0;

  if (!items.length) {
    elements.myBookingsSummary.textContent = `No bookings found for ${statusLabel}.`;
    elements.myBookings.innerHTML = '<div class="item muted">No booking history available for the selected filter.</div>';
    renderPagination(elements.myBookingsPagination, null, () => {});
    return;
  }

  elements.myBookingsSummary.textContent =
    `Showing ${startIndex}-${endIndex} of ${pageData.totalElements} booking(s) | Filter: ${statusLabel}`;

  elements.myBookings.innerHTML = items.map(booking => `
    <div class="item">
      <div class="row">
        <strong>${booking.source} -> ${booking.destination}</strong>
        <span class="info-pill">${normalizeBookingStatus(booking.bookingStatus)} | ${formatPaymentStatus(booking)}</span>
      </div>
      <div class="row muted">
        <span>${booking.busNumber} | Seat(s) ${formatSeatNumbers(booking)} | ${formatDisplayDate(booking.travelDate)} ${formatTime(booking.departureTime)}</span>
        <span>${formatAmount(booking.amount)}</span>
      </div>
      <div class="row muted">
        <span>Booked ${formatDateTime(booking.bookedAt)}</span>
        <span>${booking.paymentMode}${booking.paymentReference ? ` | Ref ${booking.paymentReference}` : ''}</span>
      </div>
      <div class="row">
        <span class="muted">Booking #${booking.bookingId}${booking.cancelledAt ? ` | Cancelled ${formatDateTime(booking.cancelledAt)}` : ''}</span>
        ${normalizeBookingStatus(booking.bookingStatus) === 'BOOKED'
          ? `<button type="button" data-cancel="${booking.bookingId}">Cancel</button>`
          : '<span class="muted">Cancelled</span>'}
      </div>
    </div>
  `).join('');

  renderPagination(elements.myBookingsPagination, pageData, nextPage => {
    state.myBookingsPage.page = nextPage;
    loadMyBookings();
  });

  elements.myBookings.querySelectorAll('[data-cancel]').forEach(button => {
    button.addEventListener('click', async () => {
      const bookingId = Number(button.dataset.cancel);
      if (!window.confirm(`Cancel booking #${bookingId}? The seat will be released.`)) return;

      try {
        await api(`/api/bookings/${bookingId}`, { method: 'DELETE', auth: true });
        await loadMyBookings();
        if (isAdmin()) {
          await loadAdminDashboard();
        }
        if (state.selectedSchedule) await loadSeats(state.selectedSchedule.id, false, true);
      } catch (err) {
        alert(err.message);
      }
    });
  });
}

async function loadMyBookings(options = {}) {
  if (!getToken()) {
    state.myBookingsPage.page = 0;
    elements.myBookingsSummary.textContent = 'Login to see your bookings.';
    elements.myBookings.innerHTML = '<div class="item muted">Login to see your bookings.</div>';
    renderPagination(elements.myBookingsPagination, null, () => {});
    return;
  }

  if (options.resetPage) {
    state.myBookingsPage.page = 0;
  }

  state.myBookingsPage.status = elements.myBookingsStatus?.value || '';
  const params = new URLSearchParams({
    page: String(state.myBookingsPage.page),
    size: String(state.myBookingsPage.size)
  });
  if (state.myBookingsPage.status) {
    params.set('status', state.myBookingsPage.status);
  }

  try {
    const pageData = await api(`/api/my-bookings/paged?${params.toString()}`, { auth: true });
    if (pageData.totalPages > 0 && !pageData.items.length && state.myBookingsPage.page >= pageData.totalPages) {
      state.myBookingsPage.page = pageData.totalPages - 1;
      return loadMyBookings();
    }
    renderMyBookings(pageData);
  } catch (err) {
    elements.myBookingsSummary.textContent = 'Could not fetch booking history.';
    elements.myBookings.innerHTML = `<div class="item muted">${err.message}</div>`;
    renderPagination(elements.myBookingsPagination, null, () => {});
  }
}

function renderAdminBookings(pageData) {
  const items = pageData?.items || [];
  const statusLabel = statusFilterLabel(state.adminBookingsPage.status);
  const startIndex = items.length ? state.adminBookingsPage.page * state.adminBookingsPage.size + 1 : 0;
  const endIndex = items.length ? startIndex + items.length - 1 : 0;

  if (!items.length) {
    elements.adminBookingsSummary.textContent = `No admin bookings found for ${statusLabel}.`;
    elements.adminBookings.innerHTML = '<div class="item muted">No bookings matched the current admin filter.</div>';
    renderPagination(elements.adminBookingsPagination, null, () => {});
    return;
  }

  elements.adminBookingsSummary.textContent =
    `Showing ${startIndex}-${endIndex} of ${pageData.totalElements} booking(s) | Filter: ${statusLabel}`;

  elements.adminBookings.innerHTML = items.map(booking => `
    <div class="item">
      <div class="row">
        <strong>${booking.source} -> ${booking.destination}</strong>
        <span class="info-pill">${normalizeBookingStatus(booking.bookingStatus)} | ${formatPaymentStatus(booking)}</span>
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
        <span>${booking.paymentMode} / ${formatPaymentStatus(booking)}${booking.paymentGateway ? ` / ${booking.paymentGateway}` : ''}</span>
        <span>${booking.cancelledAt ? `Cancelled by ${booking.cancelledByEmail || booking.cancelledByName || 'system'}` : 'Active booking record'}</span>
      </div>
    </div>
  `).join('');

  renderPagination(elements.adminBookingsPagination, pageData, nextPage => {
    state.adminBookingsPage.page = nextPage;
    loadAdminBookings();
  });
}

async function loadAdminBookings(options = {}) {
  if (!isAdmin()) return;

  if (options.resetPage) {
    state.adminBookingsPage.page = 0;
  }

  state.adminBookingsPage.status = elements.adminBookingsStatus?.value || '';
  const params = new URLSearchParams({
    page: String(state.adminBookingsPage.page),
    size: String(state.adminBookingsPage.size)
  });
  if (state.adminBookingsPage.status) {
    params.set('status', state.adminBookingsPage.status);
  }

  try {
    const pageData = await api(`/api/admin/bookings/paged?${params.toString()}`, { auth: true });
    if (pageData.totalPages > 0 && !pageData.items.length && state.adminBookingsPage.page >= pageData.totalPages) {
      state.adminBookingsPage.page = pageData.totalPages - 1;
      return loadAdminBookings();
    }
    renderAdminBookings(pageData);
  } catch (err) {
    elements.adminBookingsSummary.textContent = 'Could not fetch admin bookings.';
    elements.adminBookings.innerHTML = `<div class="item muted">${err.message}</div>`;
    renderPagination(elements.adminBookingsPagination, null, () => {});
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

function renderAdminTrends(trend) {
  const items = trend?.items || [];
  if (!items.length) {
    elements.adminTrendSummary.innerHTML = '<div class="item muted">No booking trend data is available for the selected date range.</div>';
    elements.adminTrendChart.innerHTML = '';
    return;
  }

  const maxVolume = Math.max(...items.map(point => point.confirmedBookings + point.cancelledBookings), 1);
  elements.adminTrendSummary.innerHTML = `
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

  elements.adminTrendChart.innerHTML = items.map(point => {
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

async function loadAdminTrends() {
  if (!isAdmin()) return;

  state.adminTrendRangeDays = Number(elements.adminTrendRange?.value || state.adminTrendRangeDays || 7);
  const toDate = new Date();
  toDate.setHours(0, 0, 0, 0);
  const fromDate = new Date(toDate);
  fromDate.setDate(fromDate.getDate() - (state.adminTrendRangeDays - 1));

  const params = new URLSearchParams({
    fromDate: toIsoDate(fromDate),
    toDate: toIsoDate(toDate)
  });

  try {
    const trend = await api(`/api/admin/metrics/trends?${params.toString()}`, { auth: true });
    renderAdminTrends(trend);
  } catch (err) {
    elements.adminTrendSummary.innerHTML = `<div class="item muted">${err.message}</div>`;
    elements.adminTrendChart.innerHTML = '';
  }
}

function renderAdminMonitoring(monitoring) {
  const appInfo = monitoring.info?.app || {};
  const javaInfo = monitoring.info?.java || {};
  const metricsCount = monitoring.metrics?.names?.length || 0;

  elements.adminMonitoring.innerHTML = `
    <div class="monitor-card">
      <div class="label">Health</div>
      <div class="monitor-value">${monitoring.health?.status || 'UNKNOWN'}</div>
      <div class="monitor-copy">Overall actuator health endpoint.</div>
    </div>
    <div class="monitor-card">
      <div class="label">Readiness</div>
      <div class="monitor-value">${monitoring.readiness?.status || 'UNKNOWN'}</div>
      <div class="monitor-copy">Traffic acceptance probe for deploy/load balancer checks.</div>
    </div>
    <div class="monitor-card">
      <div class="label">App Stage</div>
      <div class="monitor-value">${appInfo.stage || '-'}</div>
      <div class="monitor-copy">${appInfo.frontend || 'Frontend unknown'}</div>
    </div>
    <div class="monitor-card">
      <div class="label">Metrics</div>
      <div class="monitor-value">${metricsCount}</div>
      <div class="monitor-copy">Actuator metric names currently exposed.</div>
    </div>
    <div class="monitor-card">
      <div class="label">Java</div>
      <div class="monitor-value">${javaInfo.version || '-'}</div>
      <div class="monitor-copy">${javaInfo.vendor || 'JVM vendor unavailable'}</div>
    </div>
    <div class="monitor-card">
      <div class="label">Prometheus</div>
      <div class="monitor-value">Enabled</div>
      <div class="monitor-copy">Scrape endpoint available at /actuator/prometheus.</div>
    </div>
  `;
}

async function loadAdminMonitoring() {
  if (!isAdmin()) return;
  try {
    const [health, readiness, info, metrics] = await Promise.all([
      api('/actuator/health'),
      api('/actuator/health/readiness'),
      api('/actuator/info'),
      api('/actuator/metrics', { auth: true })
    ]);
    renderAdminMonitoring({ health, readiness, info, metrics });
  } catch (err) {
    elements.adminMonitoring.innerHTML = `<div class="item muted">${err.message}</div>`;
  }
}

async function loadAdminDashboard() {
  if (!isAdmin()) return;
  await Promise.all([
    loadAdminMetrics(),
    loadAdminBookings(),
    loadAdminTrends(),
    loadAdminMonitoring()
  ]);
}

function updateAuthStatus() {
  const user = currentUser();
  const loggedIn = Boolean(user);

  elements.guestOnlyBlocks.forEach(block => {
    block.classList.toggle('hidden', loggedIn);
  });
  elements.userOnlyBlocks.forEach(block => {
    block.classList.toggle('hidden', !loggedIn);
  });

  if (!user) {
    elements.authStatus.textContent = 'Not logged in.';
    setAccountPanel('login');
    showGoogleAuthStatus('');
    if (elements.accountAdminLink) {
      elements.accountAdminLink.classList.add('hidden');
    }
    elements.bookingForm.classList.add('hidden');
    elements.adminPanel.classList.add('hidden');
    elements.adminBookingsSummary.textContent = 'Admin login required.';
    elements.adminBookings.innerHTML = '<div class="item muted">Admin login required.</div>';
    elements.adminTrendSummary.innerHTML = '<div class="item muted">Admin login required.</div>';
    elements.adminTrendChart.innerHTML = '';
    elements.adminMonitoring.innerHTML = '<div class="item muted">Admin login required.</div>';
    loadMyBookings();
    refreshWorkspaceAccess();
    return;
  }

  elements.authStatus.textContent = `Logged in as ${user.name} (${user.role})`;
  setAccountMenu(false);
  showGoogleAuthStatus('');
  if (elements.accountAdminLink) {
    elements.accountAdminLink.classList.toggle('hidden', !isAdmin());
  }
  loadMyBookings();
  elements.adminPanel.classList.add('hidden');
  elements.adminBookingsSummary.textContent = 'Use /admin.html for admin tools.';
  elements.adminBookings.innerHTML = '<div class="item muted">Use the dedicated admin page for admin tools.</div>';
  elements.adminTrendSummary.innerHTML = '<div class="item muted">Use the dedicated admin page for admin tools.</div>';
  elements.adminTrendChart.innerHTML = '';
  elements.adminMonitoring.innerHTML = '<div class="item muted">Use the dedicated admin page for admin tools.</div>';
  refreshWorkspaceAccess();
}

async function releasePendingLock() {
  if (!state.paymentDraft?.bookingId) return;
  try {
    await api(`/api/bookings/locks/${state.paymentDraft.bookingId}`, { method: 'DELETE', auth: true });
  } catch {
    // Best-effort unlock when leaving the payment step.
  } finally {
    refreshPaymentAction();
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
  togglePaymentIdField(false);
  elements.paymentStatus.textContent = '';
  elements.bookingStatus.textContent = '';
  elements.paymentSummary.innerHTML = '<div><span>Status</span><strong>No pending booking</strong></div>';
  elements.confirmationCard.innerHTML = '';
  renderPaymentMethodState();
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
        paymentGateway: checkout.paymentGateway,
        payableUntil: checkout.payableUntil
      };
      elements.paymentStatus.textContent = `Payment session ${checkout.paymentSessionId} created for ${formatAmount(checkout.amount)}.`;
      elements.paymentStatus.className = 'summary status-ok';
      refreshPaymentAction();
      updateJourneySummary();
      return;
    }

    if (pending.paymentMode === 'ONLINE' && state.paymentDraft) {
      const paymentId = elements.paymentIdInput.value.trim();
      if (!paymentId) {
        elements.paymentStatus.textContent = 'Enter payment ID to complete online booking.';
        elements.paymentStatus.className = 'summary status-err';
        return;
      }
      const response = await api(`/api/bookings/locks/${state.paymentDraft.bookingId}/payments/verify`, {
        method: 'POST',
        body: JSON.stringify({
          paymentSessionId: state.paymentDraft.paymentSessionId,
          paymentId
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
      await loadAdminDashboard();
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

  if (currentUser()) {
    alert('Logout first to create a new account.');
    return;
  }

  try {
    const payload = {
      name: document.getElementById('reg-name').value.trim(),
      email: document.getElementById('reg-email').value.trim(),
      password: document.getElementById('reg-password').value
    };
    const data = await api('/api/auth/register', { method: 'POST', body: JSON.stringify(payload) });
    setAuth(data);
    document.getElementById('register-form').reset();
  } catch (err) {
    alert(err.message);
  }
});

document.getElementById('login-form').addEventListener('submit', async event => {
  event.preventDefault();

  const payload = {
    email: document.getElementById('login-email').value.trim(),
    password: document.getElementById('login-password').value
  };
  const user = currentUser();

  if (user) {
    const activeEmail = user.email?.trim().toLowerCase();
    const requestedEmail = payload.email.toLowerCase();

    if (activeEmail === requestedEmail) {
      document.getElementById('login-form').reset();
      setAccountMenu(false);
      updateAuthStatus();
      return;
    }

    alert('Logout first to switch accounts.');
    return;
  }

  try {
    const data = await api('/api/auth/login', { method: 'POST', body: JSON.stringify(payload) });
    setAuth(data);
    showGoogleAuthStatus('');
    document.getElementById('login-form').reset();
  } catch (err) {
    alert(err.message);
  }
});

document.getElementById('forgot-toggle-btn').addEventListener('click', () => {
  setAccountPanel('reset');
  setAccountMenu(true);
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
  clearAuth();
  showGoogleAuthStatus('');
  if (isResultsPage()) {
    window.location.assign('/');
    return;
  }
  resetBookingFlow();
});

if (elements.accountMenuBtn) {
  elements.accountMenuBtn.addEventListener('click', () => {
    const isOpen = elements.accountMenuBtn.getAttribute('aria-expanded') === 'true';
    setAccountMenu(!isOpen);
  });
}

if (elements.accountPanelTabs.length) {
  elements.accountPanelTabs.forEach(button => {
    button.addEventListener('click', () => {
      setAccountPanel(button.dataset.accountPanelTab);
    });
  });
}

document.addEventListener('click', event => {
  if (!elements.accountMenu || !elements.accountMenuBtn) return;
  if (elements.accountMenu.classList.contains('hidden')) return;
  if (elements.accountMenu.contains(event.target) || elements.accountMenuBtn.contains(event.target)) return;
  setAccountMenu(false);
});

document.addEventListener('keydown', event => {
  if (event.key === 'Escape' && elements.accountMenu && !elements.accountMenu.classList.contains('hidden')) {
    setAccountMenu(false);
  }
});

if (elements.workspaceTabs) {
  elements.workspaceTabs.addEventListener('click', event => {
    const button = event.target.closest('[data-workspace-tab]');
    if (!button) return;
    setWorkspacePanel(button.dataset.workspaceTab);
  });
}

if (elements.myBookingsStatus) {
  elements.myBookingsStatus.addEventListener('change', () => {
    loadMyBookings({ resetPage: true });
  });
}

if (elements.myBookingsRefresh) {
  elements.myBookingsRefresh.addEventListener('click', () => {
    loadMyBookings();
  });
}

if (elements.adminBookingsStatus) {
  elements.adminBookingsStatus.addEventListener('change', () => {
    loadAdminBookings({ resetPage: true });
  });
}

if (elements.adminBookingsRefresh) {
  elements.adminBookingsRefresh.addEventListener('click', () => {
    loadAdminBookings();
  });
}

if (elements.adminTrendRange) {
  elements.adminTrendRange.addEventListener('change', () => {
    loadAdminTrends();
  });
}

if (elements.adminTrendsRefresh) {
  elements.adminTrendsRefresh.addEventListener('click', () => {
    loadAdminTrends();
  });
}

if (elements.adminMonitorRefresh) {
  elements.adminMonitorRefresh.addEventListener('click', () => {
    loadAdminMonitoring();
  });
}

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
  if (!isResultsPage()) {
    navigateToResultsPage({ source, destination, date });
    return;
  }
  await showBusResults({ source, destination, date });
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
if (elements.paymentIdInput) {
  elements.paymentIdInput.addEventListener('input', refreshPaymentAction);
}

document.getElementById('back-to-search').addEventListener('click', () => {
  if (isResultsPage()) {
    window.location.assign('/');
    return;
  }
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
  if (isResultsPage()) {
    window.location.assign('/');
    return;
  }
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
  if (isResultsPage()) {
    refreshJourneyDateUI();
  } else {
    setJourneyDate(0);
  }
} else {
  refreshJourneyDateUI();
}

renderPaymentSummary();
refreshPaymentAction();
updateJourneySummary();
setAccountPanel(state.accountPanel);
updateAuthStatus();
initializeGoogleAuth();
loadRoutes();
loadTourismRoutes();
refreshWorkspaceAccess();

if (isResultsPage()) {
  const params = new URLSearchParams(window.location.search);
  const source = params.get('source')?.trim() || '';
  const destination = params.get('destination')?.trim() || '';
  const date = params.get('date')?.trim() || '';
  updateSearchInputs({ source, destination, date });
  setFlowStep('buses', 'Loading bus results...');
  showBusResults({ source, destination, date });
} else {
  setFlowStep('search', 'Start by searching a route.');
}
