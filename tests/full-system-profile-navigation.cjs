const {chromium}=require('playwright'),fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve('src/main/webapp');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.PLAYWRIGHT_BROWSER_PATH||'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'});
 try{
  const page=await browser.newPage();page.setDefaultTimeout(3000);let calls=[];
  await page.route('**/*',async route=>{
   const url=new URL(route.request().url()),api=url.pathname.replace(/^\/ustbhospital/,'');calls.push(api);
   const json=body=>route.fulfill({contentType:'application/json',body:JSON.stringify(body)});
   if(api==='/portal/profile')return json({role:'DOCTOR',id:8,name:'会话医生',avatar:null});
   if(api==='/security/csrf')return json({token:'csrf'});
   if(api==='/notifications/unread-count')return json({unreadCount:0});
   if(api==='/schedules/mine'||api==='/appointments/patient')return json({list:[],pages:0,pageNum:1});
   if(api==='/patients/current')return json({patientId:8,name:'会话患者',avatar:null});
   if(api==='/hospro/portal-profile.html')return route.fulfill({contentType:'text/html',body:'<h1>会话资料</h1>'});
   const file=path.resolve(root,api.replace(/^\//,''));
   if(!file.startsWith(root+path.sep)||!fs.existsSync(file)||!fs.statSync(file).isFile())return route.fulfill({status:404,body:'Missing'});
   let body=fs.readFileSync(file);if(file.endsWith('.jsp'))body=body.toString().replace(/<%@[^%]*%>/g,'');
   return route.fulfill({contentType:({'.html':'text/html','.jsp':'text/html','.js':'application/javascript','.css':'text/css','.png':'image/png'})[path.extname(file)]||'application/octet-stream',body});
  });
  await page.goto('http://hospital.test/ustbhospital/doctor-schedule/date.html?doctorId=999');await page.waitForLoadState('networkidle');
  assert.equal(await page.locator('#doctorNameLabel').innerText(),'会话医生','schedule identity must come from session, even with a forged URL doctorId');
  assert.equal(await page.locator('.author-box .author').innerText(),'会话医生','header must not display a fictional template doctor');
  assert.ok(!calls.includes('/doctors/details'),'schedule must not query arbitrary doctor profiles');
  assert.ok((await page.locator('#doctorAvatarImg').getAttribute('src')).includes('author-1.png'),'missing doctor avatar uses existing fallback');
  calls=[];await page.goto('http://hospital.test/ustbhospital/doctor-schedule/date.html');await page.waitForLoadState('networkidle');assert.equal(await page.locator('#doctorNameLabel').innerText(),'会话医生');
  await page.goto('http://hospital.test/ustbhospital/hospro/patient.jsp');await page.waitForLoadState('networkidle');
  assert.equal(await page.locator('.author-box .author').innerText(),'会话患者','patient header must show the authenticated patient');
  assert.ok((await page.locator('#patientAvatarImg').getAttribute('src')).includes('author-1.png'),'missing patient avatar uses existing fallback');
  await page.goto('http://hospital.test/ustbhospital/hospro/doctor-profile.html?doctorId=999');await page.waitForURL('**/hospro/portal-profile.html');
  console.log('PASS session-owned schedule identity with and without URL parameters, null avatars, canonical profile redirect');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
