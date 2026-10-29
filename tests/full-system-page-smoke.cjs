const { chromium } = require('playwright');
const fs = require('node:fs'), path = require('node:path');
const f = JSON.parse(fs.readFileSync('.hospital-local/five-extensions-http-fixture.json', 'utf8').replace(/^\uFEFF/, ''));
const base = process.env.HOSPITAL_SMOKE_BASE || 'http://127.0.0.1:18081/ustbhospital';
const out = path.resolve('target/full-system-ui');
fs.mkdirSync(out, { recursive: true });
const routes = [
 ['guest', 'login', '/login.html'],
 ['guest', 'register', '/hospro/register-page.html'],
 ['guest', 'forgot-password', '/hospro/forgot-password.html'],
 ['guest', 'public-home', '/hospro/index.html'],
 ['guest', 'public-announcements', '/hospro/news.html'],
 ['PATIENT', 'booking-confirmation', '/doctor-schedule/confirm.html'],
 ['PATIENT', 'home-patient', '/hospro/index.html'],
 ['PATIENT', 'doctor-directory', '/hospro/doctor-directory.html'],
 ['PATIENT', 'appointment-departments', '/hospro/appointment-departments.html'],
 ['PATIENT', 'doctor-details', '/hospro/doctors-details.html?doctorId=' + f.doctor],
 ['PATIENT', 'booking', '/hospro/book-appointment.html?doctor_id=' + f.doctor + '&date=' + f.tomorrow + '&time=AM'],
 ['PATIENT', 'patient-appointments', '/hospro/patient.jsp'],
 ['PATIENT', 'medical-records', '/hospro/medical-records.html'],
 ['PATIENT', 'patient-clinic', '/clinic-workflow.html'],
 ['PATIENT', 'patient-notifications', '/notifications.html'],
 ['PATIENT', 'patient-profile', '/hospro/portal-profile.html'],
 ['PATIENT', 'change-password', '/hospro/change-password.html'],
 ['DOCTOR', 'doctor-profile', '/hospro/doctor-profile.html?doctorId=' + f.doctor],
 ['DOCTOR', 'home-doctor', '/hospro/index.html'],
 ['DOCTOR', 'doctor-schedule', '/doctor-schedule/date.html'],
 ['DOCTOR', 'doctor-appointments', '/doctor-schedule/appointments.html'],
 ['DOCTOR', 'doctor-clinic', '/clinic-workflow.html'],
 ['DOCTOR', 'doctor-notifications', '/notifications.html'],
 ['DOCTOR', 'doctor-portal-profile', '/hospro/portal-profile.html'],
 ['ADMIN', 'admin-profile', '/hospro/portal-profile.html'],
 ['ADMIN', 'home-admin', '/hospro/index.html'],
 ['ADMIN', 'master-data', '/hospro/admin-master-data.html'],
 ['ADMIN', 'account-security', '/hospro/account-security.html'],
 ['ADMIN', 'audit', '/hospro/audit-logs.html'],
 ['ADMIN', 'announcements-management', '/hospro/admin-announcements.html'],
 ['ADMIN', 'income-report', '/hospro/sum.html'],
 ['ADMIN', 'admin-clinic', '/clinic-workflow.html'],
 ['ADMIN', 'admin-notifications', '/notifications.html']
];

