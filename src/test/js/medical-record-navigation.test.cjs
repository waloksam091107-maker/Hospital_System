const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { test, before, after } = require('node:test');
const { chromium } = require('playwright');

let browser;
before(async () => { browser = await chromium.launch({ channel: 'msedge', headless: true }); });
after(async () => { await browser.close(); });

async function openRecords(query, detailStatus = 200) {
  const page = await browser.newPage();
  const requests = [], errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.route('http://hospital.test/**', route => {
    const url = new URL(route.request().url());
    if (url.pathname === '/hospital/medical-records/patient') {
      requests.push(url.pathname + url.search);
      return route.fulfill({ status: detailStatus, json: detailStatus === 200 ? {
        appointmentId: 812, doctorId: 4, recordStatus: 'FINAL', diagnosis: '通知对应的历史诊断 <script>bad()</script>',
        treatmentPlan: '历史诊疗方案', orders: [{ itemName: '历史检查项目', quantity: 1 }]
      } : { success: false, message: '未找到可访问的就诊记录。' } });
    }
    if (url.pathname === '/hospital/medical-records/patient/search') {
      requests.push(url.pathname + url.search);
      return route.fulfill({ json: { items: [{ appointmentId: 999, doctorId: 4, diagnosis: '最近诊断' }], total: 1, pages: 1, pageNum: 1 } });
    }
    if (url.pathname.includes('/notifications/')) return route.fulfill({ json: { unreadCount: 0 } });
    if (url.pathname === '/hospital/portal/profile') return route.fulfill({ json: { role: 'PATIENT', name: '患者' } });
    const file = path.join(process.cwd(), 'src/main/webapp', url.pathname.replace('/hospital/', ''));
    if (fs.existsSync(file) && fs.statSync(file).isFile()) return route.fulfill({ path: file });
    return route.fulfill({ status: 404, body: '' });
  });
  await page.goto('http://hospital.test/hospital/hospro/medical-records.html' + query);
  await page.waitForFunction(() => document.getElementById('records').getAttribute('aria-busy') === 'false');
  return { page, requests, errors };
}

test('notification deep link opens its exact finalized record and can return to search', async () => {
  const { page, requests, errors } = await openRecords('?appointmentId=812');
  try {
    assert.ok(requests.some(url => url === '/hospital/medical-records/patient?appointmentId=812'), 'deep link must request the notification record, not unrelated page one');
    assert.match(await page.locator('#records').innerText(), /预约 #812/);
    assert.match(await page.locator('#records').innerText(), /历史检查项目/);
    assert.equal(await page.locator('#records script').count(), 0, 'medical text must remain escaped');
    assert.doesNotMatch(await page.locator('#records').innerText(), /undefined/);
    await page.getByRole('button', { name: '查看全部就诊记录' }).click();
    await page.waitForFunction(() => document.getElementById('records').textContent.includes('最近诊断'));
    assert.ok(requests.some(url => url.includes('/patient/search')));
    assert.equal(new URL(page.url()).searchParams.has('appointmentId'), false);
    assert.deepEqual(errors, []);
  } finally { await page.close(); }
});

test('unavailable notification record stays a clear failure and never substitutes other records', async () => {
  const { page, requests, errors } = await openRecords('?appointmentId=812', 404);
  try {
    assert.match(await page.locator('#records').innerText(), /未找到可访问/);
    assert.equal(requests.filter(url => url.includes('/patient/search')).length, 0);
    await page.getByRole('button', { name: '重新加载' }).click();
    await page.waitForFunction(() => document.getElementById('records').getAttribute('aria-busy') === 'false');
    assert.equal(requests.filter(url => url === '/hospital/medical-records/patient?appointmentId=812').length, 2);
    assert.deepEqual(errors, []);
  } finally { await page.close(); }
});

test('ordinary medical-record search still opens normally', async () => {
  const { page, requests, errors } = await openRecords('');
  try {
    assert.match(await page.locator('#records').innerText(), /最近诊断/);
    assert.equal(requests.filter(url => url.includes('/patient/search')).length, 1);
    assert.deepEqual(errors, []);
  } finally { await page.close(); }
});
