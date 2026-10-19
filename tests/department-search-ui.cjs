// Browser regression tests for both visible department searches and the legacy entry.
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const webRoot = path.resolve(__dirname, '../src/main/webapp');
const origin = 'http://hospital.test';
const departments = [
  { departmentId: 1, departmentName: '内科', departmentPid: null, departmentLevel: 1, departmentPath: '/1', departmentDiscription: null },
  { departmentId: 2, departmentName: '内分泌科', departmentPid: null, departmentLevel: 1, departmentPath: '/2', departmentDiscription: null }
];
const doctor = name => ({ doctorId: 10, name, jobNumber: 'D1001', phone: '', avatar: '', introduction: '', departments: { departmentName: name }, professionalTitles: { titleName: '主治医师' } });

async function run() {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.PLAYWRIGHT_BROWSER_PATH || 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe' });
  let failures = 0;
  async function openPage(relative = '/hospro/doctor-directory.html?departmentId=2&departmentName=%E5%86%85%E5%88%86%E6%B3%8C%E7%A7%91') {
    const page = await browser.newPage();
    page.setDefaultTimeout(4000);
    const doctorRequests = [];
    await page.route('**/*', async route => {
      const url = new URL(route.request().url());
      const name = url.pathname.replace(/^\/ustbhospital/, '');
      const json = (body, status = 200) => route.fulfill({ status, contentType: 'application/json; charset=utf-8', body: JSON.stringify(body) });
      if (name === '/find') {
        const keyword = url.searchParams.get('keyword') || '';
        if (keyword === '接口故障') return json({ message: '查询失败' }, 500);
        return json(departments.filter(d => d.departmentName.includes(keyword)));
      }
      if (name === '/doc/findByDeptId') {
        const id = url.searchParams.get('departmentId');
        doctorRequests.push(id);
        const department = departments.find(d => String(d.departmentId) === id);
        return json({ list: department ? [doctor(department.departmentName + '医生')] : [], pages: department ? 1 : 0, pageNum: 1 });
      }
      if (name === '/first') return json(departments);
      if (name === '/second' || name === '/news/latest') return json([]);
      const file = path.resolve(webRoot, decodeURIComponent(name).replace(/^\//, ''));
      if (url.origin !== origin || !file.startsWith(webRoot + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) return route.fulfill({ status: 404, body: '' });
      const ext = path.extname(file);
      return route.fulfill({ status: 200, contentType: { '.js': 'application/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8', '.html': 'text/html; charset=utf-8' }[ext] || 'application/octet-stream', body: fs.readFileSync(file) });
    });
    await page.goto(origin + '/ustbhospital' + relative);
    await page.addStyleTag({ content: '.preloader { display: none !important; }' });
    return { page, doctorRequests };
  }
  const tests = [
    ['医生目录顶部搜索内科，显示该科室医生而不返回旧首页', async () => {
      const { page, doctorRequests } = await openPage();
      try {
        const form = page.locator('form').first();
        await form.locator('input').first().fill('内科');
        await form.locator('button').first().click();
        await page.getByText('内科医生', { exact: true }).first().waitFor({ timeout: 2500 });
        assert.match(page.url(), /doctor-directory\.html/);
        assert.equal(doctorRequests[doctorRequests.length - 1], '1');
      } finally { await page.close(); }
    }],
    ['医生目录下方搜索能直接匹配一个科室', async () => {
      const { page, doctorRequests } = await openPage();
      try {
        await page.locator('#inputView').fill('内科');
        await page.locator('#searchButton').click();
        await page.getByText('内科医生', { exact: true }).first().waitFor({ timeout: 2500 });
        assert.equal(doctorRequests[doctorRequests.length - 1], '1');
      } finally { await page.close(); }
    }],
    ['多个匹配时显示可选择科室并按选择查询医生', async () => {
      const { page, doctorRequests } = await openPage();
      try {
        const form = page.locator('form').first();
        await form.locator('input').first().fill('内');
        await form.locator('button').first().click();
        await page.getByRole('button', { name: '内科', exact: true }).first().click({ timeout: 2500 });
        await page.getByText('内科医生', { exact: true }).first().waitFor({ timeout: 2500 });
        assert.equal(doctorRequests[doctorRequests.length - 1], '1');
      } finally { await page.close(); }
    }],
    ['无匹配时显示提示并保留当前页面', async () => {
      const { page, doctorRequests } = await openPage();
      try {
        await page.locator('#clinicListContent').getByText('内分泌科医生', { exact: true }).first().waitFor();
        const before = doctorRequests.length;
        const form = page.locator('form').first();
        await form.locator('input').first().fill('不存在');
        await form.locator('button').first().click();
        await page.getByText(/暂无匹配|没有匹配|未找到/).first().waitFor({ timeout: 2500 });
        assert.match(page.url(), /doctor-directory\.html/);
        assert.equal(doctorRequests.length, before);
        assert.equal(await page.locator('#clinicListContent').getByText('内分泌科医生', { exact: true }).count(), 0);
      } finally { await page.close(); }
    }],
    ['搜索接口失败时显示提示并保留当前页面', async () => {
      const { page } = await openPage();
      try {
        const form = page.locator('form').first();
        await form.locator('input').first().fill('接口故障');
        await form.locator('button').first().click();
        await page.getByText(/搜索失败|查询失败/).first().waitFor({ timeout: 2500 });
        assert.match(page.url(), /doctor-directory\.html/);
      } finally { await page.close(); }
    }],
    ['正式首页输入唯一科室后可直接进入医生目录', async () => {
      const { page } = await openPage('/hospro/index.html');
      try {
        await page.locator('#inputView').fill('内科');
        await page.locator('#departmentForm button[type=submit]').click();
        await page.waitForURL(/doctor-directory\.html\?departmentId=1/, { timeout: 2500 });
        await page.locator('#clinicListContent').getByText('内科医生', { exact: true }).first().waitFor({ timeout: 2500 });
      } finally { await page.close(); }
    }],
    ['正式首页多个匹配时选择科室后进入对应医生目录', async () => {
      const { page } = await openPage('/hospro/index.html');
      try {
        await page.locator('#inputView').fill('内');
        await page.locator('#departmentForm button[type=submit]').click();
        await page.locator('#departmentSearchResults').getByRole('button', { name: '内分泌科', exact: true }).click();
        await page.locator('#departmentForm button[type=submit]').click();
        await page.waitForURL(/doctor-directory\.html\?departmentId=2/, { timeout: 2500 });
        await page.locator('#clinicListContent').getByText('内分泌科医生', { exact: true }).first().waitFor();
      } finally { await page.close(); }
    }],
    ['修改科室关键词后不会沿用此前选中的科室', async () => {
      const { page, doctorRequests } = await openPage();
      try {
        await page.locator('#clinicListContent').getByText('内分泌科医生', { exact: true }).first().waitFor();
        await page.locator('#inputView').fill('内');
        await page.locator('#result').getByRole('button', { name: '内科', exact: true }).click();
        await page.locator('#clinicListContent').getByText('内科医生', { exact: true }).first().waitFor();
        const before = doctorRequests.length;
        await page.locator('#inputView').fill('不存在');
        await page.locator('#searchButton').click();
        await page.locator('#result').getByText(/暂无匹配/).waitFor();
        assert.equal(doctorRequests.length, before);
      } finally { await page.close(); }
    }],
    ['旧首页重定向正式首页并保留登录上下文参数', async () => {
      const { page } = await openPage('/hospro/archive/index.html?roleCode=2&patientId=1#booking');
      try { await page.waitForURL(/\/hospro\/index\.html\?roleCode=2&patientId=1#booking/, { timeout: 2500 }); }
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
