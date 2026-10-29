const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { test, before, after } = require('node:test');
const { chromium } = require('playwright');
let browser;
before(async () => { browser = await chromium.launch({ channel: 'msedge', headless: true }); });
after(async () => { await browser.close(); });

async function openClinic(role) {
  const page = await browser.newPage();
  await page.clock.install({ time: new Date('2030-05-06T01:00:00Z') });
  const queues = [], errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.route('http://hospital.test/**', route => {
    const url = new URL(route.request().url());
    if (url.pathname === '/hospital/portal/profile') return route.fulfill({ json: { role, name: '测试账号' } });
    if (url.pathname === '/hospital/security/csrf') return route.fulfill({ json: { token: 'test-token' } });
    if (url.pathname === '/hospital/clinic/queue') {
      queues.push(url.searchParams.toString());
      return route.fulfill({ json: { success: true, list: [
        { appointmentId: 1, appointmentDate: '2030-05-05', appointmentTime: 'AM', status: 'BOOKED', queueStatus: 'SKIPPED' },
        { appointmentId: 2, appointmentDate: '2030-05-06', appointmentTime: 'AM', status: 'COMPLETED', queueStatus: 'DONE', leaveBlocked: true },
        { appointmentId: 3, appointmentDate: '2030-05-06', appointmentTime: 'AM', status: 'BOOKED', queueStatus: 'SKIPPED' }
      ] } });
    }
    if (url.pathname.startsWith('/hospital/clinic/')) return route.fulfill({ json: { success: true, list: [] } });
    if (url.pathname.includes('/notifications/')) return route.fulfill({ json: { unreadCount: 0 } });
    const file = path.join(process.cwd(), 'src/main/webapp', url.pathname.replace('/hospital/', ''));
    if (fs.existsSync(file) && fs.statSync(file).isFile()) return route.fulfill({ path: file });
    return route.fulfill({ status: 404, body: '' });
  });
  await page.goto('http://hospital.test/hospital/clinic-workflow.html');
  await page.waitForFunction(() => document.querySelectorAll('#queueRows tr').length === 3);
  return { page, queues, errors };
}

test('doctor only sees requeue action while the skipped visit can still be restored', async () => {
  const { page, errors } = await openClinic('DOCTOR');
  try {
    assert.equal(await page.locator('#queueRows tr').nth(0).getByRole('button', { name: '恢复候诊' }).count(), 0, 'expired visit cannot be restored');
    assert.equal(await page.locator('#queueRows tr').nth(2).getByRole('button', { name: '恢复候诊' }).count(), 1, 'same-day open visit can still be restored');
    assert.deepEqual(errors, []);
  } finally { await page.close(); }
});

test('completed visits keep their terminal status when a leave overlaps the slot', async () => {
  const { page, errors } = await openClinic('PATIENT');
  try {
    const text = await page.locator('#queueRows tr').nth(1).innerText();
    assert.match(text, /已完成/);
    assert.doesNotMatch(text, /请改期或取消/);
    assert.deepEqual(errors, []);
  } finally { await page.close(); }
});

test('patient queue refresh applies the date and period displayed in the filters', async () => {
  const { page, queues, errors } = await openClinic('PATIENT');
  try {
    await page.locator('#queueDate').fill('2030-05-07');
    await page.locator('#queuePeriod').selectOption('PM');
    await Promise.all([page.waitForResponse(response => response.url().includes('/clinic/queue')), page.getByRole('button', { name: '刷新队列' }).click()]);
    await page.waitForFunction(() => document.querySelectorAll('#queueRows tr').length === 3);
    const query = new URLSearchParams(queues.at(-1));
    assert.equal(query.get('date'), '2030-05-07');
    assert.equal(query.get('period'), 'PM');
    assert.deepEqual(errors, []);
  } finally { await page.close(); }
});

test('doctor leave reason input enforces the service limit before submission', async () => {
  const { page } = await openClinic('DOCTOR');
  try {
    await page.locator('#leaveReason').fill('原'.repeat(499));
    await page.locator('#leaveReason').press('End');
    await page.locator('#leaveReason').pressSequentially('因因');
    assert.equal((await page.locator('#leaveReason').inputValue()).length, 500, 'leave service accepts at most 500 characters');
  } finally { await page.close(); }
});

test('patient initially sees every period today so an evening check-in remains discoverable', async () => {
  const { page, queues } = await openClinic('PATIENT');
  try {
    const query = new URLSearchParams(queues[0]);
    assert.equal(query.get('date'), '2030-05-06');
    assert.equal(query.get('period'), '', 'patient queue must not silently default to morning only');
    assert.equal(await page.locator('#queuePeriod').inputValue(), '');
  } finally { await page.close(); }
});
