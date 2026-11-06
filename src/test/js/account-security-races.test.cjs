const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');
let browser;
before(async () => { browser = await chromium.launch({ channel: 'msedge', headless: true }); });
after(async () => { await browser.close(); });
const account = role => ({ id: role === 'DOCTOR' ? 11 : 22, name: role === 'DOCTOR' ? '医生甲' : '患者乙', enabled: true });
async function open(options = {}) {
  const page = await browser.newPage({ viewport: options.viewport || { width: 1280, height: 900 } });
  const requests = [], mutations = [];
  let intercept;
  await page.route('http://hospital.test/**', async route => {
    const url = new URL(route.request().url());
    if (url.pathname.endsWith('/list')) {
      const role = url.searchParams.get('role'); requests.push(role);
      if (intercept && await intercept(route, role)) return;
      return route.fulfill({ json: { success: true, items: [options.row || account(role)] } });
    }
    if (url.pathname.endsWith('/security/csrf')) return route.fulfill({ json: { token: 'test-token' } });
    if (route.request().method() === 'POST') {
      mutations.push(Object.fromEntries(new URLSearchParams(route.request().postData())));
      return route.fulfill({ json: { success: true, message: '已更新' } });
    }
    const file = path.join(process.cwd(), 'src/main/webapp', url.pathname.replace('/hospital/', ''));
    return fs.existsSync(file) ? route.fulfill({ path: file }) : route.fulfill({ status: 404 });
  });
  await page.goto('http://hospital.test/hospital/hospro/account-security.html');
  await page.locator('#accounts button').first().waitFor();
  return { page, requests, mutations, intercept: handler => { intercept = handler; } };
}
test('role switch cannot operate on stale rows while the next list is loading or fails', async () => {
  const s = await open(); let pending;
  s.intercept((route, role) => { if (role === 'PATIENT') { pending = route; return true; } });
  try {
    await s.page.locator('#role').selectOption('PATIENT');
    await s.page.waitForFunction(() => document.querySelector('#role').value === 'PATIENT');
    assert.equal(await s.page.locator('#accounts button:enabled').count(), 0, 'old doctor actions remain active under patient role');
    while (!pending) await new Promise(resolve => setTimeout(resolve, 10));
    await pending.fulfill({ status: 503, json: { success: false, message: '暂时无法读取' } });
    await s.page.getByText('暂时无法读取', { exact: true }).waitFor();
    assert.equal(await s.page.locator('#accounts button:enabled').count(), 0);
    assert.deepEqual(s.mutations, []);
  } finally { await s.page.close(); }
});
test('late doctor response cannot replace a newer patient list or change its action identity', async () => {
  const s = await open(); let pending;
  s.intercept((route, role) => { if (role === 'DOCTOR') { pending = route; return true; } });
  try {
    await s.page.locator('#refresh').click();
    while (!pending) await new Promise(resolve => setTimeout(resolve, 10));
    await s.page.locator('#role').selectOption('PATIENT');
    await s.page.getByText('患者乙', { exact: true }).waitFor();
    await pending.fulfill({ json: { success: true, items: [account('DOCTOR')] } });
    await s.page.waitForLoadState('networkidle');
    assert.match(await s.page.locator('#accounts').innerText(), /患者乙/);
    await s.page.getByRole('button', { name: '停用', exact: true }).click();
    await s.page.getByText('已更新', { exact: true }).waitFor();
    assert.equal(s.mutations[0].role, 'PATIENT');
    assert.equal(s.mutations[0].accountId, '22');
  } finally { await s.page.close(); }
});
test('account table and password reset remain within a narrow mobile viewport', async () => {
  const s = await open({ viewport: { width: 390, height: 844 }, row: { ...account('DOCTOR'), name: 'a'.repeat(80), lockedUntil: '2030-05-06T12:00:00+08:00' } });
  try {
    await s.page.getByRole('button', { name: '重置密码', exact: true }).click();
    assert.ok(await s.page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 2), 'page overflows horizontally');
    const submit = s.page.locator('#reset-form button[type=submit]');
    await submit.scrollIntoViewIfNeeded();
    const box = await submit.boundingBox();
    assert.ok(box.x >= 0 && box.x + box.width <= 392, 'reset submit is clipped');
  } finally { await s.page.close(); }
});
