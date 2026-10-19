// Browser regression tests. Run with NODE_PATH pointing to a Playwright installation.
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const webRoot = path.resolve(__dirname, '../src/main/webapp');
const origin = 'http://hospital.test';
const records = ['BOOKED', 'RESCHEDULED', 'CANCELLED'].map((status, index) => ({
    appointmentId: index + 1, appointmentDate: '2030-01-10', appointmentTime: 'PM', status,
    doctors: { name: '演示医生' }, departments: { departmentName: '内科' }
}));
const departments = [{ departmentId: 1, departmentName: '内科' }];

async function newPage(browser, mutations) {
    const page = await browser.newPage();
    await page.route('**/*', async route => {
        const url = new URL(route.request().url());
        const name = url.pathname.replace(/^\/ustbhospital/, '');
        const json = (body, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
        if (name === '/security/csrf') return json({ token: 'test-token' });
        if (name === '/patients/current') return json({ patientId: 1, name: 'patient_demo', avatar: '' });
        if (name === '/appointments/patient') return json({ list: records, pages: 1, pageNum: 1 });
        if (name === '/appointments/reschedule') {
            mutations.push('reschedule');
            const headers = await route.request().allHeaders();
            mutations.lastRequest = { body: Object.fromEntries(new URLSearchParams(route.request().postData())), token: headers['x-csrf-token'] };
            return json({ success: false, code: 'SLOT_UNAVAILABLE', message: '该医生在所选日期或时段没有排班，或该时段已被预约。' }, 409);
        }
        if (name === '/appointments/cancel') { mutations.push('cancel'); return json({ success: true, message: '预约已取消' }); }
        if (name === '/first') return json(departments);
        if (name === '/second') return json([]);
        if (url.origin !== origin || name.startsWith('/hos_images/')) return route.fulfill({ status: 404, body: '' });
        const relative = decodeURIComponent(name).replace(/^\//, '');
        const file = path.resolve(webRoot, relative);
        if (!file.startsWith(webRoot + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) return route.fulfill({ status: 404, body: 'Not found' });
        const ext = path.extname(file);
        const type = { '.jsp': 'text/html; charset=utf-8', '.html': 'text/html; charset=utf-8', '.js': 'application/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8' }[ext] || 'application/octet-stream';
        let body = fs.readFileSync(file);
        if (ext === '.jsp') body = body.toString('utf8').replace(/<%@[^%]*%>/g, '');
        return route.fulfill({ status: 200, contentType: type, body });
    });
    await page.goto(origin + '/ustbhospital/hospro/patient.jsp?patientId=1');
    await page.locator('#appBody tr').first().waitFor();
    return page;
}

async function run() {
    const executablePath = process.env.PLAYWRIGHT_BROWSER_PATH || 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
    const browser = await chromium.launch({ headless: true, executablePath });
    let failures = 0;
    const tests = [
        ['改期窗口同时展示三个时间段', async page => {
            await page.locator('#appBody tr').nth(0).getByRole('button', { name: /修改/ }).click();
            for (const label of ['上午', '下午', '晚间']) assert.equal(await page.getByRole('radio', { name: label }).isVisible(), true, label + '应直接可见');
            assert.equal(await page.getByRole('radio', { name: '下午' }).isChecked(), true);
        }],
        ['无排班改期时显示服务端提示', async (page, mutations, dialogs) => {
            await page.locator('#appBody tr').nth(0).getByRole('button', { name: /修改/ }).click();
            await page.locator('#updateAppointmentDate').fill('2030-01-11');
            await page.getByRole('radio', { name: '晚间' }).check();
            await page.getByRole('button', { name: '提交', exact: true }).click();
            await page.waitForTimeout(150);
            assert.deepEqual(Array.from(mutations), ['reschedule']);
            assert.deepEqual(mutations.lastRequest, { body: { appointmentId: '1', appointmentDate: '2030-01-11', appointmentTime: 'EVENING' }, token: 'test-token' });
            assert.match(dialogs.join(' '), /排班|不可预约/);
            assert.equal(await page.locator('#updateModal').isVisible(), true, '改期失败后应保留窗口以便重新选择');
        }],
        ['已改期记录取消时显示状态而不发送请求', async (page, mutations, dialogs) => {
            await page.locator('#appBody tr').nth(1).getByRole('button', { name: /取消/ }).click({ timeout: 1500 });
            assert.match(dialogs.join(' '), /已改期/);
            assert.deepEqual(mutations, []);
        }],
        ['已取消记录修改时显示状态而不发送请求', async (page, mutations, dialogs) => {
            await page.locator('#appBody tr').nth(2).getByRole('button', { name: /修改/ }).click({ timeout: 1500 });
            assert.match(dialogs.join(' '), /已取消/);
            assert.deepEqual(mutations, []);
        }],
        ['页面状态过期时显示服务端最新状态', async (page, mutations, dialogs) => {
            await page.route('**/appointments/cancel', async route => {
                mutations.push('cancel');
                await route.fulfill({ status: 409, contentType: 'application/json', body: JSON.stringify({ success: false, code: 'INVALID_STATE', message: '该预约已改期，请操作新的预约记录。' }) });
            });
            await page.locator('#appBody tr').nth(0).getByRole('button', { name: /取消/ }).click();
            await page.waitForTimeout(150);
            assert.match(dialogs.join(' '), /已改期/);
            assert.deepEqual(mutations, ['cancel']);
        }],
        ['新增预约进入科室选择并可进入医生目录', async page => {
            await page.locator('#newAppointmentLink').click();
            assert.match(page.url(), /appointment-departments\.html/);
            await page.getByRole('link', { name: /内科/ }).click();
            assert.match(page.url(), /doctor-directory\.html\?departmentId=1/);
        }]
    ];
    for (const [name, test] of tests) {
        const mutations = [], dialogs = [];
        let page;
        try {
            page = await newPage(browser, mutations);
            page.on('dialog', async dialog => { dialogs.push(dialog.message()); await dialog.accept(); });
            await test(page, mutations, dialogs);
            console.log('PASS ' + name);
        } catch (error) {
            failures++;
            console.error('FAIL ' + name + ': ' + error.message);
        } finally { if (page) await page.close(); }
    }
    await browser.close();
    if (failures) process.exitCode = 1;
}
run().catch(error => { console.error(error); process.exitCode = 1; });
