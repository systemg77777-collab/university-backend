let isFromSchool = true;

function showPanel(id) {
  document.querySelectorAll('.auth-panel').forEach(p => p.classList.remove('active'));
  const target = document.getElementById(id);
  if (target) target.classList.add('active');
}

function showStudentForm(fromSchool) {
  isFromSchool = fromSchool;
  showPanel(fromSchool ? 'panel-student-school' : 'panel-student-external');
}

function togglePw(id, btn) {
  const input = document.getElementById(id);
  if (!input) return;
  input.type = input.type === 'password' ? 'text' : 'password';
  btn.textContent = input.type === 'password' ? '👁️' : '🙈';
}

function previewProfilePhoto(input, previewId) {
  const file = input.files[0];
  const preview = document.getElementById(previewId);
  if (!file || !preview) return;
  const reader = new FileReader();
  reader.onload = e => { preview.innerHTML = `<img src="${e.target.result}" alt="Preview"/>`; };
  reader.readAsDataURL(file);
}

document.getElementById('login-email')?.addEventListener('blur', function () {
  if (this.value.trim().toLowerCase() === 'superadmin') {
    const pw = document.getElementById('login-password');
    if (pw) pw.addEventListener('blur', checkSecretApply);
  }
});

function checkSecretApply() {
  const email = document.getElementById('login-email').value.trim().toLowerCase();
  const pw    = document.getElementById('login-password').value.trim().toLowerCase();
  if (email === 'superadmin' && pw === 'apply') {
    showPanel('panel-superadmin-apply');
  }
}

document.getElementById('form-login')?.addEventListener('submit', async function (e) {
  e.preventDefault();
  const btn = document.getElementById('login-btn');
  btn.textContent = 'Logging in...'; btn.disabled = true;
  const data = await api.post('/api/auth/login', {
    email:    document.getElementById('login-email').value.trim(),
    password: document.getElementById('login-password').value
  });
  btn.textContent = 'Log In →'; btn.disabled = false;
  if (!data) return;
  if (data.error) { showToast(data.error, 'error'); return; }
  showToast('Welcome back, ' + data.name + '! 👋', 'success');
  setTimeout(() => window.location.href = data.redirect, 800);
});

async function submitStudentSchool(e) {
  e.preventDefault();
  const form = e.target;
  const pw   = form.password.value;
  const cpw  = form.confirmPassword.value;
  if (pw !== cpw) { showToast('Passwords do not match!', 'error'); return; }

  const fd = new FormData();
  fd.append('fromSchool', 'true');
  fd.append('fullName',      form.fullName.value);
  fd.append('email',         form.email.value);
  fd.append('idNumber',      form.idNumber.value);
  fd.append('program',       form.program.value);
  fd.append('yearLevel',     '4');
  fd.append('section',       form.section.value);
  fd.append('requiredHours', form.requiredHours.value);
  fd.append('password',      pw);
  const profilePhoto = form.profilePhoto?.files[0];
  if (profilePhoto) fd.append('profilePhoto', profilePhoto);

  const btn = form.querySelector('button[type=submit]');
  btn.textContent = 'Creating...'; btn.disabled = true;
  const data = await api.postForm('/api/auth/register/student', fd);
  btn.textContent = 'Create Account 🎓'; btn.disabled = false;

  if (!data) return;
  if (data.error) { showToast(data.error, 'error'); return; }
  showSuccessPanel('Account Created! 🎓', 'Welcome to UNI-Versity! You can now log in.');
}

async function submitStudentExternal(e) {
  e.preventDefault();
  const form = e.target;
  const pw   = form.password.value;
  const cpw  = form.confirmPassword.value;
  if (pw !== cpw) { showToast('Passwords do not match!', 'error'); return; }

  const fd = new FormData(form);
  fd.append('fromSchool', 'false');
  fd.append('yearLevel', '4');
  fd.set('password', pw);
  fd.delete('confirmPassword');
  const profilePhoto = form.profilePhoto?.files[0];
  if (profilePhoto) fd.set('profilePhoto', profilePhoto);

  const btn = form.querySelector('button[type=submit]');
  btn.textContent = 'Creating...'; btn.disabled = true;
  const data = await api.postForm('/api/auth/register/student', fd);
  btn.textContent = 'Create Account 🎓'; btn.disabled = false;

  if (!data) return;
  if (data.error) { showToast(data.error, 'error'); return; }
  showSuccessPanel('Account Created! 🎓', 'Welcome to UNI-Versity! You can now log in.');
}

