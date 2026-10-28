// Real browser regression: NODE_PATH must contain Playwright.
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const webRoot = path.resolve(__dirname, '../src/main/webapp');
const origin = 'http://hospital.test';
const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai' }).format(new Date());
async function open(browser, role, mutations, extra = '', pagePath = 'clinic-workflow.html') {
  const page = await browser.newPage();
  await page.clock.install({ time: new Date(today + 'T09:00:00+08:00') });
  page.on('dialog', d => d.accept(d.type() === 'prompt' ? '审核意见' : undefined));
  await page.route('**/*', async route => {
    const url = new URL(route.request().url());
    const api = url.pathname.replace('/ustbhospital', '');
    const json = (body, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
    if (api === '/portal/profile') return json({ role, doctorId: 5, patientId: 2, name: '当前用户' });
    if (api === '/security/csrf') return json({ token: 'clinic-csrf' });
    if (api === '/patients/current') return json({ patientId: 2, name: '当前用户' });
    if (api === '/first') return json([]);
    if (api === '/doctors/details') return json({ doctorId: 5, name: '医生' });
    if (api === '/appointments/patient' || api === '/appointments/doctor') return json({ list: [
      { appointmentId: 21, doctors: { name: '医生' }, departments: { departmentName: '内科' }, patients: { name: '患者' }, appointmentDate: today, appointmentTime: 'AM', appointmentSequence: 9, queueStatus: 'WAITING', status: 'BOOKED' },
      { appointmentId: 22, doctors: { name: '医生' }, departments: { departmentName: '内科' }, patients: { name: '患者' }, appointmentDate: today, appointmentTime: 'AM', appointmentSequence: 10, queueStatus: 'CALLED', status: 'BOOKED' }
    ], pages: 1, pageNum: 1 });
    if (api.startsWith('/clinic/')) {
      if (route.request().method() === 'POST') {
        mutations.push({ path: api, data: Object.fromEntries(new URLSearchParams(route.request().postData())), token: (await route.request().allHeaders())['x-csrf-token'] });
        if (mutations.fail) return json({ success: false, message: '容量不得少于已占用数量' }, 409);
        return json({ success: true, message: '操作成功' });
      }
      if (api === '/clinic/slots') return json({ success: true, list: [{ scheduleSlotId: 7, scheduleDate: today, slotPeriod: 'AM', slotStatus: 'OPEN', capacity: 10, occupiedCount: 2, remaining: 8 }] });
      if (api === '/clinic/leave') return json({ success: true, list: [{ leaveId: 11, doctorId: 5, startDate: today, endDate: today, periods: 'AM,PM', reason: '<img src=x onerror=alert(1)>', status: 'PENDING', affectedAppointments: 2, affectedSlots: 2 }] });
      return json({ success: true, list: [
        { appointmentId: 21, patientName: '<img src=x onerror=alert(1)>', appointmentDate: today, appointmentTime: 'AM', appointmentSequence: 9, status: 'BOOKED', queueStatus: role === 'DOCTOR' ? 'CALLED' : 'NOT_CHECKED_IN' },
        { appointmentId: 22, patientName: '停诊患者', appointmentDate: today, appointmentTime: 'AM', appointmentSequence: 10, status: 'BOOKED', queueStatus: 'NOT_CHECKED_IN', leaveBlocked: true }
      ] });
    }
    if (api === '/appointments/start') { mutations.push({ path: api }); return json({ success: true }); }
    const file = path.resolve(webRoot, decodeURIComponent(api).replace(/^\//, ''));
    if (!file.startsWith(webRoot + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) return route.fulfill({ status: 404, body: 'Not found' });
    const ext = path.extname(file);
    const body = ext === '.jsp' ? fs.readFileSync(file, 'utf8').replace(/<%@[^%]*%>/g, '') : fs.readFileSync(file);
    return route.fulfill({ contentType: { '.jsp': 'text/html', '.html': 'text/html', '.js': 'application/javascript', '.css': 'text/css' }[ext] || 'application/octet-stream', body });
  });
  await page.goto(origin + '/ustbhospital/' + pagePath + extra);
  return page;
}
async function run() {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.PLAYWRIGHT_BROWSER_PATH || 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe' });
  let failures = 0;
  const tests = [
    ['患者看到预约号、候诊状态和停诊，签到携带 CSRF', 'PATIENT', async (p, m) => {
      await p.getByText('预约号 9', { exact: true }).waitFor({ timeout: 2500 });
      assert.match(await p.locator('#queueRows').innerText(), /未签到/);
      assert.match(await p.locator('#queueRows').innerText(), /停诊/);
      assert.equal(await p.locator('#queueRows img').count(), 0);
      assert.equal(await p.getByRole('button', { name: '签到', exact: true }).count(), 1);
      await p.getByRole('button', { name: '签到', exact: true }).click();
      await p.waitForTimeout(80);
      assert.deepEqual(m[0], { path: '/clinic/queue', data: { action: 'check-in', appointmentId: '21' }, token: 'clinic-csrf' });
      assert.equal(await p.locator('#capacityPanel').isVisible(), false);
    }],
    ['医生按日期时段叫号并可安全编辑容量', 'DOCTOR', async (p, m) => {
      await p.getByRole('button', { name: '保存容量' }).waitFor({ timeout: 2500 });
      assert.match(await p.locator('#slotRows').innerText(), /余号 8/);
      await p.getByRole('button', { name: '叫下一位' }).click();
      await p.waitForTimeout(80);
      assert.deepEqual(m[0].data, { action: 'call-next', date: today, period: 'AM' });
      await p.locator('[name=capacity]').fill('12');
      m.fail = true;
      await p.getByRole('button', { name: '保存容量' }).click();
      await p.getByRole('alert').filter({ hasText: '容量不得少于' }).waitFor();
      assert.equal(m[1].token, 'clinic-csrf');
      assert.deepEqual(m[1].data, { action: 'capacity', slotId: '7', capacity: '12' });
    }],
    ['医生提交停诊日期、时段与原因', 'DOCTOR', async (p, m) => {
      await p.locator('#leaveReason').waitFor({ timeout: 2500 });
      await p.locator('#leaveStart').fill(today); await p.locator('#leaveEnd').fill(today);
      await p.locator('#leaveReason').fill('学术会议');
      await p.getByRole('button', { name: '提交停诊申请' }).click();
      await p.waitForTimeout(80);
      assert.deepEqual(m[0].data, { action: 'apply', startDate: today, endDate: today, periods: 'AM', reason: '学术会议' });
    }],
    ['管理员角色由会话决定，显示影响数量与审核操作', 'ADMIN', async (p, m) => {
      await p.getByRole('button', { name: '批准', exact: true }).waitFor({ timeout: 2500 });
      assert.match(await p.locator('#leaveRows').innerText(), /影响预约：2/);
      assert.equal(await p.locator('#leaveRows img').count(), 0);
      assert.equal(await p.locator('#leaveApplyPanel').isVisible(), false);
      assert.equal(await p.locator('#queuePanel').isVisible(), false);
      await p.getByRole('button', { name: '批准', exact: true }).click();
      await p.waitForTimeout(80);
      assert.deepEqual(m[0].data, { action: 'approve', id: '11', reason: '审核意见' });
    }, '?role=DOCTOR'],
    ['患者原预约页显示预约号与候诊状态', 'PATIENT', async p => {
      await p.locator('#appBody tr').first().waitFor();
      assert.match(await p.locator('#appBody tr').first().innerText(), /预约号 9/);
      assert.match(await p.locator('#appBody tr').first().innerText(), /候诊中/);
    }, '', 'hospro/patient.jsp'],
    ['医生原预约页只允许已叫号患者开始接诊', 'DOCTOR', async p => {
      await p.locator('#appointmentRows tr').first().waitFor();
      assert.equal(await p.locator('#appointmentRows tr').nth(0).getByRole('button', { name: '开始接诊' }).count(), 0);
      assert.equal(await p.locator('#appointmentRows tr').nth(1).getByRole('button', { name: '开始接诊' }).count(), 1);
      assert.match(await p.locator('#appointmentRows').innerText(), /预约号 9/);
    }, '', 'doctor-schedule/appointments.html'],
    ['确认预约页显示服务端容量与余号', 'PATIENT', async p => {
      await p.locator('#bookingCapacity').waitFor({ timeout: 2500 });
      assert.match(await p.locator('#bookingCapacity').innerText(), /容量 10.*余号 8/);
    }, '?doctor_id=5&date=' + today + '&time=AM', 'hospro/book-appointment.html']
  ];
  for (const [name, role, test, extra, pagePath] of tests) {
    let page; const mutations = [];
    try { page = await open(browser, role, mutations, extra, pagePath); await test(page, mutations); console.log('PASS ' + name); }
    catch (e) { failures++; console.error('FAIL ' + name + ': ' + e.message); }
    finally { if (page) await page.close(); }
  }
  await browser.close(); if (failures) process.exitCode = 1;
}
run().catch(e => { console.error(e); process.exitCode = 1; });
