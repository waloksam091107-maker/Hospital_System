const fs = require('node:fs'), path = require('node:path');
const assert = require('node:assert/strict');
const { test, before, after } = require('node:test');
const { chromium } = require('playwright');
let browser;
before(async () => { browser = await chromium.launch({ channel: 'msedge', headless: true }); });
after(async () => { await browser.close(); });
const name = '医生 <img src=x onerror="window.untrustedExecuted=true">';
const doctor = { doctorId: 8, name, jobNumber: 'D8', phone: '123456', registrationFee: '10.00', introduction: '现有医生简介', avatar: null, departments: { departmentName: '内科' }, professionalTitles: { titleName: '主治医师' } };

async function open(route) {
 const page = await browser.newPage();
 const errors = [], badResponses = [], bookings = [];
 page.on('pageerror', error => errors.push(error.message));
 page.on('response', response => { if (response.status() >= 400) badResponses.push(response.url()); });
 await page.route('http://hospital.test/**', request => {
  const url = new URL(request.request().url());
  if (url.pathname.endsWith('/doc/findById') || url.pathname.endsWith('/doctors/details')) return request.fulfill({ json: route.includes('book-appointment') ? Object.assign({}, doctor, { name: '真实医生' }) : doctor });
  if (url.pathname.endsWith('/doc/findByDeptId')) return request.fulfill({ json: { list: [doctor], pages: 1, pageNum: 1 } });
  if (url.pathname.endsWith('/portal/profile')) return request.fulfill({ json: { role: 'PATIENT' } });
  if (url.pathname.endsWith('/notifications/unread-count')) return request.fulfill({ json: { unreadCount: 0 } });
  if (url.pathname.endsWith('/clinic/slots')) return request.fulfill({ json: { success: true, list: [] } });
  if (url.pathname.endsWith('/appointments/availability')) return request.fulfill({ json: { available: true } });
  if (url.pathname.endsWith('/security/csrf')) return request.fulfill({ json: { token: 'fixture-token' } });
  if (url.pathname.endsWith('/appointments/book')) { bookings.push(request.request().postData()); return request.fulfill({ status: 409, json: { success: false, message: '所选时段已满，请重新选择。' } }); }
  if (url.pathname.endsWith('/optional-map.html')) return request.fulfill({ contentType: 'text/html; charset=utf-8', body: '<meta charset="utf-8"><div class="google-map" id="test-map"></div><script src="hospro/static/js/jquery.js"></script><script src="hospro/static/js/gmaps.js"></script><script src="hospro/static/js/map-helper.js"></script>' });
  const file = path.resolve('src/main/webapp', url.pathname.replace('/hospital/', ''));
  if (fs.existsSync(file) && fs.statSync(file).isFile()) return request.fulfill({ path: file });
  return request.fulfill({ status: 404, body: '' });
 });
 await page.goto('http://hospital.test/hospital' + route, { waitUntil: 'networkidle' });
 return { page, errors, badResponses, bookings };
}

test('optional maps render honest unavailable feedback without an SDK import exception', async () => {
 const { page, errors } = await open('/optional-map.html');
 try {
  assert.deepEqual(errors, []);
  assert.match(await page.locator('.google-map').textContent(), /地图服务未配置/);
 } finally { await page.close(); }
});

test('doctor details consume actual JSON and show service metadata and safe default avatar', async () => {
 const { page, errors, badResponses } = await open('/hospro/doctors-details.html?doctorId=8');
 try {
  assert.deepEqual(errors, []);
  assert.equal(await page.locator('.name h2').textContent(), name);
  assert.match(await page.locator('[id="001"]').textContent(), /内科/);
  assert.equal(await page.locator('.image-box img').first().evaluate(img => img.complete && img.naturalWidth > 0), true);
  assert.equal(await page.evaluate(() => window.untrustedExecuted), undefined);
  assert.deepEqual(badResponses, []);
 } finally { await page.close(); }
});

test('doctor directory supports jQuery 3 and escapes server-provided doctor labels', async () => {
 const { page, errors, badResponses } = await open('/hospro/doctor-directory.html?departmentId=1');
 try {
  assert.deepEqual(errors, []);
  assert.equal(await page.locator('#clinicListContent .name a').textContent(), name);
  assert.equal(await page.evaluate(() => window.untrustedExecuted), undefined);
  assert.equal(await page.locator('#clinicListContent img').first().evaluate(img => img.complete && img.naturalWidth > 0), true);
  assert.deepEqual(badResponses, []);
 } finally { await page.close(); }
});

test('doctor detail presents real metadata and booking without fictional template services', async () => {
 const { page } = await open('/hospro/doctors-details.html?doctorId=8');
 try {
  assert.doesNotMatch(await page.locator('body').textContent(), /New York Medical College|New Patient Visit|Phasellus/);
  assert.equal(await page.locator('#datepicker').count(), 1);
  assert.equal(await page.locator('[name=appointmentTime]').count(), 3);
 } finally { await page.close(); }
});

test('booking confirmation is compact on mobile and uses existing balance payment only', async () => {
 const { page, errors } = await open('/hospro/book-appointment.html?doctor_id=8&date=2030-05-06&time=AM');
 try {
  await page.setViewportSize({ width: 390, height: 844 });
  assert.equal(await page.locator('[name=card_number],[name=ccv],[name=expiration_date]').count(), 0, 'booking uses account balance and must not ask for unused card details');
  assert.match(await page.locator('[id="004"]').textContent(), /10\.00/);
  await page.screenshot({ path: 'target/full-system-ui/mobile-booking-workspace.png', fullPage: true });
  assert.ok(await page.locator('[id="005"]').evaluate(button => button.getBoundingClientRect().bottom <= innerHeight), 'confirmation button should be reachable on the initial mobile screen');
  assert.deepEqual(errors, []);
 } finally { await page.close(); }
});

test('booking submits only once while busy and preserves server failure feedback', async () => {
 const { page, bookings } = await open('/hospro/book-appointment.html?doctor_id=8&date=2030-05-06&time=AM');
 try {
  await page.waitForFunction(() => !document.getElementById('005').disabled);
  await Promise.all([page.waitForResponse(response => response.url().endsWith('/appointments/book')), page.locator('[id="005"]').evaluate(button => { button.click(); button.click(); })]);
  await page.waitForFunction(() => document.getElementById('bookingMessage').textContent.includes('所选时段已满'));
  await page.waitForLoadState('networkidle');
  assert.equal(bookings.length, 1, 'a rapid second confirmation must not create a second booking request');
  assert.match(await page.locator('#bookingMessage').textContent(), /所选时段已满/);
  assert.equal(await page.locator('[id="005"]').isEnabled(), true, 'a rejected request must let the patient try again');
 } finally { await page.close(); }
});

test('doctor availability check carries the chosen canonical slot into confirmation', async () => {
 const { page } = await open('/hospro/doctors-details.html?doctorId=8');
 try {
  await page.locator('#datepicker').fill('2030-05-06');
  await page.locator('[name=appointmentTime][value=PM]').check();
  await page.locator('#book-btn').click();
  await page.waitForURL('**/book-appointment.html?doctor_id=8&date=2030-05-06&time=PM');
  await page.waitForLoadState('networkidle');
  assert.equal(await page.locator('#time').textContent(), '下午');
 } finally { await page.close(); }
});