async function submitAdminRequest(e) {
  e.preventDefault();
  const form = e.target;
  const btn  = form.querySelector('button[type=submit]');
  btn.textContent = 'Submitting...'; btn.disabled = true;
  const data = await api.post('/api/auth/register/admin-request', {
    fullName: form.fullName.value,
    email:    form.email.value,
    idNumber: form.idNumber.value,
    institute: form.institute.value,
    password: form.password.value
  });
  btn.textContent = 'Submit Request 🛠️'; btn.disabled = false;
  if (!data) return;
  if (data.error) { showToast(data.error, 'error'); return; }
  showSuccessPanel('Request Submitted! 🛠️', 'Your admin account request has been sent for approval.');
}

async function submitSuperAdminApply(e) {
  e.preventDefault();
  const form = e.target;
  const btn  = form.querySelector('button[type=submit]');
  btn.textContent = 'Submitting...'; btn.disabled = true;
  const data = await api.post('/api/auth/register/superadmin-apply', {
    fullName: form.fullName.value,
    email:    form.email.value,
    idNumber: form.idNumber.value,
    position: form.position.value,
    institute: form.institute.value,
    password: form.password.value
  });
  btn.textContent = 'Submit Application'; btn.disabled = false;
  if (!data) return;
  if (data.error) { showToast(data.error, 'error'); return; }
  showSuccessPanel('Application Submitted!', 'Your SuperAdmin application is awaiting approval.');
}

function showSuccessPanel(title, msg) {
  document.getElementById('success-title').textContent = title;
  document.getElementById('success-msg').textContent   = msg;
  showPanel('panel-success');
}

(function initParticles() {
  const canvas = document.getElementById('particles-canvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  let W, H, particles;

  function resize() { W = canvas.width = window.innerWidth; H = canvas.height = window.innerHeight; }
  window.addEventListener('resize', resize); resize();

  function mkParticle() {
    return { x: Math.random()*W, y: Math.random()*H,
             r: Math.random()*2+0.5, vx: (Math.random()-.5)*0.4, vy: (Math.random()-.5)*0.4,
             o: Math.random()*0.5+0.1 };
  }
  particles = Array.from({length: 80}, mkParticle);

  function draw() {
    ctx.clearRect(0,0,W,H);
    particles.forEach(p => {
      p.x += p.vx; p.y += p.vy;
      if (p.x < 0) p.x = W; if (p.x > W) p.x = 0;
      if (p.y < 0) p.y = H; if (p.y > H) p.y = 0;
      ctx.beginPath(); ctx.arc(p.x, p.y, p.r, 0, Math.PI*2);
      ctx.fillStyle = `rgba(167,139,250,${p.o})`; ctx.fill();
    });
    for (let i = 0; i < particles.length; i++) {
      for (let j = i+1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x;
        const dy = particles[i].y - particles[j].y;
        const dist = Math.sqrt(dx*dx + dy*dy);
        if (dist < 120) {
          ctx.beginPath();
          ctx.moveTo(particles[i].x, particles[i].y);
          ctx.lineTo(particles[j].x, particles[j].y);
          ctx.strokeStyle = `rgba(108,60,225,${0.15*(1-dist/120)})`;
          ctx.lineWidth = 0.5; ctx.stroke();
        }
      }
    }
    requestAnimationFrame(draw);
  }
  draw();
})();

window.addEventListener('DOMContentLoaded', () => {
  const params = new URLSearchParams(window.location.search);
  if (params.get('googleEmail')) {
    showPanel('panel-signup-student');
    showToast('Google account detected. Please complete your profile.', 'info');
  }
});

function showForgotPasswordModal() {
  document.getElementById('forgot-password-email').value = '';
  openModal('modal-forgot-password');
}

async function submitForgotPassword(e) {
  e.preventDefault();
  const btn = document.getElementById('btn-forgot-password');
  const email = document.getElementById('forgot-password-email').value;

  btn.textContent = 'Sending...';
  btn.disabled = true;

  const data = await api.post('/api/auth/forgot-password', { email });

  btn.textContent = 'Send Reset Link';
  btn.disabled = false;

  if (!data) return;
  if (data.error) {
    showToast(data.error, 'error');
    return;
  }

  showToast(data.message, 'success');
  closeModal('modal-forgot-password');
}
