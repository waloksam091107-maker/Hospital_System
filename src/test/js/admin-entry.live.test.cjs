// Run against the isolated app only: HOSPITAL_ADMIN_ENTRY_LIVE=1 node --test this-file.
// Credentials remain in the ignored fixture; this test performs navigation only.
const fs = require('node:fs');
const assert = require('node:assert/strict');
const { test, before, after } = require('node:test');
const { chromium } = require('playwright');
const enabled = process.env.HOSPITAL_ADMIN_ENTRY_LIVE === '1';
const base = 'http://127.0.0.1:18081/ustbhospital';
let browser, fixture;
before(async () => {
  if (!enabled) return;
  fixture = JSON.parse(fs.readFileSync('.hospital-local/five-extensions-http-fixture.json', 'utf8').replace(/^\uFEFF/, ''));
  browser = await chromium.launch({ channel: 'msedge', headless: true });
});
after(async () => { if (browser) await browser.close(); });
async function session(roleCode) {
  const context = await browser.newContext();
  if (roleCode) {
    const login = await context.request.post(base + '/auth/login', { form: { user: fixture.prefix, password: fixture.password, roleCode }, maxRedirects: 0 });
    assert.equal(login.status(), 302, 'fixture login should redirect');
    const profile = await context.request.get(base + '/portal/profile');
    assert.equal(profile.status(), 200, 'fixture should establish a usable session');
    assert.equal((await profile.json()).role, roleCode === '3' ? 'ADMIN' : 'PATIENT');
  }
  return context;
}

test('admin landing preserves real management destinations inside the deployed context', { skip: !enabled }, async () => {
  const context = await session('3');
  try {
    const page = await context.newPage();
    await page.goto(base + '/hospro/adm_main.jsp');
    assert.equal(await page.locator('h1').textContent(), '管理中心');
    assert.doesNotMatch(await page.locator('body').textContent(), /2375|Total Patients|Total Staffs|New Appointments|Doctor Dashboard/);
    const destinations = {
      '基础数据维护': '/hospro/admin-master-data.html',
      '账号安全管理': '/hospro/account-security.html',
      '审计日志': '/hospro/audit-logs.html',
      '公告管理': '/hospro/admin-announcements.html',
      '收入报表': '/hospro/sum.html',
      '停诊审批': '/clinic-workflow.html',
      '个人资料': '/hospro/portal-profile.html',
      '返回首页': '/hospro/index.html'
    };
    for (const [label, route] of Object.entries(destinations)) {
      const link = page.getByRole('link', { name: label, exact: true });
      assert.equal(await link.evaluate(a => a.href), base + route, label + ' must retain the application context');
      const response = await context.request.get(base + route);
      assert.equal(response.status(), 200, label + ' destination must exist');
      assert.equal(response.url(), base + route, label + ' must not fall back to another page');
    }
    await page.setViewportSize({ width: 390, height: 844 });
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'management navigation must fit mobile width');
    await page.getByRole('link', { name: '基础数据维护', exact: true }).click();
    await page.getByRole('link', { name: '返回管理主页', exact: true }).click();
    assert.equal(await page.locator('h1').textContent(), '管理中心', 'master data must return to the hub');
  } finally { await context.close(); }
});

test('clinic admin back link and missing-page fallback return to the real hub', { skip: !enabled }, async () => {
  const context = await session('3');
  try {
    const page = await context.newPage();
    await page.goto(base + '/clinic-workflow.html');
    await page.getByRole('link', { name: '管理中心', exact: true }).click();
    assert.equal(await page.locator('h1').textContent(), '管理中心');
    await page.goto(base + '/hospro/retired-admin-destination.html');
    assert.equal(await page.locator('h1').textContent(), '管理中心');
  } finally { await context.close(); }
});

test('guest admin landing redirects to login inside the deployment context', { skip: !enabled }, async () => {
  const context = await session();
  try {
    const response = await context.request.get(base + '/hospro/adm_main.jsp', { maxRedirects: 0 });
    assert.equal(response.status(), 302);
    assert.equal(new URL(response.headers().location, base).pathname, '/ustbhospital/hospro/login.html');
  } finally { await context.close(); }
});

test('patient cannot select admin landing through URL role overrides', { skip: !enabled }, async () => {
  const context = await session('2');
  try {
    const response = await context.request.get(base + '/hospro/adm_main.jsp?role=ADMIN&roleCode=3', { maxRedirects: 0 });
    assert.equal(response.status(), 302);
    assert.equal(new URL(response.headers().location, base).pathname, '/ustbhospital/hospro/index.html');
  } finally { await context.close(); }
});
