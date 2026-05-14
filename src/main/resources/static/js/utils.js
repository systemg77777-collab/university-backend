const api = {
  async get(url) {
    const res = await fetch(url, { credentials: 'include' });
    if (res.status === 401 || res.status === 403) { redirectToLogin(); return null; }
    return res.json();
  },
  async post(url, body) {
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(body)
    });
    if (res.status === 401 || res.status === 403) { redirectToLogin(); return null; }
    return res.json();
  },
  async postForm(url, formData) {
    const res = await fetch(url, {
      method: 'POST',
      credentials: 'include',
      body: formData
    });
    if (res.status === 401 || res.status === 403) { redirectToLogin(); return null; }
    return res.json();
  },
  async put(url, body) {
    const res = await fetch(url, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(body)
    });
    return res.json();
  }
};

function showToast(message, type = 'info', duration = 4000) {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }
  const icons = { success: '✅', error: '❌', warning: '⚠️', info: '💬' };
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `<span>${icons[type] || '💬'}</span><span>${message}</span>`;
  container.appendChild(toast);
  requestAnimationFrame(() => {
    setTimeout(() => toast.classList.add('show'), 10);
    setTimeout(() => {
      toast.classList.remove('show');
      setTimeout(() => toast.remove(), 400);
    }, duration);
  });
}

function initThemeToggle() {
  const saved = localStorage.getItem('uni-theme');
  if (saved === 'dark') {
    document.documentElement.setAttribute('data-theme', 'dark');
  }
  updateThemeIcons();
}

function toggleTheme() {
  const html = document.documentElement;
  const isDark = html.getAttribute('data-theme') === 'dark';
  if (isDark) {
    html.removeAttribute('data-theme');
    localStorage.setItem('uni-theme', 'light');
  } else {
    html.setAttribute('data-theme', 'dark');
    localStorage.setItem('uni-theme', 'dark');
  }
  updateThemeIcons();
}

function updateThemeIcons() {
  const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
  document.querySelectorAll('.theme-icon').forEach(el => {
    el.textContent = isDark ? '🌙' : '☀️';
  });
}

function updateDateTime(elementId = 'datetime-display') {
  const el = document.getElementById(elementId);
  if (!el) return;
  const update = () => {
    const now = new Date();
    const opts = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
    const timeOpts = { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true };
    el.innerHTML = `
      <span class="date">${now.toLocaleDateString('en-PH', opts)}</span>
      <span style="color:var(--text-dim)">|</span>
      <span id="live-time">${now.toLocaleTimeString('en-PH', timeOpts)}</span>
    `;
  };
  update();
  setInterval(() => {
    const timeEl = document.getElementById('live-time');
    if (timeEl) {
      const now = new Date();
      const timeOpts = { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true };
      timeEl.textContent = now.toLocaleTimeString('en-PH', timeOpts);
    }
  }, 1000);
}

async function loadMotivationalQuote(elementId = 'quote-text') {
  const el = document.getElementById(elementId);
  if (!el) return;

  const fallback = [
    { text: "The secret of getting ahead is getting started.", author: "Mark Twain" },
    { text: "It always seems impossible until it's done.", author: "Nelson Mandela" },
    { text: "Don't watch the clock; do what it does. Keep going.", author: "Sam Levenson" },
    { text: "Success is the sum of small efforts repeated day in and day out.", author: "Robert Collier" },
    { text: "The future depends on what you do today.", author: "Mahatma Gandhi" },
    { text: "Believe you can and you're halfway there.", author: "Theodore Roosevelt" },
    { text: "Hard work beats talent when talent doesn't work hard.", author: "Tim Notke" },
    { text: "Your only limit is your mind.", author: "Unknown" },
  ];

  try {
    const res = await fetch('https://api.quotable.io/random?tags=inspirational,success&maxLength=120', {
      signal: AbortSignal.timeout(3000)
    });
    if (res.ok) {
      const data = await res.json();
      el.innerHTML = `"${data.content}" <strong>— ${data.author}</strong>`;
      return;
    }
  } catch (_) {}

  const q = fallback[Math.floor(Math.random() * fallback.length)];
  el.innerHTML = `"${q.text}" <strong>— ${q.author}</strong>`;
}

function redirectToLogin() {
  window.location.href = '/';
}

async function logout() {
  await api.post('/api/auth/logout', {});
  document.cookie = 'jwt=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
  window.location.href = '/';
}

function openModal(id) {
  const overlay = document.getElementById(id);
  if (overlay) overlay.classList.add('open');
}
function closeModal(id) {
  const overlay = document.getElementById(id);
  if (overlay) overlay.classList.remove('open');
}
document.addEventListener('click', (e) => {
  if (e.target.classList.contains('modal-overlay')) {
    e.target.classList.remove('open');
  }
});

function initSidebar() {
  const hamburger = document.getElementById('hamburger');
  const sidebar = document.getElementById('sidebar');
  if (hamburger && sidebar) {
    hamburger.addEventListener('click', () => sidebar.classList.toggle('open'));
  }
}

function animateProgressBar(fillEl, percent) {
  fillEl.style.width = '0%';
  requestAnimationFrame(() => {
    setTimeout(() => { fillEl.style.width = Math.min(percent, 100) + '%'; }, 50);
  });
}

function formatHours(h) {
  if (h === null || h === undefined) return '0.00';
  return parseFloat(h).toFixed(2);
}

function getInitials(name) {
  if (!name) return '?';
  return name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
}

function renderAvatar(photoPath, name, sizeClass = '') {
  if (photoPath) {
    return `<img src="/uploads/${photoPath}" alt="${name}" class="avatar ${sizeClass}" onerror="this.outerHTML=renderAvatarPlaceholder('${name}','${sizeClass}')"/>`;
  }
  return renderAvatarPlaceholder(name, sizeClass);
}

function renderAvatarPlaceholder(name, sizeClass = '') {
  return `<div class="avatar-placeholder ${sizeClass}">${getInitials(name)}</div>`;
}

function getStatusBadge(status) {
  const map = {
    'APPROVED':      '<span class="badge badge-success">✓ Approved</span>',
    'APPROVED_LATE': '<span class="badge badge-warning">⏰ Late</span>',
    'DENIED':        '<span class="badge badge-danger">✗ Denied</span>',
    'PENDING':       '<span class="badge badge-pending">⋯ Pending</span>',
    'ACTIVE':        '<span class="badge badge-success">● Active</span>',
    'INACTIVE':      '<span class="badge badge-muted">○ Inactive</span>',
    'COMPLETED':     '<span class="badge badge-success">🎓 Completed</span>',
    'ONLINE':        '<span class="badge badge-online">● Online</span>',
    'OFFLINE':       '<span class="badge badge-offline">○ Offline</span>',
  };
  return map[status] || `<span class="badge badge-muted">${status}</span>`;
}

function chartGradient(ctx, color1 = '#6C3CE1', color2 = 'transparent') {
  const gradient = ctx.createLinearGradient(0, 0, 0, 300);
  gradient.addColorStop(0, color1 + '80');
  gradient.addColorStop(1, color2 + '00');
  return gradient;
}

function getChartColors() {
  const style = getComputedStyle(document.documentElement);
  return {
    grid: style.getPropertyValue('--chart-grid').trim() || 'rgba(0,0,0,0.06)',
    text: style.getPropertyValue('--chart-text').trim() || '#64748B',
  };
}

document.addEventListener('DOMContentLoaded', () => {
  initThemeToggle();
  initSidebar();
  updateDateTime();
  loadMotivationalQuote();
});
