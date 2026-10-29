// Runs the real home page and assets. API doubles describe the current safe
// endpoints so navigation can be checked without touching a database.
const {chromium}=require('playwright');
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'../src/main/webapp');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.PLAYWRIGHT_BROWSER_PATH||'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'});
 const results=[];
 try {
  for(const [role,code] of [['PATIENT',2],['ADMIN',3],['DOCTOR',1]]) {
   const context=await browser.newContext(),page=await context.newPage(),errors=[],requests=[];
   let access='normal';
   page.on('pageerror',e=>{if(new URL(page.url()).pathname.endsWith('/index.html'))errors.push(e.message);});
   await page.route('**/*',async route=>{
    const url=new URL(route.request().url()),api=url.pathname.replace(/^\/ustbhospital/,'');
    const json=(body,status=200)=>route.fulfill({status,contentType:'application/json',body:JSON.stringify(body)});
    if(api==='/portal/profile')return access==='required'?json({code:'PASSWORD_CHANGE_REQUIRED'},403):access==='anonymous'?json({code:'LOGIN_REQUIRED'},401):json({id:123,role,name:'测试账号',avatar:null});
    if(api==='/notifications/unread-count')return json({unreadCount:0});
    if(api==='/patients/select'||api==='/admins/list-by-id'){requests.push(api);return json({code:'LEGACY_WRITE_DISABLED'},410);}
    if(api==='/doctor/list'){requests.push(api);return json({doctorId:123,name:'测试账号',avatar:null});}
    if(api==='/announcements/list')return json([{title:'正式发布公告',content:'<img src=x onerror=alert(1)>安全的公告内容',publishedAt:'2026-10-29'}]);
    if(api==='/first'||api==='/second'||api==='/find')return json([]);
    const file=path.resolve(root,api.replace(/^\//,''));
    if(file.startsWith(root+path.sep)&&fs.existsSync(file)&&fs.statSync(file).isFile())return route.fulfill({contentType:({'.html':'text/html','.js':'application/javascript','.css':'text/css','.jpg':'image/jpeg','.png':'image/png','.svg':'image/svg+xml','.woff':'font/woff','.woff2':'font/woff2'})[path.extname(file)]||'application/octet-stream',body:fs.readFileSync(file)});
    return route.fulfill({status:404,body:'Missing'});
   });
   await page.goto('http://hospital.test/ustbhospital/hospro/index.html?roleCode='+code+'&'+role.toLowerCase()+'Id=123');
   await page.waitForLoadState('networkidle');
   const links=await page.locator('.main-menu a[href]').evaluateAll(links=>links.map(a=>({text:a.textContent.trim(),href:a.getAttribute('href')})));
   results.push({role,errors,legacyIdentityRequests:requests,escapingApplicationLinks:links.filter(a=>a.href.startsWith('/')&&!a.href.startsWith('/ustbhospital/')),unavailableCoreLinks:await page.locator('#hzxinxi[aria-disabled=true],#hzjiuzhen[aria-disabled=true],#jiuzhen[aria-disabled=true]').count(),liveAnnouncement:await page.locator('#news-container').innerText()});
   await page.goto('http://hospital.test/ustbhospital/hospro/index.html?roleCode=999&patientId=999');
   await page.waitForLoadState('networkidle');
   assert.ok(await page.locator('[data-role-menu="'+role+'"]:visible').count()>0,'session must drive role menu even with unrelated URL parameters');
   assert.deepEqual(await page.locator('[data-role-menu]:visible').evaluateAll(items=>[...new Set(items.map(item=>item.dataset.roleMenu))]),[role]);
   assert.equal(await page.locator('#news-container img').count(),0,'announcement content must be plain text');
   const notificationEntry=page.getByRole('link',{name:/^打开站内通知/});
   assert.equal(await notificationEntry.count(),1,'home reminders must open the implemented notification center instead of posting an unsupported subscription');
   await notificationEntry.click();await page.waitForURL('**/notifications.html');
   await page.goto('http://hospital.test/ustbhospital/hospro/index.html');await page.waitForLoadState('networkidle');
   await page.setViewportSize({width:390,height:844});
   await page.locator('.mobile-nav-toggler').first().click();
   await page.locator('.mobile-menu [data-role-menu="'+role+'"]').waitFor({state:'visible'});
   access='anonymous';await page.goto('http://hospital.test/ustbhospital/hospro/index.html');await page.waitForLoadState('networkidle');
   assert.equal(await page.locator('[data-role-menu]:visible').count(),0,'anonymous visitors must not receive role menus');
   assert.ok(await page.locator('.author-box a[href="login.html"]').count()>0,'anonymous home must expose login');
   access='required';await page.goto('http://hospital.test/ustbhospital/hospro/index.html');await page.waitForURL('**/change-password.html?required=true');
   await context.close();
  }
  console.log(JSON.stringify(results,null,2));
  assert.deepEqual(results.flatMap(r=>r.errors),[],'home page must initialize for all authenticated roles');
  assert.deepEqual(results.flatMap(r=>r.legacyIdentityRequests),[],'home page must use session-owned profile');
  assert.deepEqual(results.flatMap(r=>r.escapingApplicationLinks),[],'menu links must preserve application context');
  assert.ok(results.every(r=>r.unavailableCoreLinks===0),'core links must remain available after template guard');
  assert.ok(results.every(r=>r.liveAnnouncement.includes('正式发布公告')),'home must display actual published announcements');
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
