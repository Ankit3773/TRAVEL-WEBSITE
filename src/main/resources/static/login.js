const PAGE_CONFIG = window.NARAYAN_TRAVELS_CONFIG || {};
const PAGE_API_BASE_URL = normalizeBaseUrl(PAGE_CONFIG.apiBaseUrl || '');
const loginElements = {
  status: document.getElementById('page-auth-status'),
  forgotStatus: document.getElementById('page-forgot-status'),
  guestShell: document.getElementById('guest-auth-shell'),
  userShell: document.getElementById('login-page-user-card'),
  userHeading: document.getElementById('page-user-heading'),
  userCopy: document.getElementById('page-user-copy'),
  continueLink: document.getElementById('continue-link'),
  loginForm: document.getElementById('page-login-form'),
  registerForm: document.getElementById('page-register-form'),
  forgotForm: document.getElementById('page-forgot-password-form'),
  resetForm: document.getElementById('page-reset-password-form'),
  forgotToggleBtn: document.getElementById('page-forgot-toggle-btn'),
  logoutBtn: document.getElementById('page-logout-btn'),
  tabButtons: Array.from(document.querySelectorAll('[data-login-panel-tab]')),
  panels: Array.from(document.querySelectorAll('[data-login-panel]'))
};

function normalizeBaseUrl(value) {
  if (!value) return '';
  return value.endsWith('/') ? value.slice(0, -1) : value;
}

function resolveApiUrl(path) {
  if (!path) return path;
  if (/^https?:\/\//i.test(path)) return path;
  if (!path.startsWith('/')) return path;
  return `${PAGE_API_BASE_URL}${path}`;
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

function setAuth(data) {
  localStorage.setItem('authToken', data.token);
  localStorage.setItem('authUser', JSON.stringify(data));
}

function clearAuth() {
  localStorage.removeItem('authToken');
  localStorage.removeItem('authUser');
}

function targetPath() {
  const param = new URLSearchParams(window.location.search).get('returnTo') || '/';
  return param.startsWith('/') && !param.startsWith('//') ? param : '/';
}

async function api(url, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (options.body) headers['Content-Type'] = 'application/json';

  const response = await fetch(resolveApiUrl(url), { ...options, headers });
  const isJson = response.headers.get('content-type')?.includes('application/json');
  const body = isJson ? await response.json() : null;
  if (!response.ok) {
    throw new Error(body?.error || 'Request failed');
  }
  return body;
}

function setStatus(message, isError = false) {
  loginElements.status.textContent = message;
  loginElements.status.className = `flow-status login-flow-status${isError ? ' status-err' : ''}`;
}

function setForgotStatus(message, isError = false) {
  loginElements.forgotStatus.textContent = message;
  loginElements.forgotStatus.className = `summary${isError ? ' status-err' : ' status-ok'}`;
}

function setActivePanel(panel) {
  loginElements.tabButtons.forEach(button => {
    button.classList.toggle('active', button.dataset.loginPanelTab === panel);
  });
  loginElements.panels.forEach(section => {
    section.classList.toggle('hidden', section.dataset.loginPanel !== panel);
  });
}

function refreshPageState() {
  const user = currentUser();
  const loggedIn = Boolean(user);
  const nextPath = targetPath();

  loginElements.continueLink.href = nextPath;
  loginElements.guestShell.classList.toggle('hidden', loggedIn);
  loginElements.userShell.classList.toggle('hidden', !loggedIn);

  if (!user) {
    setStatus('Enter your details to continue.');
    return;
  }

  loginElements.userHeading.textContent = `Signed in as ${user.name}`;
  loginElements.userCopy.textContent = `${user.email} | ${user.role}. Continue back to booking or logout from this page.`;
  setStatus('You are already logged in.');
}

loginElements.tabButtons.forEach(button => {
  button.addEventListener('click', () => {
    setActivePanel(button.dataset.loginPanelTab);
    setStatus('Enter your details to continue.');
  });
});

loginElements.loginForm.addEventListener('submit', async event => {
  event.preventDefault();
  setStatus('Signing you in...');
  try {
    const payload = {
      email: document.getElementById('page-login-email').value.trim(),
      password: document.getElementById('page-login-password').value
    };
    const data = await api('/api/auth/login', { method: 'POST', body: JSON.stringify(payload) });
    setAuth(data);
    setStatus('Login successful. Redirecting...');
    window.location.assign(targetPath());
  } catch (error) {
    setStatus(error.message, true);
  }
});

loginElements.registerForm.addEventListener('submit', async event => {
  event.preventDefault();
  setStatus('Creating your account...');
  try {
    const payload = {
      name: document.getElementById('page-reg-name').value.trim(),
      email: document.getElementById('page-reg-email').value.trim(),
      password: document.getElementById('page-reg-password').value
    };
    const data = await api('/api/auth/register', { method: 'POST', body: JSON.stringify(payload) });
    setAuth(data);
    setStatus('Account created. Redirecting...');
    window.location.assign(targetPath());
  } catch (error) {
    setStatus(error.message, true);
  }
});

loginElements.forgotToggleBtn.addEventListener('click', () => {
  setActivePanel('reset');
  const loginEmail = document.getElementById('page-login-email').value.trim();
  if (loginEmail) {
    document.getElementById('page-forgot-email').value = loginEmail;
  }
  setForgotStatus('');
  setStatus('Generate a reset token to continue.');
});

loginElements.forgotForm.addEventListener('submit', async event => {
  event.preventDefault();
  setForgotStatus('Generating reset token...');
  try {
    const payload = { email: document.getElementById('page-forgot-email').value.trim() };
    const response = await api('/api/auth/forgot-password', { method: 'POST', body: JSON.stringify(payload) });
    if (response.resetToken) {
      document.getElementById('page-reset-token').value = response.resetToken;
      setForgotStatus('Reset token generated. Use it in the reset form.');
    } else {
      setForgotStatus(response.message || 'If the email exists, reset instructions have been generated.');
    }
  } catch (error) {
    setForgotStatus(error.message, true);
  }
});

loginElements.resetForm.addEventListener('submit', async event => {
  event.preventDefault();
  setForgotStatus('Updating password...');
  try {
    const payload = {
      token: document.getElementById('page-reset-token').value.trim(),
      newPassword: document.getElementById('page-reset-new-password').value
    };
    const response = await api('/api/auth/reset-password', { method: 'POST', body: JSON.stringify(payload) });
    setForgotStatus(response.message || 'Password updated successfully.');
    setActivePanel('login');
    setStatus('Password updated. You can login now.');
    document.getElementById('page-login-email').value = document.getElementById('page-forgot-email').value.trim();
    document.getElementById('page-login-password').value = '';
    document.getElementById('page-reset-new-password').value = '';
  } catch (error) {
    setForgotStatus(error.message, true);
  }
});

loginElements.logoutBtn.addEventListener('click', () => {
  clearAuth();
  setActivePanel('login');
  refreshPageState();
});

setActivePanel('login');
refreshPageState();
