// Isolated fixture only. Verifies target navigation beyond the first page and ownership.
const {chromium}=require('playwright'),fs=require('node:fs'),assert=require('node:assert/strict');
const f=JSON.parse(fs.readFileSync('.hospital-local/five-extensions-http-fixture.json','utf8'));
assert.match(f.prefix,/^http_[a-f0-9]{10}$/);
const base='http://127.0.0.1:18081/ustbhospital';
(async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true});
 try{
  for(const [role,user,endpoint,route,body] of [[2,f.prefix,'patient','hospro/patient.jsp','#appBody'],[1,f.prefix,'doctor','doctor-schedule/appointments.html','#appointmentRows']]){
   const c=await browser.newContext();
   await c.request.post(base+'/auth/login',{form:{roleCode:String(role),user,password:f.password}});
   const first=await(await c.request.get(base+'/appointments/'+endpoint)).json();
   const last=await(await c.request.get(base+'/appointments/'+endpoint+'?pageNum='+Math.max(1,first.pages))).json();
   const id=last.list.at(-1).appointmentId;
   const target=await c.request.get(base+'/appointments/'+endpoint+'?appointmentId='+id);
   assert.equal(target.status(),200);assert.deepEqual((await target.json()).list.map(a=>a.appointmentId),[id]);
   for(const [input,status] of [['bad',400],['0',400],['2147483647',404]])assert.equal((await c.request.get(base+'/appointments/'+endpoint+'?appointmentId='+input)).status(),status);
   const page=await c.newPage();await page.goto(base+'/'+route+'?appointmentId='+id);await page.waitForLoadState('networkidle');
   assert.equal(await page.locator(body+' tr').count(),1);
   await page.goto(base+'/'+route+'?appointmentId=2147483647');await page.getByText('预约不存在或无权查看。',{exact:true}).waitFor();
   await c.close();
  }
  const other=await browser.newContext();await other.request.post(base+'/auth/login',{form:{roleCode:'2',user:f.prefix+'-other',password:f.password}});
  const doctor=await browser.newContext();await doctor.request.post(base+'/auth/login',{form:{roleCode:'1',user:f.prefix,password:f.password}});
  const page=await(await doctor.request.get(base+'/appointments/doctor')).json();
  const own=page.list.find(a=>a.patients && a.patients.name===f.prefix);
  assert.ok(own);assert.equal((await other.request.get(base+'/appointments/patient?appointmentId='+own.appointmentId)).status(),404);
  await other.close();await doctor.close();
  console.log('PASS exact patient/doctor notification targets, invalid IDs, missing/foreign records and visible errors');
 }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exitCode=1});
