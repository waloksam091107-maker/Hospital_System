const { chromium } = require('playwright');
const fs = require('fs'), assert = require('node:assert/strict');
const f = JSON.parse(fs.readFileSync('.hospital-local/five-extensions-http-fixture.json','utf8').replace(/^\uFEFF/,''));
assert.match(f.prefix, /^http_[a-f0-9]{10}$/);
const base='http://127.0.0.1:18081/ustbhospital';
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.PLAYWRIGHT_BROWSER_PATH || 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'});
 try{
  for(const [role,text] of [[1,'医生'],[2,'患者'],[3,'管理员']]){
   const context=await browser.newContext();
   await context.request.post(base+'/auth/login',{form:{roleCode:String(role),user:f.prefix,password:f.password}});
   const page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));
   await page.goto(base+'/clinic-workflow.html');await page.locator('#identity').filter({hasText:text}).waitFor();
   await page.waitForLoadState('networkidle'); assert.equal(await page.locator('#message').evaluate(e=>e.classList.contains('error')),false,await page.locator('#message').innerText());
   assert.equal(await page.locator('#capacityPanel').isVisible(),role===1);
   assert.equal(await page.locator('#leavePanel').isVisible(),role!==2);
   await page.goto(base+'/notifications.html');await page.locator('h1').waitFor();await page.waitForLoadState('networkidle');
   assert.equal(errors.length,0,errors.join('; '));await context.close();
  }
  async function login(role, user=f.prefix) {
   const ctx=await browser.newContext();
   const response=await ctx.request.post(base+'/auth/login',{form:{roleCode:String(role),user,password:f.password}});
   assert.ok(!response.url().includes('login_failed'),'live UI login failed');return ctx;
  }
  async function settle(page) { await page.waitForLoadState('networkidle'); }
  function dialogs(page) { page.on('dialog',d=>d.accept(d.type()==='prompt'?'实际页面测试审核意见':undefined)); }
  const patient=await login(2,f.prefix+'-third'), doctor=await login(1), admin=await login(3);
  try {
   const queue=await (await patient.request.get(base+'/clinic/queue')).json();
   const active=queue.list.find(a=>a.doctorId===f.doctor&&a.appointmentDate===f.today&&a.status==='BOOKED');
   assert.ok(active,'run HTTP fixture test before live UI flow');
   const patientPage=await patient.newPage();dialogs(patientPage);
   await patientPage.goto(base+'/clinic-workflow.html');await settle(patientPage);
   const patientRow=patientPage.locator('#queueRows tr').filter({has:patientPage.getByRole('cell',{name:f.prefix+'-third / '+active.appointmentId,exact:true})});
   await patientRow.getByRole('button',{name:'签到',exact:true}).click();
   await patientRow.getByText('候诊中',{exact:true}).waitFor();
   const doctorPage=await doctor.newPage();dialogs(doctorPage);
   await doctorPage.goto(base+'/clinic-workflow.html');await settle(doctorPage);
   await doctorPage.locator('#queuePeriod').selectOption('EVENING');await doctorPage.locator('#refreshQueue').click();
   const doctorRow=doctorPage.locator('#queueRows tr').filter({has:doctorPage.getByRole('cell',{name:f.prefix+'-third / '+active.appointmentId,exact:true})});
   await doctorRow.getByText('候诊中',{exact:true}).waitFor();
   await doctorPage.locator('#callNext').click();await doctorRow.getByText('已叫号',{exact:true}).waitFor();
   await doctorRow.getByRole('button',{name:'过号',exact:true}).click();await doctorRow.getByText('已过号',{exact:true}).waitFor();
   await doctorRow.getByRole('button',{name:'恢复候诊',exact:true}).click();await doctorRow.getByText('候诊中',{exact:true}).waitFor();
   await doctorPage.locator('#callNext').click();await doctorRow.getByText('已叫号',{exact:true}).waitFor();
   await doctorRow.getByRole('button',{name:'开始接诊',exact:true}).click();await doctorPage.waitForURL('**/encounter.html?appointmentId='+active.appointmentId);
   await doctorPage.locator('#diagnosis:enabled').fill('实际页面测试诊断');
   await doctorPage.locator('#treatmentPlan').fill('实际页面测试诊疗方案');
   await doctorPage.locator('#addOrder').click();await doctorPage.locator('[name=orderItem]').fill('实际页面测试医嘱');
   await doctorPage.locator('[name=orderQuantity]').fill('1');
   await doctorPage.locator('#draftButton').click();
   await doctorPage.waitForFunction(()=>document.querySelector('#saveStatus').textContent.includes('最近保存'));
   await doctorPage.reload();await settle(doctorPage);assert.equal(await doctorPage.locator('#diagnosis').inputValue(),'实际页面测试诊断');
   await doctorPage.locator('#submitButton').click();await doctorPage.waitForURL('**/appointments.html');
   const result=await (await patient.request.get(base+'/clinic/queue')).json();
   assert.equal(result.list.find(a=>a.appointmentId===active.appointmentId).queueStatus,'DONE');
   const medical=await (await patient.request.get(base+'/medical-records/patient?appointmentId='+active.appointmentId)).json();
   assert.equal(medical.diagnosis,'实际页面测试诊断');assert.equal(medical.orders.length,1);
   await patientPage.goto(base+'/notifications.html');await settle(patientPage);
   assert.ok((await patientPage.locator('body').textContent()).includes('就诊记录已完成'));
   await doctorPage.goto(base+'/clinic-workflow.html');await settle(doctorPage);
   const capacityRow=doctorPage.locator('#slotRows tr').filter({hasText:'晚间'});
   await capacityRow.locator('input[name=capacity]').fill('3');await capacityRow.getByRole('button',{name:'保存容量'}).click();
   await capacityRow.getByText('容量 3 / 已占用 2 / 余号 1',{exact:true}).waitFor();
   const extraDay=new Date(f.today+'T12:00:00+08:00');extraDay.setDate(extraDay.getDate()+3);
   const date=extraDay.toISOString().slice(0,10);
   await doctorPage.locator('#leaveStart').fill(date);await doctorPage.locator('#leaveEnd').fill(date);
   const reason='实际页面申请停诊 '+f.prefix;
   await doctorPage.locator('#leaveReason').fill(reason);await doctorPage.getByRole('button',{name:'提交停诊申请'}).click();
   await doctorPage.locator('#leaveRows tr').filter({hasText:reason}).getByText('待审核',{exact:true}).waitFor();
   const adminPage=await admin.newPage();dialogs(adminPage);await adminPage.goto(base+'/clinic-workflow.html');await settle(adminPage);
   const leaveRow=adminPage.locator('#leaveRows tr').filter({hasText:reason});await leaveRow.getByRole('button',{name:'批准',exact:true}).click();
   await leaveRow.getByText(/已批准停诊/).waitFor();
   await adminPage.goto(base+'/hospro/account-security.html');await settle(adminPage);
   const accountRow=adminPage.locator('#accounts tr').filter({has:adminPage.locator('td').getByText(String(f.doctor),{exact:true})});
   while(await accountRow.count()===0&&await adminPage.locator('#next').isEnabled()){await adminPage.locator('#next').click();await settle(adminPage);}
   await accountRow.getByRole('button',{name:'停用',exact:true}).click();await accountRow.getByRole('button',{name:'启用',exact:true}).waitFor();
   assert.equal((await doctor.request.get(base+'/clinic/queue')).status(),401);
   await accountRow.getByRole('button',{name:'启用',exact:true}).click();await accountRow.getByRole('button',{name:'停用',exact:true}).waitFor();
   assert.equal((await doctor.request.get(base+'/clinic/queue')).status(),401);
  } finally {await patient.close();await doctor.close();await admin.close();}
  console.log('PASS live browser: 3 role views plus real check-in, call, skip, requeue, encounter draft/final/orders, notification, capacity, leave approval and account enable/disable workflows');
 }finally{await browser.close();}
})().catch(e=>{console.error(e.message);process.exitCode=1});