(async () => {
 const browser = await chromium.launch({ channel: 'msedge', headless: true });
 const results = [];
 try {
  for (const [device, viewport] of [['desktop', { width: 1440, height: 1000 }], ['mobile', { width: 390, height: 844 }]]) {
   const contexts = {};
   for (const [role, code] of [['guest', null], ['PATIENT', 2], ['DOCTOR', 1], ['ADMIN', 3]]) {
    const context = await browser.newContext({ viewport, isMobile: device === 'mobile', hasTouch: device === 'mobile' });
    if (code) {
     const response = await context.request.post(base + '/auth/login', { form: { roleCode: String(code), user: f.prefix, password: f.password } });
     if (response.url().includes('login_failed')) throw new Error(role + ' fixture login failed');
    }
    contexts[role] = context;
   }
   for (const [role, name, route] of routes) {
    const page = await contexts[role].newPage();
    const entry = { device, role, name, route, pageErrors: [], failedAssets: [], expectedAuthResponses: [], requestFailures: [], brokenImages: [], overflow: [] };
    const responseChecks = [];
    const pendingRequests = new Map();
    page.on('request', request => pendingRequests.set(request, { url: request.url(), startedAt: Date.now() }));
    page.on('requestfinished', request => pendingRequests.delete(request));
    page.on('pageerror', e => entry.pageErrors.push(e.message));
    page.on('response', response => {
     if (response.request().isNavigationRequest()) entry.navigationStatus = response.status();
     if (response.status() < 400) return;
     const failure = { status: response.status(), url: response.url() };
     const anonymousAuth = role === 'guest' && name === 'public-home' && response.status() === 401
      && ['/portal/profile', '/notifications/unread-count'].some(path => response.url() === base + path);
     if (!anonymousAuth) { entry.failedAssets.push(failure); return; }
     responseChecks.push((async () => {
      try {
       const body = await response.json();
       if (body.code === 'LOGIN_REQUIRED' && body.success === false) entry.expectedAuthResponses.push(failure);
       else entry.failedAssets.push({ ...failure, reason: 'invalid anonymous authentication response' });
      } catch (error) { entry.failedAssets.push({ ...failure, reason: error.message }); }
     })());
    });
    page.on('requestfailed', request => {
     pendingRequests.delete(request);
     entry.requestFailures.push({ url: request.url(), error: request.failure().errorText });
    });
    try {
     const response = await page.goto(base + route, { waitUntil: 'networkidle', timeout: 20000 });
     entry.status = response.status(); entry.url = page.url();
     await page.locator('body').waitFor();
     await Promise.all(responseChecks);
     if (role === 'guest' && name === 'public-home') {
      entry.anonymousAccountNavigationHidden = await page.locator('#notificationCenterNavigation').isHidden();
      if (!entry.anonymousAccountNavigationHidden) entry.error = 'anonymous home must hide account navigation';
     }
     entry.brokenImages = await page.locator('img').evaluateAll(images => images.filter(i => i.complete && i.naturalWidth === 0 && i.getBoundingClientRect().width > 0).map(i => ({ src: i.currentSrc || i.src, id: i.id })));
     entry.overflow = await page.evaluate(() => {
      if (document.documentElement.scrollWidth <= innerWidth + 2) return [];
      return [...document.querySelectorAll('body *')].filter(e => { const r = e.getBoundingClientRect(), style = getComputedStyle(e); return r.width > 0 && (r.right > innerWidth + 2 || r.left < -2) && style.position !== 'fixed' && style.display !== 'none'; }).slice(0, 15).map(e => ({ tag: e.tagName, id: e.id, class: e.className, width: Math.round(e.getBoundingClientRect().width) }));
     });
     entry.documentWidth = await page.evaluate(() => document.documentElement.scrollWidth);
     await page.screenshot({ path: path.join(out, device + '-' + name + '.png'), fullPage: true, animations: 'disabled' });
    } catch (error) {
     entry.error = error.message;
     entry.pendingRequests = [...pendingRequests.values()].map(request => ({ url: request.url, elapsedMs: Date.now() - request.startedAt }));
     try {
      entry.bodyTextAtFailure = (await page.locator('body').innerText({ timeout: 2000 })).slice(0, 2000);
      await page.screenshot({ path: path.join(out, device + '-' + name + '-failure.png'), fullPage: true, animations: 'disabled', timeout: 3000 });
     } catch (diagnosticError) { entry.diagnosticError = diagnosticError.message; }
    }
    results.push(entry);
    console.log(JSON.stringify(entry));
    await page.close();
   }
   for (const context of Object.values(contexts)) await context.close();
  }
 } finally { await browser.close(); fs.writeFileSync(path.join(out, 'page-smoke.json'), JSON.stringify(results, null, 2)); }
 const bad = results.filter(r => r.error || r.status >= 400 || r.pageErrors.length || r.failedAssets.length || r.requestFailures.length || r.brokenImages.length || r.overflow.length);
 console.log('Page smoke: ' + results.length + ' pages, ' + bad.length + ' with findings. Screenshots: ' + out);
 if (bad.length) process.exitCode = 1;
})().catch(error => { console.error(error); process.exitCode = 1; });
