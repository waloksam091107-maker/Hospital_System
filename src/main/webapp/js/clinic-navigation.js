(function () {
  'use strict';
  const match = location.pathname.match(/^(.*)\/(?:hospro|doctor-schedule)\//);
  if (!match) return;
  const context = match[1];
  fetch(context + '/portal/profile', { credentials: 'same-origin' }).then(async response => response.ok ? response.json() : null).then(profile => {
    if (!profile || !['PATIENT', 'DOCTOR', 'ADMIN'].includes(profile.role)) return;
    const nav = document.createElement('nav'); nav.id = 'clinicNavigation'; nav.setAttribute('aria-label', '门诊服务导航'); nav.style.cssText = 'display:flex;flex-wrap:wrap;gap:18px;padding:14px 24px;background:#eaf6f5;color:#16365c;position:relative;z-index:9';
    function link(label, path) { const a = document.createElement('a'); a.textContent = label; a.href = context + path; nav.appendChild(a); }
    link(profile.role === 'DOCTOR' ? '候诊、容量与停诊' : profile.role === 'ADMIN' ? '停诊审批' : '签到与候诊', '/clinic-workflow.html');
    link('站内通知', '/notifications.html');
    if (profile.role === 'ADMIN') link('账号安全管理', '/hospro/account-security.html');
    document.body.prepend(nav);
  }).catch(() => {});
}());
