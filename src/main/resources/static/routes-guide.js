const ROUTE_FAVORITES_KEY = 'narayanRouteFavorites';
const ROUTE_RECENTS_KEY = 'narayanRouteRecents';
const MAX_ROUTE_RECENTS = 6;
const MAX_COMPARE_ROUTES = 3;

const state = {
  routes: [],
  schedules: [],
  routeReviewSummaries: {},
  compareRouteIds: []
};

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

function formatAmount(amount) {
  if (amount == null) return '-';
  return `Rs ${Number(amount).toFixed(0)}`;
}

function formatAverageRating(value) {
  if (value == null) return '0.0';
  return Number(value).toFixed(1);
}

function parseLines(value) {
  if (!value) return [];
  return value
    .split('\n')
    .map(item => item.trim())
    .filter(Boolean);
}

function todayIso() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

async function fetchJson(path) {
  const response = await fetch(path);
  const isJson = response.headers.get('content-type')?.includes('application/json');
  const body = isJson ? await response.json() : null;
  if (!response.ok) throw new Error(body?.error || 'Request failed');
  return body;
}

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function fareCopyForSchedules(schedules) {
  const fareValues = schedules.map(schedule => Number(schedule.baseFare)).filter(Number.isFinite);
  if (!fareValues.length) {
    return 'Fare appears after schedules are published';
  }
  const minFare = Math.min(...fareValues);
  const maxFare = Math.max(...fareValues);
  return minFare === maxFare ? formatAmount(minFare) : `${formatAmount(minFare)} - ${formatAmount(maxFare)}`;
}

function sortSchedules(schedules) {
  return [...schedules].sort((left, right) => {
    if (left.travelDate !== right.travelDate) return left.travelDate.localeCompare(right.travelDate);
    return left.departureTime.localeCompare(right.departureTime);
  });
}

function routeSchedules(routeId) {
  return sortSchedules(state.schedules.filter(schedule => schedule.route.id === routeId));
}

function reviewSummaryForRoute(routeId) {
  return state.routeReviewSummaries[String(routeId)] || null;
}

function reviewSummaryText(routeId) {
  const summary = reviewSummaryForRoute(routeId);
  if (!summary?.totalReviews) return 'No reviews yet';
  const noun = summary.totalReviews === 1 ? 'review' : 'reviews';
  return `${formatAverageRating(summary.averageRating)}/5 from ${summary.totalReviews} ${noun}`;
}

