// Opt-in: ONLY the seeded disposable environment on port 18081. Never prints credentials.
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const fixture = JSON.parse(fs.readFileSync(path.resolve(__dirname, '../.hospital-local/five-extensions-http-fixture.json'), 'utf8').replace(/^\uFEFF/, ''));
assert.match(fixture.prefix, /^http_[a-f0-9]{10}$/);
const base = 'http://127.0.0.1:18081/ustbhospital';
const stamp = Date.now().toString();
const unique = fixture.prefix + '-portal-' + stamp;
const card = '110101' + stamp.slice(-12).padStart(12, '0');
const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a5xkAAAAASUVORK5CYII=', 'base64');
let checks = 0;
function check(condition, message) { assert.ok(condition, message); checks++; }

(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.PLAYWRIGHT_BROWSER_PATH || 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe' });
  const contexts = [];
  async function client(role, username = fixture.prefix) {
    const context = await browser.newContext(); contexts.push(context);
    if (role) {
      const response = await context.request.post(base + '/auth/login', { form: { roleCode: String(role), user: username, password: fixture.password } });
      check(response.ok() && !response.url().includes('login_failed'), 'isolated role login failed');
      const token = await context.request.get(base + '/security/csrf');
      check(token.status() === 200, 'role CSRF bootstrap failed');
      context.csrf = (await token.json()).token;
    }
    return context;
  }
  async function api(context, route, form, status = 200, token = context.csrf) {
    const response = form === undefined ? await context.request.get(base + route) : await context.request.post(base + route, { form, headers: token ? { 'X-CSRF-Token': token } : {} });
    assert.equal(response.status(), status, route + ': unexpected HTTP status'); checks++;
    return response.json();
  }
  async function mutation(context, route, form) { const data = await api(context, route, form); check(data.success === true, route + ': mutation failed'); return data; }
  async function search(context, resource, keyword) { return api(context, '/portal/admin/' + resource + '?' + new URLSearchParams({ keyword, pageNum: '1', pageSize: '100' })); }
  try {
    const anonymous = await client();
    check((await anonymous.request.get(base + '/health/ready')).status() === 200, 'isolated app not ready');
    await api(anonymous, '/portal/profile', undefined, 401);
    await api(anonymous, '/portal/balance/recharge', undefined, 401);
    await api(anonymous, '/portal/admin/doctors', undefined, 401);
    const admin = await client(3), doctor = await client(1), patient = await client(2);
    for (const actor of [patient, doctor]) {
      await api(actor, '/portal/admin/departments', undefined, 403);
      await api(actor, '/portal/admin/announcements', { action: 'create', title: unique, content: 'forbidden' }, 403);
      await api(actor, '/audit-logs/search', undefined, 403);
    }
    for (const actor of [admin, doctor]) await api(actor, '/portal/balance/recharge', { amount: '1.00' }, 403);
    await api(patient, '/portal/balance/recharge', { amount: '1.00' }, 403, null);
    await api(patient, '/portal/profile', { phone: '13900139000' }, 403, null);
    await api(admin, '/portal/admin/departments', { action: 'create', departmentName: unique }, 403, null);

    // Real browser registration, including client validation and the real backend.
    const registerPage = await anonymous.newPage(); const pageErrors = [];
    registerPage.on('pageerror', error => pageErrors.push(error.message));
    const registrationReady = registerPage.waitForResponse(response => response.url().endsWith('/portal/accounts/csrf'));
    await registerPage.goto(base + '/hospro/register-page.html'); await registrationReady;
    await registerPage.locator('[name=name]').fill(unique);
    await registerPage.locator('[name=idCardNumber]').fill(card);
    await registerPage.locator('[name=phone]').fill('13800138000');
    await registerPage.locator('[name=email]').fill('portal@example.test');
    await registerPage.locator('[name=password]').fill(fixture.password);
    await registerPage.locator('[name=confirmPassword]').fill('different-password');
    await registerPage.locator('#registrationForm button').click();
    await registerPage.getByText('两次输入的密码不一致。', { exact: true }).waitFor(); checks++;
    await registerPage.locator('[name=confirmPassword]').fill(fixture.password);
    await registerPage.locator('#registrationForm button').click();
    await registerPage.locator('#message').filter({ hasText: '注册成功' }).waitFor(); checks++;
    check(await registerPage.locator('[name=name]').inputValue() === '', 'registration did not clear successful form');
    const registrationToken = (await api(anonymous, '/portal/accounts/csrf')).token;
    await api(anonymous, '/portal/accounts/patient-register', { name: unique, idCardNumber: card, password: fixture.password }, 409, registrationToken);
    const virtual = await client(2, unique);
    let profile = await api(virtual, '/portal/profile');
    check(profile.name === unique && Number(profile.balance) === 0, 'registered patient identity or balance wrong');
    check(!('password' in profile) && !('passwordHash' in profile), 'profile discloses credentials');
    const virtualId = profile.id;
    check((await api(virtual, '/portal/profile?patientId=' + fixture.patient)).id === virtualId, 'profile accepted a caller-supplied identity');
    const profilePage = await virtual.newPage(); profilePage.on('pageerror', error => pageErrors.push(error.message));
    await profilePage.goto(base + '/hospro/portal-profile.html');
    await profilePage.waitForFunction(() => document.querySelector('#name').value !== '');
    await profilePage.locator('#phone').fill('13900139000');
    await profilePage.locator('#email').fill('updated@example.test');
    await profilePage.locator('#profileForm button').click();
    await profilePage.locator('#message').filter({ hasText: '已保存' }).waitFor();
    profile = await api(virtual, '/portal/profile');
    check(profile.phone === '13900139000' && profile.email === 'updated@example.test', 'profile browser save not persisted');
    await api(virtual, '/portal/profile', { phone: 'invalid-phone', email: 'bad' }, 400);
    check((await api(virtual, '/portal/profile')).phone === '13900139000', 'invalid profile changed saved data');
    for (const [actor, role] of [[admin, 'ADMIN'], [doctor, 'DOCTOR']]) check((await api(actor, '/portal/profile')).role === role, 'profile crossed role boundary');

    const upload = await virtual.request.post(base + '/portal/profile/avatar', { headers: { 'X-CSRF-Token': virtual.csrf }, multipart: { avatar: { name: 'avatar.png', mimeType: 'image/png', buffer: png } } });
    check(upload.status() === 200, 'valid avatar upload failed');
    const avatar = (await upload.json()).avatar;
    check(/^uploads\/avatars\/patient-\d+-[a-f0-9]+\.png$/.test(avatar), 'avatar path is not generated safely');
    const raw = await anonymous.request.get(base + '/' + avatar);
    check(raw.status() === 200 && (await raw.body()).equals(png), 'persisted avatar raw bytes differ');
    const badAvatar = await virtual.request.post(base + '/portal/profile/avatar', { headers: { 'X-CSRF-Token': virtual.csrf }, multipart: { avatar: { name: 'fake.png', mimeType: 'image/png', buffer: Buffer.from('invalid image') } } });
    check(badAvatar.status() === 400, 'forged avatar accepted');
    const largeAvatar = await virtual.request.post(base + '/portal/profile/avatar', { headers: { 'X-CSRF-Token': virtual.csrf }, multipart: { avatar: { name: 'large.png', mimeType: 'image/png', buffer: Buffer.alloc(2 * 1024 * 1024 + 1) } } });
    check(largeAvatar.status() === 400, 'oversized avatar not rejected with validation error');
    check((await api(virtual, '/portal/profile')).avatar === avatar, 'invalid avatar overwrote saved path');
    // Save upload identity for the orchestrator to verify again after container replacement.
    fs.writeFileSync(path.resolve(__dirname, '../.hospital-local/full-system-portal-avatar.json'), JSON.stringify({ base, path: avatar, base64: png.toString('base64') }));

    const rechargePage = await virtual.newPage(); rechargePage.on('pageerror', error => pageErrors.push(error.message));
    await rechargePage.goto(base + '/hospro/patients-pay.html');
    await rechargePage.getByText('暂无流水。', { exact: true }).waitFor();
    await rechargePage.locator('[name=amount]').fill('12.34');
    await rechargePage.locator('#rechargeForm button').click();
    await rechargePage.locator('#message.success').waitFor();
    await rechargePage.getByRole('cell', { name: '12.34', exact: true }).first().waitFor();
    check(Number((await api(virtual, '/portal/profile')).balance) === 12.34, 'recharge balance incorrect');
    let ledger = (await api(virtual, '/portal/balance/recharge')).transactions;
    check(ledger.length === 1 && Number(ledger[0].amount) === 12.34 && Number(ledger[0].balanceAfter) === 12.34, 'recharge ledger incorrect');
    for (const amount of ['0', '-1', '1.001', '10000.01', 'NaN']) await api(virtual, '/portal/balance/recharge', { amount }, 400);
    check(Number((await api(virtual, '/portal/profile')).balance) === 12.34, 'invalid recharge changed balance');

    const departmentName = unique + '-科室';
    await mutation(admin, '/portal/admin/departments', { action: 'create', departmentName, departmentDescription: '原说明' });
    let rows = (await search(admin, 'departments', departmentName)).items;
    check(rows.length === 1, 'created department search did not find exactly one row');
    const departmentId = rows[0].departmentId;
    await mutation(admin, '/portal/admin/departments', { action: 'update', departmentId, departmentName, departmentDescription: '更新说明' });
    check((await search(admin, 'departments', departmentName)).items[0].departmentDiscription === '更新说明', 'department description not retained');
    const childName = unique + '-子科室';
    await mutation(admin, '/portal/admin/departments', { action: 'create', departmentName: childName, departmentPid: departmentId });
    const child = (await search(admin, 'departments', childName)).items[0];
    check(child.departmentPid === departmentId, 'department parent not retained');
    await api(admin, '/portal/admin/departments', { action: 'delete', departmentId }, 409);
    const found = await api(anonymous, '/find?keyword=' + encodeURIComponent(childName));
    check(found.some(item => item.departmentId === child.departmentId), 'public department search failed');
    await api(anonymous, '/find?keyword=', undefined, 400);
    await mutation(admin, '/portal/admin/professional-titles', { action: 'create', titleName: unique + '-职称', description: '原说明' });
    const title = (await search(admin, 'professional-titles', unique + '-职称')).items[0];
    await mutation(admin, '/portal/admin/professional-titles', { action: 'update', id: title.id, titleName: unique + '-职称', description: '更新职称说明' });
    check((await search(admin, 'professional-titles', unique + '-职称')).items[0].description === '更新职称说明', 'title update not persisted');
    const doctorForm = { action: 'create', name: unique + '-医生', departmentId: child.departmentId, professionalTitleId: title.id, registrationFee: '20.00', password: fixture.password, phone: '13800138000', email: 'doctor@example.test', entryDate: '2020-01-01', introduction: '原简介' };
    await mutation(admin, '/portal/admin/doctors', doctorForm);
    const createdDoctor = (await search(admin, 'doctors', doctorForm.name)).items[0];
    check(createdDoctor && createdDoctor.jobNumber && !createdDoctor.password && !createdDoctor.passwordHash, 'doctor creation identity or credential exposure wrong');
    await api(admin, '/portal/admin/professional-titles', { action: 'delete', id: title.id }, 409);
    await api(admin, '/portal/admin/departments', { action: 'delete', departmentId: child.departmentId }, 409);
    await mutation(admin, '/portal/admin/doctors', { ...doctorForm, action: 'update', doctorId: createdDoctor.doctorId, introduction: '更新医生简介', registrationFee: '25.00' });
    check((await search(admin, 'doctors', doctorForm.name)).items[0].introduction === '更新医生简介', 'doctor edit not retained');
    await api(admin, '/portal/admin/doctors?pageNum=0', undefined, 400);
    await api(admin, '/portal/admin/patients?pageSize=101', undefined, 400);
    await api(admin, '/portal/admin/departments', { action: 'update', departmentName }, 400);
    await api(admin, '/portal/admin/professional-titles', { action: 'update', titleName: 'invalid' }, 400);
    const managedPatient = unique + '-管理患者', managedCard = '110102' + stamp.slice(-12).padStart(12, '0');
    await mutation(admin, '/portal/admin/patients', { action: 'create', name: managedPatient, idCardNumber: managedCard, password: fixture.password });
    const managedId = (await search(admin, 'patients', managedPatient)).items[0].patientId;
    await mutation(admin, '/portal/admin/patients', { action: 'update', patientId: managedId, phone: '13700137000', email: 'managed@example.test' });
    check((await search(admin, 'patients', managedPatient)).items[0].phone === '13700137000', 'managed patient edit not persisted');
    await mutation(admin, '/portal/admin/patients', { action: 'delete', patientId: managedId });
    check((await search(admin, 'patients', managedPatient)).items.length === 0, 'managed patient delete not persisted');
    await api(admin, '/portal/admin/patients', { action: 'delete', patientId: virtualId }, 409);
    await mutation(admin, '/portal/admin/doctors', { action: 'delete', doctorId: createdDoctor.doctorId });
    await mutation(admin, '/portal/admin/professional-titles', { action: 'delete', id: title.id });
    await mutation(admin, '/portal/admin/departments', { action: 'delete', departmentId: child.departmentId });
    await mutation(admin, '/portal/admin/departments', { action: 'delete', departmentId });

    const announcementTitle = unique + '-公告';
    let saved = await mutation(admin, '/portal/admin/announcements', { action: 'create', title: announcementTitle, content: '公告正文 <img src=x onerror=alert(1)>' });
    check(saved.message.includes('草稿'), 'new draft reported as already published');
    const announcement = (await api(admin, '/portal/admin/announcements')).items.find(item => item.title === announcementTitle);
    check(announcement.status === 'DRAFT', 'new announcement not a draft');
    check(!(await api(anonymous, '/announcements/list')).some(item => item.announcementId === announcement.announcementId), 'draft exposed publicly');
    await mutation(admin, '/portal/admin/announcements', { action: 'update', announcementId: announcement.announcementId, title: announcementTitle, content: '更新正文 <img src=x onerror=alert(1)>' });
    await mutation(admin, '/portal/admin/announcements', { action: 'publish', announcementId: announcement.announcementId });
    check((await api(anonymous, '/announcements/list')).some(item => item.announcementId === announcement.announcementId), 'published announcement missing');
    const publicPage = await anonymous.newPage(); publicPage.on('pageerror', error => pageErrors.push(error.message));
    await publicPage.goto(base + '/hospro/news.html');
    await publicPage.getByRole('heading', { name: announcementTitle, exact: true }).waitFor();
    check(await publicPage.locator('#list img').count() === 0, 'public announcement markup executed');
    await mutation(admin, '/portal/admin/announcements', { action: 'offline', announcementId: announcement.announcementId });
    check(!(await api(anonymous, '/announcements/list')).some(item => item.announcementId === announcement.announcementId), 'offline announcement remained public');
    await mutation(admin, '/portal/admin/announcements', { action: 'delete', announcementId: announcement.announcementId });
    check(!(await api(admin, '/portal/admin/announcements')).items.some(item => item.announcementId === announcement.announcementId), 'announcement delete not retained');

    const audit = await api(admin, '/audit-logs/search?' + new URLSearchParams({ action: 'PROFILE_UPDATE', role: 'PATIENT', actorId: String(virtualId), outcome: 'SUCCESS', page: '1', pageSize: '100' }));
    check(audit.rows.length >= 1, 'profile metadata audit missing');
    const rechargeAudit = await api(admin, '/audit-logs/search?' + new URLSearchParams({ action: 'BALANCE_RECHARGE', role: 'PATIENT', actorId: String(virtualId), outcome: 'SUCCESS', page: '1', pageSize: '100' }));
    check(rechargeAudit.rows.length === 1, 'recharge success audit duplicated or missing');
    check(!JSON.stringify(audit).includes(fixture.password), 'audit contains credential');
    await api(admin, '/audit-logs/search?page=0', undefined, 400);
    check(pageErrors.length === 0, 'portal browser JavaScript errors: ' + pageErrors.join('; '));
    console.log('PASS disposable live portal: ' + checks + ' checks; registration/login, three-role boundaries, profile/avatar raw bytes, recharge/ledger, admin CRUD/search, announcement lifecycle/XSS and audits');
  } finally {
    for (const context of contexts) await context.close();
    await browser.close();
  }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
