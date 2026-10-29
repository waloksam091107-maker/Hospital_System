// Exercise the actual portal pages in Edge; HTTP fixtures never touch business data.
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const webRoot = path.resolve(__dirname, '../src/main/webapp');

async function main() {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.PLAYWRIGHT_BROWSER_PATH || 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe' });
  let failures = 0;
  async function scenario(name, handler, check) {
    const page = await browser.newPage();
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.route('**/*', async route => {
      const request = route.request(), url = new URL(request.url());
      const api = url.pathname.replace(/^\/ustbhospital/, '');
      const json = (body, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
      if (api.endsWith('/csrf')) return json({ token: 'portal-csrf' });
      if (await handler({ route, request, api, json })) return;
      const file = path.resolve(webRoot, decodeURIComponent(api).replace(/^\//, ''));
      if (!file.startsWith(webRoot + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) return route.fulfill({ status: 404, body: 'Missing' });
      return route.fulfill({ contentType: ({ '.html': 'text/html;charset=utf-8', '.js': 'application/javascript', '.css': 'text/css' })[path.extname(file)] || 'application/octet-stream', body: fs.readFileSync(file) });
    });
    try { await check(page, errors); console.log('PASS ' + name); }
    catch (error) { failures++; console.error('FAIL ' + name + ': ' + error.message); }
    finally { await page.close(); }
  }

  let recharges = 0;
  await scenario('successful recharge clears amount and refreshes ledger', async ({ request, api, json }) => {
    if (api !== '/portal/balance/recharge') return false;
    if (request.method() === 'POST') { recharges++; await json({ success: true, message: '模拟充值成功。' }); }
    else await json({ transactions: recharges ? [{ createdAt: '2030-01-01 10:00:00', transactionType: 'RECHARGE', amount: '12.34', balanceAfter: '112.34' }] : [] });
    return true;
  }, async page => {
    await page.goto('http://hospital.test/ustbhospital/hospro/patients-pay.html');
    await page.getByText('暂无流水。', { exact: true }).waitFor();
    await page.locator('[name=amount]').fill('12.34');
    await page.locator('#rechargeForm button').click();
    await page.waitForFunction(() => document.querySelector('#message').textContent !== '');
    assert.equal(await page.locator('#message').textContent(), '模拟充值成功。');
    await page.getByText('112.34', { exact: true }).waitFor();
    assert.equal(await page.locator('[name=amount]').inputValue(), '');
    assert.equal(recharges, 1);
  });

  await scenario('failed login shows feedback and preserves selected role', async () => false, async page => {
    await page.goto('http://hospital.test/ustbhospital/hospro/login.html?typeId=2&error=login_failed');
    await page.waitForFunction(() => document.querySelector('#roleCode').value === '2');
    assert.match(await page.locator('.registration-form').textContent(), /账号或密码|登录失败/);
    assert.equal(await page.locator('#roleCode + .nice-select .current').textContent(), '患者');
    assert.equal(await page.locator('[name=password]').inputValue(), '');
  });

  let concurrentRecharges = 0;
  await scenario('repeated recharge submission sends one request while saving', async ({ request, api, json }) => {
    if (api !== '/portal/balance/recharge') return false;
    if (request.method() === 'POST') {
      concurrentRecharges++;
      await new Promise(resolve => setTimeout(resolve, 120));
      await json({ success: false, message: '充值暂时不可用。' }, 503);
    } else await json({ transactions: [] });
    return true;
  }, async page => {
    await page.goto('http://hospital.test/ustbhospital/hospro/patients-pay.html');
    await page.getByText('暂无流水。', { exact: true }).waitFor();
    await page.locator('[name=amount]').fill('12.34');
    const saved = page.waitForResponse(response => response.request().method() === 'POST');
    await page.locator('#rechargeForm').evaluate(form => { form.requestSubmit(); form.requestSubmit(); });
    await saved;
    await page.getByText('充值暂时不可用。', { exact: true }).waitFor();
    assert.equal(concurrentRecharges, 1, 'a pending recharge must not be sent twice');
    assert.equal(await page.locator('[name=amount]').inputValue(), '12.34');
    assert.equal(await page.locator('#rechargeForm button').isEnabled(), true);
  });

  let announcementReads = 0;
  await scenario('announcement save keeps success feedback after form reset', async ({ request, api, json }) => {
    if (api !== '/portal/admin/announcements') return false;
    if (request.method() === 'POST') await json({ success: true, message: '草稿已保存。' });
    else { announcementReads++; await json({ items: [] }); }
    return true;
  }, async page => {
    const loaded = page.waitForResponse(response => response.url().endsWith('/portal/admin/announcements'));
    await page.goto('http://hospital.test/ustbhospital/hospro/admin-announcements.html'); await loaded;
    await page.locator('[name=title]').fill('门诊公告');
    await page.locator('[name=content]').fill('门诊安排说明');
    const saved = page.waitForResponse(response => response.url().endsWith('/portal/admin/announcements') && response.request().method() === 'POST');
    await page.locator('#announcement-form button[type=submit]').click(); await saved;
    await page.waitForTimeout(50);
    assert.equal(await page.locator('#result').textContent(), '草稿已保存。');
    assert.equal(await page.locator('[name=title]').inputValue(), '');
    assert.equal(announcementReads, 2);
  });

  await scenario('registration network failure is visible and preserves input', async ({ route, api }) => {
    if (api !== '/portal/accounts/patient-register') return false;
    await route.abort('failed'); return true;
  }, async (page, errors) => {
    const loaded = page.waitForResponse(response => response.url().endsWith('/portal/accounts/csrf'));
    await page.goto('http://hospital.test/ustbhospital/hospro/register-page.html'); await loaded;
    await page.locator('[name=name]').fill('测试患者');
    await page.locator('[name=idCardNumber]').fill('110101199001011234');
    await page.locator('[name=password]').fill('password123');
    await page.locator('[name=confirmPassword]').fill('password123');
    const completed = page.waitForEvent('requestfailed', { predicate: request => request.url().endsWith('/portal/accounts/patient-register') });
    await page.locator('#registrationForm button').click(); await completed;
    await page.waitForTimeout(50);
    assert.match(await page.locator('#message').textContent(), /失败|不可用|重试/);
    assert.equal(await page.locator('[name=name]').inputValue(), '测试患者');
    assert.deepEqual(errors, []);
  });

  const profileWrites = [];
  await scenario('doctor profile and avatar use authenticated safe routes', async ({ request, api, json }) => {
    if (api === '/notifications/unread-count') { await json({ count: 0 }); return true; }
    if (!api.startsWith('/portal/profile')) return false;
    if (request.method() === 'GET') await json({ role: 'DOCTOR', id: 7, name: '测试医生', jobNumber: 'D007', phone: '13800138000', email: 'doctor@example.test', introduction: '原简介', avatar: '' });
    else {
      profileWrites.push({ api, token: request.headers()['x-csrf-token'], type: request.headers()['content-type'], body: request.postData() });
      await json({ success: true, message: api.endsWith('/avatar') ? '头像已更新。' : '个人资料已保存。', avatar: 'uploads/avatars/doctor-7-test.png' });
    }
    return true;
  }, async (page, errors) => {
    await page.goto('http://hospital.test/ustbhospital/hospro/portal-profile.html');
    await page.waitForFunction(() => document.querySelector('#name').value === '测试医生');
    assert.match(await page.locator('#identity').textContent(), /D007/);
    assert.equal(await page.locator('#introduction').isVisible(), true);
    await page.locator('#phone').fill('13900139000');
    await page.locator('#introduction').fill('更新简介');
    await page.locator('#profileForm button').click();
    await page.getByText('个人资料已保存。', { exact: true }).waitFor();
    assert.deepEqual(Object.fromEntries(new URLSearchParams(profileWrites[0].body)), { phone: '13900139000', email: 'doctor@example.test', introduction: '更新简介' });
    assert.equal(profileWrites[0].token, 'portal-csrf');
    await page.locator('[name=avatar]').setInputFiles({ name: 'avatar.png', mimeType: 'image/png', buffer: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a5xkAAAAASUVORK5CYII=', 'base64') });
    await page.locator('#avatarForm button').click();
    await page.getByText('头像已更新。', { exact: true }).waitFor();
    assert.equal(profileWrites[1].api, '/portal/profile/avatar');
    assert.equal(profileWrites[1].token, 'portal-csrf');
    assert.match(profileWrites[1].type, /^multipart\/form-data; boundary=/);
    assert.match(profileWrites[1].body, /filename="avatar.png"/);
    assert.deepEqual(errors, []);
  });

  const announcementActions = [];
  let announcementStatus = 'DRAFT';
  await scenario('announcement edit publish offline delete retain action and identity', async ({ request, api, json }) => {
    if (api !== '/portal/admin/announcements') return false;
    if (request.method() === 'POST') {
      const values = Object.fromEntries(new URLSearchParams(request.postData()));
      announcementActions.push({ ...values, token: request.headers()['x-csrf-token'] });
      if (values.action === 'publish') announcementStatus = 'PUBLISHED';
      if (values.action === 'offline') announcementStatus = 'OFFLINE';
      if (values.action === 'delete') announcementStatus = 'DELETED';
      await json({ success: true, message: '操作成功。' });
    } else await json({ items: announcementStatus === 'DELETED' ? [] : [{ announcementId: 8, title: '测试公告', content: '<img src=x onerror=alert(1)>', status: announcementStatus, creationTime: '2030-01-01 10:00:00', publishedAt: announcementStatus === 'PUBLISHED' ? '2030-01-02 10:00:00' : null }] });
    return true;
  }, async page => {
    await page.goto('http://hospital.test/ustbhospital/hospro/admin-announcements.html');
    await page.locator('#items article').waitFor();
    assert.equal(await page.locator('#items img').count(), 0);
    await page.getByRole('button', { name: '编辑', exact: true }).click();
    assert.equal(await page.locator('[name=announcementId]').inputValue(), '8');
    await page.locator('[name=content]').fill('更新正文');
    await page.locator('#announcement-form button[type=submit]').click();
    await page.getByText('操作成功。', { exact: true }).waitFor();
    assert.deepEqual(announcementActions[0], { announcementId: '8', title: '测试公告', content: '更新正文', action: 'update', token: 'portal-csrf' });
    await page.getByRole('button', { name: '发布', exact: true }).click();
    await page.getByRole('button', { name: '下线', exact: true }).waitFor();
    await page.getByRole('button', { name: '下线', exact: true }).click();
    await page.getByRole('button', { name: '发布', exact: true }).waitFor();
    page.on('dialog', dialog => dialog.accept());
    await page.getByRole('button', { name: '删除', exact: true }).click();
    await page.locator('#items article').waitFor({ state: 'detached' });
    assert.deepEqual(announcementActions.map(item => item.action), ['update', 'publish', 'offline', 'delete']);
    assert(announcementActions.every(item => item.announcementId === '8' && item.token === 'portal-csrf'));
  });

  await scenario('public announcements render server text without executing markup', async ({ api, json }) => {
    if (api !== '/announcements/list') return false;
    await json([{ announcementId: 8, title: '<img src=x onerror=alert(1)>', content: '<script>alert(1)</script>\n门诊安排', creator: '管理员', creationTime: '2030-01-01 10:00:00', publishedAt: '2030-01-02 10:00:00' }]); return true;
  }, async page => {
    await page.goto('http://hospital.test/ustbhospital/hospro/news.html');
    await page.locator('#list article').waitFor();
    assert.equal(await page.locator('#list img, #list script').count(), 0);
    assert.equal(await page.locator('#list h2').textContent(), '<img src=x onerror=alert(1)>');
    assert.match(await page.locator('#list .meta').textContent(), /2030-01-02 10:00:00/);
    assert.match(await page.locator('#list .content').textContent(), /门诊安排/);
  });

  await browser.close();
  assert.equal(failures, 0, failures + ' portal UI failures');
}
main().catch(error => { console.error(error); process.exitCode = 1; });