function readStoredRoutes(key) {
  const raw = localStorage.getItem(key);
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function writeStoredRoutes(key, routes) {
  localStorage.setItem(key, JSON.stringify(routes));
}

function routeSnapshot(route) {
  if (!route) return null;
  return {
    id: route.id,
    source: route.source,
    destination: route.destination,
    distanceKm: route.distanceKm,
    tourismRoute: Boolean(route.tourismRoute),
    description: route.description || '',
    active: route.active !== false
  };
}

function favoriteRoutes() {
  return readStoredRoutes(ROUTE_FAVORITES_KEY);
}

function recentRoutes() {
  return readStoredRoutes(ROUTE_RECENTS_KEY);
}

function isFavoriteRoute(routeId) {
  return favoriteRoutes().some(route => route.id === routeId);
}

function toggleFavoriteRoute(route) {
  const snapshot = routeSnapshot(route);
  if (!snapshot?.id) return false;
  const current = favoriteRoutes();
  const next = isFavoriteRoute(snapshot.id)
    ? current.filter(item => item.id !== snapshot.id)
    : [snapshot, ...current.filter(item => item.id !== snapshot.id)];
  writeStoredRoutes(ROUTE_FAVORITES_KEY, next.slice(0, 8));
  return !current.some(item => item.id === snapshot.id);
}

function rememberRecentRoute(route) {
  const snapshot = routeSnapshot(route);
  if (!snapshot?.id) return;
  const next = [snapshot, ...recentRoutes().filter(item => item.id !== snapshot.id)].slice(0, MAX_ROUTE_RECENTS);
  writeStoredRoutes(ROUTE_RECENTS_KEY, next);
}

function buildResultsUrl(route) {
  const params = new URLSearchParams({
    source: route.source,
    destination: route.destination
  });
  return `/results.html?${params.toString()}`;
}

function buildRouteGuideUrl(routeId) {
  const params = new URLSearchParams({ routeId: String(routeId) });
  return `/routes-guide.html?${params.toString()}`;
}

function routeFromDataset(element) {
  if (!element) return null;
  const id = Number(element.dataset.routeId);
  if (!id) return null;
  return state.routes.find(route => route.id === id) || {
    id,
    source: element.dataset.routeSource || '',
    destination: element.dataset.routeDestination || '',
    distanceKm: Number(element.dataset.routeDistanceKm || 0),
    tourismRoute: element.dataset.routeTourism === 'true',
    description: element.dataset.routeDescription || '',
    active: element.dataset.routeActive !== 'false'
  };
}

function routeCardActionData(route) {
  return `
    data-route-id="${route.id}"
    data-route-source="${escapeHtml(route.source)}"
    data-route-destination="${escapeHtml(route.destination)}"
    data-route-distance-km="${route.distanceKm || ''}"
    data-route-tourism="${route.tourismRoute ? 'true' : 'false'}"
    data-route-description="${escapeHtml(route.description || '')}"
    data-route-active="${route.active === false ? 'false' : 'true'}"
  `;
}

function routeMemoryCard(route, label) {
  return `
    <article class="planner-route-card">
      <div class="row">
        <strong>${escapeHtml(route.source)} -> ${escapeHtml(route.destination)}</strong>
        <span class="bus-chip">${label}</span>
      </div>
      <p class="planner-route-copy">${escapeHtml(route.description || 'Route shortcuts help you revisit the right corridor faster.')}</p>
      <div class="planner-route-meta">
        <span>${escapeHtml(reviewSummaryText(route.id))}</span>
        <span>${escapeHtml(String(route.distanceKm || '-'))} km</span>
      </div>
      <div class="route-action-row">
        <button type="button" class="secondary compact-btn" data-route-spotlight="true" ${routeCardActionData(route)}>Open guide</button>
        <button type="button" class="ghost-btn compact-btn" data-route-save="true" ${routeCardActionData(route)}>${isFavoriteRoute(route.id) ? 'Saved route' : 'Save route'}</button>
        <a class="ghost-btn compact-btn" href="${buildResultsUrl(route)}">Book route</a>
      </div>
    </article>
  `;
}

function compareCard(route) {
  const schedules = routeSchedules(route.id);
  const nextSchedule = schedules[0] || null;
  const summary = reviewSummaryForRoute(route.id);
  return `
    <article class="planner-route-card planner-route-card-compare">
      <div class="row">
        <strong>${escapeHtml(route.source)} -> ${escapeHtml(route.destination)}</strong>
        <span class="bus-chip">${route.tourismRoute ? 'Tourism' : 'Regular'}</span>
      </div>
      <div class="planner-route-meta planner-route-meta-stack">
        <span>Distance: ${escapeHtml(String(route.distanceKm || '-'))} km</span>
        <span>Fare band: ${escapeHtml(fareCopyForSchedules(schedules))}</span>
        <span>Next trip: ${escapeHtml(nextSchedule ? `${formatDisplayDate(nextSchedule.travelDate)} at ${formatTime(nextSchedule.departureTime)}` : 'Awaiting next schedule')}</span>
        <span>Rating: ${escapeHtml(summary?.totalReviews ? `${formatAverageRating(summary.averageRating)}/5 (${summary.totalReviews})` : 'No reviews yet')}</span>
      </div>
      <div class="route-action-row">
        <button type="button" class="secondary compact-btn" data-route-spotlight="true" ${routeCardActionData(route)}>View guide</button>
        <button type="button" class="ghost-btn compact-btn" data-route-compare="true" ${routeCardActionData(route)}>Remove</button>
      </div>
    </article>
  `;
}

function renderPlannerDesk() {
  const routePlannerStatus = document.getElementById('route-planner-status');
  const routeMemoryStatus = document.getElementById('route-memory-status');
  const routeMemory = document.getElementById('route-memory');
  const routeCompareStatus = document.getElementById('route-compare-status');
  const routeCompare = document.getElementById('route-compare');
  if (!routePlannerStatus || !routeMemoryStatus || !routeMemory || !routeCompareStatus || !routeCompare) {
    return;
  }

  const saved = favoriteRoutes()
    .map(savedRoute => state.routes.find(route => route.id === savedRoute.id) || savedRoute)
    .map(route => ({ route, label: 'Saved' }));
  const recent = recentRoutes()
    .filter(recentRoute => !saved.some(item => item.route.id === recentRoute.id))
    .map(recentRoute => state.routes.find(route => route.id === recentRoute.id) || recentRoute)
    .map(route => ({ route, label: 'Recent' }));
  const memoryRoutes = [...saved, ...recent].slice(0, 6);

  if (!memoryRoutes.length) {
    routeMemoryStatus.textContent = 'No saved or recent routes yet.';
    routeMemory.innerHTML = '<div class="item muted">Open route guides or save routes from the live directory below.</div>';
  } else {
    routeMemoryStatus.textContent = `${memoryRoutes.length} route memory card(s) ready.`;
    routeMemory.innerHTML = memoryRoutes.map(item => routeMemoryCard(item.route, item.label)).join('');
  }

  const compareRoutes = state.compareRouteIds
    .map(routeId => state.routes.find(route => route.id === routeId))
    .filter(Boolean);
  if (!compareRoutes.length) {
    routeCompareStatus.textContent = 'Select compare on route cards below.';
    routeCompare.innerHTML = '<div class="item muted">Select compare on live route cards to build a route matrix here.</div>';
  } else {
    routeCompareStatus.textContent = `${compareRoutes.length} route(s) in compare mode.`;
    routeCompare.innerHTML = compareRoutes.map(route => compareCard(route)).join('');
  }

  routePlannerStatus.textContent = `${favoriteRoutes().length} saved route(s), ${recentRoutes().length} recent route(s), and ${state.compareRouteIds.length} compare slot(s) active.`;
}

function renderRouteSpotlight(route, reviewSummary) {
  const spotlightEl = document.getElementById('route-spotlight');
  const statusEl = document.getElementById('route-spotlight-status');
  if (!spotlightEl || !statusEl) return;

  if (!route) {
    statusEl.textContent = 'No route spotlight is available right now.';
    spotlightEl.innerHTML = '<div class="item muted">Route guidance will appear here once active routes are available.</div>';
    return;
  }

  const orderedSchedules = routeSchedules(route.id);
  const nextSchedule = orderedSchedules[0] || null;
  const highlightItems = parseLines(route.travelHighlights);
  const tipItems = parseLines(route.travelTips);
  const reviewItems = reviewSummary?.reviews || [];

  spotlightEl.innerHTML = `
    <article class="route-spotlight-card">
      <p class="section-tag">${route.tourismRoute ? 'Tourism Route' : 'Regular Corridor'}</p>
      <h3>${route.source} to ${route.destination}</h3>
      <p>${route.description || 'Route guidance will appear here once the admin team adds route content.'}</p>
      <div class="route-directory-meta">
        <span class="bus-chip">${route.distanceKm} km</span>
        <span class="bus-chip">${orderedSchedules.length} upcoming trip(s)</span>
        <span class="bus-chip">${fareCopyForSchedules(orderedSchedules)}</span>
        <span class="bus-chip">${reviewSummary?.totalReviews ? `${formatAverageRating(reviewSummary.averageRating)}/5 from ${reviewSummary.totalReviews}` : 'No reviews yet'}</span>
      </div>
      <div class="page-cta-group">
        <a class="ghost-btn" href="${buildResultsUrl(route)}">Book this corridor</a>
        <button type="button" class="ghost-btn" data-route-save="true" ${routeCardActionData(route)}>${isFavoriteRoute(route.id) ? 'Saved route' : 'Save route'}</button>
        <button type="button" class="ghost-btn" data-route-compare="true" ${routeCardActionData(route)}>${state.compareRouteIds.includes(route.id) ? 'Remove compare' : 'Compare route'}</button>
      </div>
    </article>
    <article class="route-spotlight-card">
      <h3>Route briefing</h3>
      <ul class="notes-list">
        <li>Next departure: ${nextSchedule ? `${formatDisplayDate(nextSchedule.travelDate)} at ${formatTime(nextSchedule.departureTime)}` : 'No upcoming departure published yet'}</li>
        <li>Boarding preview: ${nextSchedule?.boardingPoint || `${route.source} main boarding zone`}</li>
        <li>Drop preview: ${nextSchedule?.droppingPoint || `${route.destination} main drop zone`}</li>
      </ul>
      <div class="route-content-grid">
        <div class="route-content-panel">
          <h4>Highlights</h4>
          <ul class="notes-list">
            ${(highlightItems.length ? highlightItems : ['Highlights will appear once route content is added.'])
              .map(item => `<li>${escapeHtml(item)}</li>`).join('')}
          </ul>
        </div>
        <div class="route-content-panel">
          <h4>Travel tips</h4>
          <ul class="notes-list">
            ${(tipItems.length ? tipItems : ['Travel tips will appear once route content is added.'])
              .map(item => `<li>${escapeHtml(item)}</li>`).join('')}
          </ul>
        </div>
        <div class="route-content-panel">
          <h4>Recent reviews</h4>
          <ul class="notes-list">
            ${(reviewItems.length
              ? reviewItems.map(item => `${item.rating}/5 | ${item.reviewerName}: ${item.comment}`)
              : ['Verified route reviews will appear here once travellers submit feedback.'])
              .map(item => `<li>${escapeHtml(item)}</li>`).join('')}
          </ul>
        </div>
      </div>
    </article>
  `;

  statusEl.textContent = `Showing live guide content for ${route.source} to ${route.destination}.`;
}

async function showRouteSpotlight(route) {
  if (!route) return;
  rememberRecentRoute(route);
  renderPlannerDesk();
  window.history.replaceState({}, '', buildRouteGuideUrl(route.id));
  try {
    const reviewSummary = await fetchJson(`/api/reviews/route/${route.id}?limit=3`);
    renderRouteSpotlight(route, reviewSummary);
  } catch {
    renderRouteSpotlight(route, null);
  }
}

function renderRouteDirectory(focusRouteId = null) {
  const statusEl = document.getElementById('route-directory-status');
  const directoryEl = document.getElementById('route-directory');
  if (!statusEl || !directoryEl) return;

  if (!state.routes.length) {
    statusEl.textContent = 'No live routes available right now.';
    directoryEl.innerHTML = '<div class="item muted">No routes are currently active in the system.</div>';
    renderRouteSpotlight(null, null);
    return;
  }

  const routeCards = state.routes.map(route => {
    const schedules = routeSchedules(route.id);
    const nextSchedule = schedules[0] || null;
    return `
      <article class="route-directory-card">
        <div>
          <p class="section-tag">${route.tourismRoute ? 'Tourism Route' : 'Regular Corridor'}</p>
          <h3>${route.source} to ${route.destination}</h3>
        </div>
        <div class="route-directory-meta">
          <span class="bus-chip">${route.distanceKm} km</span>
          <span class="bus-chip">${schedules.length} upcoming option(s)</span>
          <span class="bus-chip">${fareCopyForSchedules(schedules)}</span>
          <span class="bus-chip">${reviewSummaryText(route.id)}</span>
        </div>
        <p>${route.description || (route.tourismRoute
          ? 'Suitable for premium sightseeing and longer planned travel across Bihar landmarks.'
          : 'Built for repeat intercity movement with simple booking and predictable availability.')}</p>
        <ul class="notes-list">
          <li>Next trip: ${nextSchedule ? `${formatDisplayDate(nextSchedule.travelDate)} at ${formatTime(nextSchedule.departureTime)}` : 'No upcoming departure published yet'}</li>
          <li>Typical fare: ${fareCopyForSchedules(schedules)}</li>
          <li>Service type: ${route.tourismRoute ? 'Tourism-focused fleet' : 'Daily intercity service'}</li>
        </ul>
        <div class="route-card-actions">
          <button type="button" class="secondary compact-btn" data-route-spotlight="true" ${routeCardActionData(route)}>View route guide</button>
          <button type="button" class="ghost-btn compact-btn" data-route-save="true" ${routeCardActionData(route)}>${isFavoriteRoute(route.id) ? 'Saved route' : 'Save route'}</button>
          <button type="button" class="ghost-btn compact-btn" data-route-compare="true" ${routeCardActionData(route)}>${state.compareRouteIds.includes(route.id) ? 'Remove compare' : 'Compare route'}</button>
        </div>
      </article>
    `;
  });

  statusEl.textContent = `${state.routes.length} live route card(s) loaded from the current system data.`;
  directoryEl.innerHTML = routeCards.join('');
  const focusRoute = state.routes.find(route => route.id === focusRouteId) || state.routes[0];
  showRouteSpotlight(focusRoute);
}

function toggleCompareRoute(routeId) {
  if (!routeId) return;
  if (state.compareRouteIds.includes(routeId)) {
    state.compareRouteIds = state.compareRouteIds.filter(id => id !== routeId);
  } else {
    state.compareRouteIds = [...state.compareRouteIds, routeId].slice(-MAX_COMPARE_ROUTES);
  }
  renderPlannerDesk();
  renderRouteDirectory(Number(new URLSearchParams(window.location.search).get('routeId')) || null);
}

function attachRouteActions(container) {
  if (!container) return;
  container.addEventListener('click', event => {
    const spotlightButton = event.target.closest('[data-route-spotlight]');
    if (spotlightButton) {
      showRouteSpotlight(routeFromDataset(spotlightButton));
      return;
    }

    const saveButton = event.target.closest('[data-route-save]');
    if (saveButton) {
      toggleFavoriteRoute(routeFromDataset(saveButton));
      renderPlannerDesk();
      renderRouteDirectory(Number(new URLSearchParams(window.location.search).get('routeId')) || null);
      return;
    }

    const compareButton = event.target.closest('[data-route-compare]');
    if (compareButton) {
      toggleCompareRoute(Number(compareButton.dataset.routeId));
    }
  });
}

async function loadRouteReviewSummaries(routeIds) {
  const uniqueIds = Array.from(new Set(routeIds.filter(Boolean)));
  if (!uniqueIds.length) {
    state.routeReviewSummaries = {};
    return;
  }

  const params = new URLSearchParams();
  uniqueIds.forEach(routeId => params.append('routeIds', String(routeId)));
  const summaries = await fetchJson(`/api/reviews/summary?${params.toString()}`);
  state.routeReviewSummaries = Object.fromEntries(
    summaries.map(summary => [String(summary.routeId), summary])
  );
}

async function loadRouteDirectory() {
  const statusEl = document.getElementById('route-directory-status');
  const spotlightStatusEl = document.getElementById('route-spotlight-status');
  const today = todayIso();
  try {
    const [routes, schedules] = await Promise.all([
      fetchJson('/api/routes'),
      fetchJson('/api/schedules')
    ]);
    state.routes = routes;
    state.schedules = schedules.filter(schedule => schedule.travelDate >= today);
    await loadRouteReviewSummaries(routes.map(route => route.id));
    renderPlannerDesk();
    const focusRouteId = Number(new URLSearchParams(window.location.search).get('routeId')) || null;
    renderRouteDirectory(focusRouteId);
  } catch (error) {
    if (statusEl) {
      statusEl.textContent = 'Could not load live route cards.';
      statusEl.className = 'summary status-err';
    }
    if (spotlightStatusEl) {
      spotlightStatusEl.textContent = 'Could not load route spotlight.';
      spotlightStatusEl.className = 'summary status-err';
    }
    const directoryEl = document.getElementById('route-directory');
    if (directoryEl) {
      directoryEl.innerHTML = `<div class="item muted">${escapeHtml(error.message)}</div>`;
    }
    const spotlightEl = document.getElementById('route-spotlight');
    if (spotlightEl) {
      spotlightEl.innerHTML = `<div class="item muted">${escapeHtml(error.message)}</div>`;
    }
  }
}

attachRouteActions(document.getElementById('route-directory'));
attachRouteActions(document.getElementById('route-memory'));
attachRouteActions(document.getElementById('route-compare'));
attachRouteActions(document.getElementById('route-spotlight'));

loadRouteDirectory();
