const {chromium}=require('playwright'),fs=require('node:fs'),assert=require('node:assert/strict');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.PLAYWRIGHT_BROWSER_PATH||'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'});
 try{
  const page=await browser.newPage({viewport:{width:390,height:844}});const requests=[],errors=[];
  page.on('pageerror',e=>errors.push(e.message));
  await page.route('**/*',route=>{const url=new URL(route.request().url());requests.push(url.pathname);if(url.pathname.endsWith('/forgot-password.html')||url.pathname.endsWith('/confirm.html'))return route.fulfill({contentType:'text/html',body:fs.readFileSync('src/main/webapp'+url.pathname.replace('/ustbhospital',''))});return route.fulfill({status:404,body:''});});
  await page.goto('http://hospital.test/ustbhospital/hospro/forgot-password.html');
  assert.ok((await page.locator('body').innerText()).includes('临时密码'),'recovery must explain the existing administrator reset');
  assert.equal(await page.locator('form').count(),0,'recovery must not offer unsupported email/SMS submission');
  assert.ok(await page.locator('a[href="login.html"]').count()>0);assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+2),true);
  await page.goto('http://hospital.test/ustbhospital/doctor-schedule/confirm.html');
  assert.ok((await page.locator('body').innerText()).includes('预约成功'));
  assert.ok(await page.locator('a[href="../hospro/patient.jsp"]').count()>0);assert.ok(await page.locator('a[href="../notifications.html"]').count()>0);
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+2),true);assert.deepEqual(errors,[]);
  assert.deepEqual(requests,['/ustbhospital/hospro/forgot-password.html','/ustbhospital/doctor-schedule/confirm.html'],'informational pages must not require external resources');
  console.log('PASS working recovery instructions, no unavailable form, canonical confirmation links, mobile layout and zero external requests');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
