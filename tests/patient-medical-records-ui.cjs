// Exercises the actual patient page in a browser with controlled API responses.
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const webRoot = path.resolve(__dirname, '../src/main/webapp');
const origin = 'http://hospital.test';

async function run() {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.PLAYWRIGHT_BROWSER_PATH || 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe' });
  let failures = 0;
  async function makePage(options = {}) {
    const page = await browser.newPage(); const requests = [];
    await page.route('**/*', async route => {
      const url = new URL(route.request().url()); const name = url.pathname.replace(/^\/ustbhospital/, '');
      const json = (body, status = 200) => route.fulfill({status, contentType: 'application/json', body: JSON.stringify(body)});
      if (name.startsWith('/medical-records/')) {
        requests.push(url);
        if (name !== '/medical-records/patient/search') return json([]);
        if (options.denied) return json({message: '请先登录患者账号。'}, 401);
        const pageNum = Number(url.searchParams.get('pageNum') || 1);
        return json({items: options.empty || pageNum === 2 ? [] : [{ appointmentId: 6, doctorId: 8, doctorName: '王医生', appointmentDate: '2030-01-01', appointmentTime: 'AM', finalizedAt: '2030-01-02 10:00:00', diagnosis: '<script>危险</script>', treatmentPlan: '休息', orders: [{itemName: '检查'}] }], pageNum, pageSize: 10, total: options.empty ? 0 : 11, pages: options.empty ? 0 : 2});
      }
      const file = path.resolve(webRoot, decodeURIComponent(name).replace(/^\//, ''));
      if (url.origin !== origin || !file.startsWith(webRoot + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) return route.fulfill({status:404,body:''});
      return route.fulfill({status:200, contentType:file.endsWith('.js')?'application/javascript; charset=utf-8':'text/html; charset=utf-8',body:fs.readFileSync(file)});
    });
    await page.goto(origin + '/ustbhospital/hospro/medical-records.html');
    await page.waitForFunction(() => !document.querySelector('#records').textContent.includes('正在加载'));
    return {page,requests};
  }
  const tests = [
    ['分页新入口展示医生预约定稿时间和医嘱，内容安全转义', async () => {
      const {page,requests} = await makePage();
      try {
        assert.equal(requests[0].pathname, '/ustbhospital/medical-records/patient/search');
        const text = await page.locator('#records').textContent();
        for (const expected of ['王医生','2030-01-01','AM','2030-01-02 10:00:00','检查','<script>危险</script>']) assert.ok(text.includes(expected), expected);
        assert.equal(await page.locator('#records script').count(), 0);
        assert.match(await page.locator('#pageInfo').textContent(), /11/);
        assert.equal(await page.locator('#prevPage').isDisabled(),true);
      } finally { await page.close(); }
    }],
    ['组合筛选重置页码，空的后一页仍可返回上一页', async () => {
      const {page,requests} = await makePage();
      try {
        assert.equal(requests[0].pathname, '/ustbhospital/medical-records/patient/search');
        await page.locator('#dateFrom').fill('2030-01-01'); await page.locator('#dateTo').fill('2030-01-02'); await page.locator('#doctorId').fill('8');
        await page.locator('#searchButton').click();
        await page.waitForFunction(() => document.querySelector('#searchButton').disabled === false);
        const query = requests.at(-1).searchParams;
        assert.equal(query.get('dateFrom'),'2030-01-01'); assert.equal(query.get('doctorId'),'8'); assert.equal(query.get('pageNum'),'1');
        await page.locator('#nextPage').click();
        await page.waitForFunction(() => document.querySelector('#records').textContent.includes('本页'));
        assert.equal(requests.at(-1).searchParams.get('pageNum'),'2');
        assert.equal(await page.locator('#nextPage').isDisabled(),true); assert.equal(await page.locator('#prevPage').isEnabled(),true);
        await page.locator('#prevPage').click(); await page.waitForFunction(() => document.querySelector('#records').textContent.includes('王医生'));
        assert.equal(requests.at(-1).searchParams.get('pageNum'),'1');
      } finally { await page.close(); }
    }],
    ['空结果与权限失效有明确状态和恢复入口', async () => {
      for (const options of [{empty:true}, {denied:true}]) {
        const {page,requests} = await makePage(options);
        try {
          assert.equal(requests[0].pathname,'/ustbhospital/medical-records/patient/search');
          assert.equal(await page.locator('#nextPage').isDisabled(),true);
          const text = await page.locator('#records').textContent();
          assert.match(text,options.empty ? /暂无/ : /登录/);
          if (options.denied) assert.equal(await page.locator('#retryButton').isVisible(),true);
        } finally { await page.close(); }
      }
    }]
  ];
  for (const [name,test] of tests) { try { await test(); console.log('PASS '+name); } catch(e) { failures++; console.error('FAIL '+name+': '+e.message); } }
  await browser.close(); if (failures) process.exitCode=1;
}
run().catch(e=>{console.error(e);process.exitCode=1;});
