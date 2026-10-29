// Opt-in real OceanBase day-boundary checks. The fixture is prepared only in a
// restricted disposable hospital_test_ database; no production URL is accepted.
const {chromium}=require('playwright');
const fs=require('node:fs'),assert=require('node:assert/strict');
const f=JSON.parse(fs.readFileSync('.hospital-local/five-extensions-http-fixture.json','utf8').replace(/^\uFEFF/,''));
const boundary=JSON.parse(fs.readFileSync('.hospital-local/full-system-income-boundary.json','utf8'));
assert.match(f.prefix,/^http_[a-f0-9]{10}$/);assert.equal(boundary.prefix,f.prefix);
const base='http://127.0.0.1:18081/ustbhospital';
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.PLAYWRIGHT_BROWSER_PATH||'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'});
 const clients=[];let checks=0;
 async function status(response,expected){assert.equal(response.status(),expected,response.url());checks++;return response;}
 try {
  const anonymous=await browser.newContext();clients.push(anonymous);
  await status(await anonymous.request.get(base+'/portal/admin/income?date='+boundary.date),401);
  for(const role of [1,2,3]){
   const client=await browser.newContext();clients.push(client);
   const login=await client.request.post(base+'/auth/login',{form:{roleCode:String(role),user:f.prefix,password:f.password}});
   assert.ok(!login.url().includes('login_failed'));checks++;
   for(const endpoint of ['/portal/patients','/patient/list','/portal/admin/income?date='+boundary.date])
    await status(await client.request.get(base+endpoint),role===3?200:403);
   if(role!==3)continue;
   const report=await (await status(await client.request.get(base+'/portal/admin/income?date='+boundary.date),200)).json();
   assert.equal(report.timezone,'Asia/Shanghai');assert.equal(Number(report.total),19.75);assert.equal(report.items.length,2);checks+=3;
   assert.deepEqual(report.items.map(row=>Number(row.amount)).sort((a,b)=>a-b),[7.25,12.5]);checks++;
   for(const date of ['2035-02-30','2035-1-2','bad'])await status(await client.request.get(base+'/portal/admin/income?date='+date),400);
   await status(await client.request.get(base+'/portal/patients?pageNum=bad'),400);
   await status(await client.request.get(base+'/portal/patients?pageSize=101'),400);
   const page=await client.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));
   await page.goto(base+'/hospro/sum.html');await page.locator('#date').fill(boundary.date);await page.locator('#queryButton').click();
   await page.getByText('当日总收入：19.75 元',{exact:true}).waitFor();
   assert.equal(await page.locator('#report tbody tr').count(),2);assert.deepEqual(errors,[]);checks+=2;
  }
  const avatar=JSON.parse(fs.readFileSync('.hospital-local/full-system-portal-avatar.json','utf8'));
  const response=await status(await anonymous.request.get(avatar.base+'/'+avatar.path.replace(/^\//,'')),200);
  assert.equal((await response.body()).toString('base64'),avatar.base64);checks++;
  console.log('PASS '+checks+' actual income/authorization checks: OceanBase DATETIME midnight boundaries, recharge exclusion, Shanghai date, cancelled payment retained, browser report and persisted avatar');
 }finally{for(const client of clients)await client.close();await browser.close();}
})().catch(error=>{console.error(error.message);process.exitCode=1;});
