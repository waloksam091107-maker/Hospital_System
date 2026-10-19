const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../src/main/webapp');

(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.PLAYWRIGHT_BROWSER_PATH || 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe' });
  const page = await browser.newPage(); const requests = [];
  await page.route('**/*', async route => {
    const url = new URL(route.request().url()); const relative = url.pathname.replace(/^\/ustbhospital/, '');
    if (relative === '/audit-logs/search') {
      requests.push(url.searchParams);
      if (url.searchParams.get('action') === 'ERROR') return route.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ message: '查询失败' }) });
      const rows = url.searchParams.get('action') === 'EMPTY' ? [] : [{ createdAt:'2030-01-01 10:00:00', actorRole:'PATIENT', actorId:1, action:'<img src=x onerror=alert(1)>', outcome:'SUCCESS' }];
      return route.fulfill({ status:200, contentType:'application/json', body:JSON.stringify({rows,total:rows.length?21:0,page:Number(url.searchParams.get('page')||1),pageSize:20}) });
    }
    const file = path.resolve(root, decodeURIComponent(relative).replace(/^\//,''));
    if (!file.startsWith(root + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) return route.fulfill({ status:404, body:'' });
    return route.fulfill({ status:200, contentType:file.endsWith('.js')?'application/javascript; charset=utf-8':'text/html; charset=utf-8', body:fs.readFileSync(file) });
  });
  try {
    await page.goto('http://hospital.test/ustbhospital/hospro/audit-logs.html');
    await page.waitForSelector('#auditRows tr');
    assert.equal(await page.locator('#auditRows img').count(),0);
    assert.ok((await page.locator('#auditRows').textContent()).includes('<img'));
    await page.locator('#role').selectOption('PATIENT'); await page.locator('#actorId').fill('1'); await page.locator('#searchButton').click();
    await page.waitForFunction(() => document.querySelector('#pageInfo').textContent.includes('1'));
    assert.equal(requests.at(-1).get('role'),'PATIENT'); assert.equal(requests.at(-1).get('actorId'),'1');
    await page.locator('#nextPage').click(); await page.waitForFunction(() => document.querySelector('#pageInfo').textContent.includes('2')); assert.equal(requests.at(-1).get('page'),'2');
    await page.locator('#resetButton').click(); await page.waitForFunction(() => document.querySelector('#role').value === '');
    await page.locator('#action').fill('EMPTY'); await page.locator('#searchButton').click(); await page.waitForFunction(() => document.querySelector('#message').textContent.includes('没有符合条件'));
    await page.locator('#action').fill('ERROR'); await page.locator('#searchButton').click(); await page.waitForFunction(() => document.querySelector('#message').textContent.includes('查询失败'));
    console.log('PASS 审计筛选、分页、清空、无结果、错误和安全渲染');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode=1; });
