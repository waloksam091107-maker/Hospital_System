// Concurrent anonymous requests must fail as authentication responses, never server errors.
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const base = 'http://127.0.0.1:18081/ustbhospital';
(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  let checks = 0;
  try {
    for (let round = 0; round < 8; round++) {
      const context = await browser.newContext();
      try {
        const page = await context.newPage(), errors = [], responses = [];
        page.on('pageerror', error => errors.push(error.message));
        await page.goto(base + '/login.html', { waitUntil: 'networkidle' });
        await page.goto(base + '/hospro/register-page.html', { waitUntil: 'networkidle' });
        await page.goto(base + '/hospro/forgot-password.html', { waitUntil: 'networkidle' });
        page.on('response', response => {
          if (response.status() >= 400) responses.push((async () => {
            let body;
            try { body = await response.json(); } catch { body = null; }
            return { url: response.url(), status: response.status(), body };
          })());
        });
        await page.goto(base + '/hospro/index.html', { waitUntil: 'networkidle' });
        for (const response of await Promise.all(responses)) {
          assert.ok(['/portal/profile', '/notifications/unread-count'].some(route => response.url === base + route), response.url);
          assert.equal(response.status, 401, response.url);
          assert.equal(response.body && response.body.code, 'LOGIN_REQUIRED', response.url);
          assert.equal(response.body.success, false, response.url);
          checks++;
        }
        assert.ok(responses.length >= 3, 'home must check its session-owned profile and notification badge');
        assert.equal(await page.locator('#notificationCenterNavigation').isHidden(), true);
        assert.deepEqual(errors, []);
        checks += 2;
        const concurrent = await Promise.all(['/portal/profile', '/notifications/unread-count', '/portal/profile'].map(route => context.request.get(base + route)));
        for (const response of concurrent) {
          assert.equal(response.status(), 401, response.url());
          assert.equal((await response.json()).code, 'LOGIN_REQUIRED');
          checks++;
        }
      } finally { await context.close(); }
    }
    console.log('PASS ' + checks + ' concurrent anonymous authentication checks across 8 fresh guest sessions; home hides account navigation and has no page errors');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
