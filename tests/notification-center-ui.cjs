const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../src/main/webapp');

(async function () {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.PLAYWRIGHT_BROWSER_PATH || 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe' });
  try {
    const page = await browser.newPage(); const writes = []; let allRead = false;
    await page.route('**/*', async route => {
      const request = route.request(), url = new URL(request.url()), name = url.pathname.replace(/^\/ustbhospital/, '');
      const json = body => route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(body)});
      if (name === '/security/csrf') return json({token:'test-token'});
      if (name === '/notifications/unread-count') return json({success:true,unreadCount:allRead?0:2});
      if (name === '/notifications/list') {
        const second = url.searchParams.get('page') === '2';
        return json({success:true,role:'PATIENT',page:second?2:1,pageSize:10,total:11,items:second?[]:[{id:12,title:'<img src=x onerror=alert(1)>',body:'<script>危险</script>',readAt:allRead?'2026-10-28 10:00:00':null,createdAt:'2026-10-28 09:00:00',targetType:'APPOINTMENT',targetId:'12'}]});
      }
      if (name === '/notifications/read' || name === '/notifications/read-all') {
        writes.push({path:name,method:request.method(),token:request.headers()['x-csrf-token'],body:request.postData()});
        if(name.endsWith('read-all')) allRead = true;
        return json({success:true});
      }
      const file = path.resolve(root, name.replace(/^\//, ''));
      if (!file.startsWith(root + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) return route.fulfill({status:404,body:''});
      return route.fulfill({status:200,contentType:file.endsWith('.js')?'application/javascript; charset=utf-8':'text/html; charset=utf-8',body:fs.readFileSync(file)});
    });
    await page.goto('http://hospital.test/ustbhospital/notifications.html');
    await page.waitForFunction(() => document.querySelector('#notificationList') && document.querySelector('#notificationList').textContent.includes('危险'));
    assert.equal(await page.locator('#notificationList script, #notificationList img').count(),0);
    assert.match(await page.locator('#notificationList').textContent(),/<script>危险<\/script>/);
    assert.equal(await page.locator('#notificationCenterBadge').textContent(),'2');
    await page.getByRole('button',{name:'标为已读',exact:true}).click();
    await page.waitForFunction(() => document.querySelector('#notificationList button') === null);
    assert.equal(writes[0].method,'POST'); assert.equal(writes[0].token,'test-token'); assert.equal(writes[0].body,'id=12');
    await page.locator('#nextPage').click();
    await page.waitForFunction(() => document.querySelector('#pageInfo').textContent.includes('第 2'));
    assert.equal(await page.locator('#previousPage').isEnabled(),true);
    await page.locator('#markAllRead').click();
    await page.waitForFunction(() => document.querySelector('#notificationCenterBadge').hidden);
    assert.equal(await page.locator('#notificationCenterBadge').isVisible(),false);
    assert.equal(writes[1].path,'/notifications/read-all'); assert.equal(writes[1].token,'test-token');
    await page.route('**/notifications/list?*', route => route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({success:true,role:'DOCTOR',page:1,pageSize:10,total:1,items:[{id:15,title:'停诊通知',body:'请查看审批结果。',readAt:'2026-10-28',targetType:'LEAVE',targetId:'10'}]})}));
    await page.locator('#reloadNotifications').click();
    await page.getByRole('heading',{name:'停诊通知'}).waitFor();
    assert.equal(await page.locator('#notificationList a').getAttribute('href'),'/ustbhospital/clinic-workflow.html?leaveId=10');
    await page.route('**/notifications/unread-count', route => route.fulfill({status:401,contentType:'application/json',body:JSON.stringify({message:'请先登录。'})}));
    await page.evaluate(() => document.dispatchEvent(new Event('hospital:notifications-changed')));
    await page.waitForFunction(() => document.querySelector('#notificationCenterNavigation').hidden);
    assert.equal(await page.locator('#notificationCenterNavigation').isVisible(),false);
    console.log('PASS notification rendering, escaping, pagination, own read actions, CSRF, unread badge');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode=1; });
