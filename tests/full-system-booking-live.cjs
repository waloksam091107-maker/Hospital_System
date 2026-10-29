// A complete visible booking journey against the disposable 18081 environment.
const {chromium}=require('playwright'),fs=require('node:fs'),assert=require('node:assert/strict');
const f=JSON.parse(fs.readFileSync('.hospital-local/five-extensions-http-fixture.json','utf8').replace(/^\uFEFF/,''));
assert.match(f.prefix,/^http_[a-f0-9]{10}$/);
const base='http://127.0.0.1:18081/ustbhospital';
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.PLAYWRIGHT_BROWSER_PATH||'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'}),contexts=[];
 try {
  async function login(role,user){const c=await browser.newContext();contexts.push(c);const r=await c.request.post(base+'/auth/login',{form:{roleCode:String(role),user,password:f.password}});assert.ok(!r.url().includes('login_failed'));c.token=(await(await c.request.get(base+'/security/csrf')).json()).token;return c;}
  async function post(c,route,form){const r=await c.request.post(base+route,{form,headers:{'X-CSRF-Token':c.token}});assert.equal(r.status(),200,route);assert.equal((await r.json()).success,true,route);}
  const doctor=await login(1,f.prefix),doctorInfo=await(await doctor.request.get(base+'/doc/findById?doctorId='+f.doctor)).json();
  const when=new Date(f.today+'T12:00:00+08:00');when.setDate(when.getDate()+7);let date;
  for(let attempt=0;attempt<31;attempt++){
   date=when.toISOString().slice(0,10);
   const existing=await(await doctor.request.get(base+'/clinic/slots?date='+date)).json();
   if(existing.list.length===0)break;when.setDate(when.getDate()+1);date=null;
  }
  assert.ok(date,'a disposable future schedule day is required');
  await post(doctor,'/schedules/create',{date,shang:'1',xia:'0',wan:'0'});
  const guest=await browser.newContext();contexts.push(guest);const now=String(Date.now()),name=f.prefix+'-booking-'+now;
  const token=(await(await guest.request.get(base+'/portal/accounts/csrf')).json()).token;
  const registration=await guest.request.post(base+'/portal/accounts/patient-register',{form:{name,idCardNumber:'110101'+now.slice(-12).padStart(12,'0'),password:f.password},headers:{'X-CSRF-Token':token}});
  assert.equal(registration.status(),200);assert.equal((await registration.json()).success,true);
  const patient=await login(2,name);await post(patient,'/portal/balance/recharge',{amount:'50.00'});
  const page=await patient.newPage(),errors=[];page.on('pageerror',error=>errors.push(error.message));page.on('dialog',dialog=>dialog.accept());
  await page.goto(base+'/hospro/appointment-departments.html');
  await page.locator('#departments a').filter({hasText:f.prefix}).first().click();
  await page.waitForURL('**/doctor-directory.html?**');
  await page.locator('.clinic-list-content .name a').filter({hasText:doctorInfo.name}).first().click();
  await page.waitForURL('**/doctors-details.html?doctorId='+f.doctor);
  await page.locator('#datepicker').fill(date);await page.locator('input[name=appointmentTime][value=AM]').check();
  await page.locator('#book-btn:enabled').click();await page.waitForURL('**/book-appointment.html?**');
  await page.locator('[id="005"]:enabled').waitFor();assert.equal(await page.locator('#docname').textContent(),doctorInfo.name);
  assert.equal(await page.locator('[id="004"]').textContent(),'¥'+Number(doctorInfo.registrationFee).toFixed(2));
  await page.locator('[id="005"]').click();await page.waitForURL('**/doctor-schedule/confirm.html');
  await page.getByRole('link',{name:'查看我的预约',exact:true}).click();await page.waitForURL('**/hospro/patient.jsp');
  const row=page.locator('#appBody tr').filter({hasText:date});await row.getByText('已预约',{exact:false}).waitFor();
  const profile=await(await patient.request.get(base+'/portal/profile')).json();assert.equal(Number(profile.balance),40);
  await row.getByRole('button',{name:'取消',exact:false}).click();await row.getByText('已取消',{exact:false}).waitFor();
  assert.equal(Number((await(await patient.request.get(base+'/portal/profile')).json()).balance),40);
  const slots=(await(await patient.request.get(base+'/clinic/slots?'+new URLSearchParams({doctorId:String(f.doctor),date}))).json()).list;
  assert.equal(slots.find(s=>s.slotPeriod==='AM').occupiedCount,0);assert.deepEqual(errors,[]);
  console.log('PASS visible department → doctor → availability → real-fee confirmation → booking → own records → cancel; one debit, no automatic refund, capacity restored, no browser errors');
 }finally{for(const c of contexts)await c.close();await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
