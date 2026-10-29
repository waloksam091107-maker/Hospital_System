// Post-deployment GET-only smoke. Protected denials create normal security audits.
const {chromium}=require('playwright'),assert=require('node:assert/strict');
const base='http://127.0.0.1:8080/ustbhospital';
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.PLAYWRIGHT_BROWSER_PATH||'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'});
 try {
  const context=await browser.newContext(),page=await context.newPage(),errors=[];let count=0;
  page.on('pageerror',e=>errors.push(e.message));
  const ready=await context.request.get(base+'/health/ready');assert.equal(ready.status(),200);assert.equal((await ready.json()).ready,true);count++;
  const root=await context.request.get(base+'/');assert.equal(root.status(),200);count++;
  for(const route of ['/portal/profile','/appointments/patient','/clinic/queue','/notifications/list','/portal/admin/doctors','/portal/admin/income?date=2026-10-29']){
   const response=await context.request.get(base+route);assert.equal(response.status(),401,route);assert.equal((await response.json()).code,'LOGIN_REQUIRED');count++;
  }
  for(const route of ['/hospro/login.html','/hospro/register-page.html','/hospro/forgot-password.html','/hospro/index.html','/hospro/appointment-departments.html','/hospro/news.html']){
   const response=await page.goto(base+route,{waitUntil:'networkidle'});assert.equal(response.status(),200,route);count++;
  }
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+2),true);assert.deepEqual(errors,[]);count+=2;
  await context.close();console.log('PASS '+count+' read-only deployed checks: health, entry, public pages and authentication boundary; zero browser errors');
 }finally{await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
