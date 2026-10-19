// Browser regression tests against the real encounter page and controlled HTTP responses.
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const webRoot = path.resolve(__dirname, '../src/main/webapp');
const origin = 'http://hospital.test';
const appointment = { appointmentId: 6, appointmentDate: '2030-01-11', appointmentTime: 'AM', status: 'IN_PROGRESS' };

async function run() {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.PLAYWRIGHT_BROWSER_PATH || 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe' });
  let failures = 0;
  async function makePage(options = {}) {
    const page = await browser.newPage();
    const submitted = [];
    await page.route('**/*', async route => {
      const url = new URL(route.request().url());
      const name = url.pathname.replace(/^\/ustbhospital/, '');
      const json = (body, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
      if (name === '/security/csrf') {
        if (options.delayToken) await new Promise(resolve => setTimeout(resolve, 600));
        return json({ token: 'test-token' });
      }
      if (name === '/appointments/doctor') return json({ list: [1, 2, 3, 4, 5].map(appointmentId => ({ ...appointment, appointmentId })), pages: 2, pageNum: 1 });
      if (name === '/appointments/doctor/detail') {
        if (url.searchParams.get('appointmentId') !== '6') return json({ message: '预约不存在或无权查看。' }, 404);
        if (options.notFound) return json({ message: '预约不存在或无权查看。' }, 404);
        return json({ ...appointment, status: options.status || appointment.status });
      }
      if (name === '/medical-records/doctor') {
        if (options.delayDraft) await new Promise(resolve => setTimeout(resolve, 1200));
        return options.draft ? json(options.draft) : json({ code: 'NOT_FOUND' }, 404);
      }
      if (name === '/medical-records/draft') {
        submitted.push({ body: Object.fromEntries(new URLSearchParams(route.request().postData())), token: (await route.request().allHeaders())['x-csrf-token'] });
        if (options.delayPost) await new Promise(resolve => setTimeout(resolve, 1200));
        return options.draftConflict ? json({ success: false, code: 'VERSION_CONFLICT', message: '草稿已变化' }, 409) : json({ success: true, message: '草稿已保存', recordStatus: 'DRAFT', version: 0, medicalRecordId: 9 });
      }
      if (name === '/medical-records/finalize') {
        submitted.push({ body: Object.fromEntries(new URLSearchParams(route.request().postData())), token: (await route.request().allHeaders())['x-csrf-token'] });
        if (options.finalizeConflict) return json({ success: false, code: 'INVALID_STATE', message: '只有接诊中的预约可以提交就诊记录。' }, 409);
        return json({ success: true, message: '就诊记录和医嘱已保存，接诊已完成。' });
      }
      const file = path.resolve(webRoot, decodeURIComponent(name).replace(/^\//, ''));
      if (url.origin !== origin || !file.startsWith(webRoot + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) return route.fulfill({ status: 404, body: '' });
      return route.fulfill({ status: 200, contentType: file.endsWith('.js') ? 'application/javascript; charset=utf-8' : 'text/html; charset=utf-8', body: fs.readFileSync(file) });
    });
    await page.goto(origin + '/ustbhospital/doctor-schedule/encounter.html?appointmentId=6');
    return { page, submitted };
  }
  const tests = [
    ['草稿恢复之前不能输入，恢复完成后可编辑', async () => {
      const { page } = await makePage({ delayDraft: true, draft: { recordStatus: 'DRAFT', version: 0, diagnosis: '已保存的诊断', treatmentPlan: '方案', orders: [] } });
      try {
        assert.equal(await page.locator('#diagnosis').isDisabled(), true);
        assert.equal(await page.locator('#treatmentPlan').isDisabled(), true);
        assert.equal(await page.locator('#addOrder').isDisabled(), true);
        await page.waitForFunction(() => !document.querySelector('#draftButton').disabled, null, { timeout: 4000 });
        assert.equal(await page.locator('#diagnosis').inputValue(), '已保存的诊断');
        assert.equal(await page.locator('#diagnosis').isEnabled(), true);
        await page.locator('#diagnosis').fill('新输入');
        assert.equal(await page.locator('#diagnosis').inputValue(), '新输入');
      } finally { await page.close(); }
    }],
    ['保存期间不能编辑，发送的是完整快照，失败后可继续编辑', async () => {
      const { page, submitted } = await makePage({ delayPost: true, draftConflict: true });
      try {
        await page.waitForFunction(() => !document.querySelector('#draftButton').disabled);
        await page.locator('#diagnosis').fill('待保存的诊断');
        await page.locator('#draftButton').click();
        await page.waitForFunction(() => document.querySelector('#diagnosis').matches(':disabled'), null, { timeout: 700 });
        assert.equal(await page.locator('#addOrder').isDisabled(), true);
        await page.waitForFunction(() => document.querySelector('#message').textContent.includes('草稿已变化'));
        assert.equal(submitted[0].body.diagnosis, '待保存的诊断');
        assert.equal(await page.locator('#diagnosis').isEnabled(), true);
        assert.equal(await page.locator('#diagnosis').inputValue(), '待保存的诊断');
      } finally { await page.close(); }
    }],
    ['可读取第一页之后的预约并提交医嘱', async () => {
      const { page, submitted } = await makePage();
      try {
        await page.waitForFunction(() => document.querySelector('#appointmentHint').textContent.includes('预约 #6'), null, { timeout: 2000 });
        assert.equal(await page.locator('#submitButton').isEnabled(), true);
        await page.locator('#diagnosis').fill('测试诊断');
        await page.locator('#treatmentPlan').fill('治疗方案');
        await page.locator('#medicalAdvice').fill('医嘱说明');
        await page.locator('#addOrder').click();
        await page.locator('[name=orderItem]').fill('检查项目');
        await page.locator('[name=orderQuantity]').fill('2');
        await page.locator('#submitButton').click();
        await page.waitForFunction(() => document.querySelector('#message').textContent.includes('已保存'));
        assert.equal(submitted.length, 1);
        assert.equal(submitted[0].token, 'test-token');
        assert.deepEqual(submitted[0].body, { appointmentId: '6', diagnosis: '测试诊断', treatmentPlan: '治疗方案', medicalAdvice: '医嘱说明', orderItem: '检查项目', orderDosage: '', orderFrequency: '', orderQuantity: '2', orderInstruction: '' });
      } finally { await page.close(); }
    }],
    ['防止校验和令牌尚未就绪时提交', async () => {
      const { page, submitted } = await makePage({ delayToken: true });
      try {
        assert.equal(await page.locator('#submitButton').isDisabled(), true);
        await page.waitForFunction(() => !document.querySelector('#submitButton').disabled, null, { timeout: 3000 });
        assert.equal(submitted.length, 0);
      } finally { await page.close(); }
    }],
    ['预约不可访问时不允许保存', async () => {
      const { page } = await makePage({ notFound: true });
      try {
        await page.waitForFunction(() => document.querySelector('#message').textContent.includes('无权查看'), null, { timeout: 2000 });
        assert.equal(await page.locator('#submitButton').isDisabled(), true);
      } finally { await page.close(); }
    }],
    ['非接诊中预约不允许保存', async () => {
      const { page } = await makePage({ status: 'CANCELLED' });
      try {
        await page.waitForFunction(() => document.querySelector('#message').textContent.includes('接诊中'), null, { timeout: 2000 });
        assert.equal(await page.locator('#submitButton').isDisabled(), true);
      } finally { await page.close(); }
    }],
    ['提交时状态变化，展示服务器提示并保留已填写内容', async () => {
      const { page, submitted } = await makePage({ finalizeConflict: true });
      try {
        await page.waitForFunction(() => !document.querySelector('#submitButton').disabled, null, { timeout: 2000 });
        await page.locator('#diagnosis').fill('保留的诊断');
        await page.locator('#treatmentPlan').fill('保留的方案');
        await page.locator('#submitButton').click();
        await page.waitForFunction(() => document.querySelector('#message').textContent.includes('只有接诊中'));
        assert.equal(await page.locator('#diagnosis').inputValue(), '保留的诊断');
        assert.equal(await page.locator('#diagnosis').isEnabled(), true);
        assert.equal(await page.locator('#submitButton').isDisabled(), true);
        assert.equal(submitted.length, 1);
      } finally { await page.close(); }
    }],
    ['空必填项可以保存草稿', async () => {
      const { page, submitted } = await makePage();
      try { await page.waitForFunction(() => !document.querySelector('#draftButton').disabled); await page.locator('#draftButton').click(); await page.waitForFunction(() => document.querySelector('#message').textContent.includes('草稿已保存')); assert.equal(submitted[0].body.diagnosis, ''); assert.equal(submitted[0].token, 'test-token'); }
      finally { await page.close(); }
    }],
    ['草稿恢复医嘱，版本冲突保留输入', async () => {
      const { page, submitted } = await makePage({ draftConflict: true, draft: { recordStatus: 'DRAFT', version: 3, diagnosis: '原诊断', treatmentPlan: '方案', medicalAdvice: '休息', orders: [{ itemName: '血常规', quantity: 1 }] } });
      try { await page.waitForFunction(() => !document.querySelector('#draftButton').disabled); assert.equal(await page.locator('[name=orderItem]').inputValue(), '血常规'); await page.locator('#diagnosis').fill('未提交的修改'); await page.locator('#draftButton').click(); await page.waitForFunction(() => document.querySelector('#message').textContent.includes('草稿已变化')); assert.equal(await page.locator('#diagnosis').inputValue(), '未提交的修改'); assert.equal(submitted[0].body.version, '3'); }
      finally { await page.close(); }
    }]
  ];
  for (const [name, test] of tests) {
    try { await test(); console.log('PASS ' + name); }
    catch (error) { failures++; console.error('FAIL ' + name + ': ' + error.message); }
  }
  await browser.close();
  if (failures) process.exitCode = 1;
}
run().catch(error => { console.error(error); process.exitCode = 1; });
