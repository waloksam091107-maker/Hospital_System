const {chromium}=require('playwright'),fs=require('node:fs'),assert=require('node:assert/strict');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.PLAYWRIGHT_BROWSER_PATH||'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'});
 try{
  const page=await browser.newPage({viewport:{width:390,height:844}}),errors=[],queries=[];let mode='normal';
  page.on('pageerror',e=>errors.push(e.message));
  await page.route('**/*',async route=>{
   const url=new URL(route.request().url());
   if(url.pathname==='/ustbhospital/hospro/sum.html')return route.fulfill({contentType:'text/html',body:fs.readFileSync('src/main/webapp/hospro/sum.html')});
   if(url.pathname==='/ustbhospital/portal/admin/income'){
    queries.push(url.searchParams.get('date'));
    return route.fulfill({status:mode==='denied'?403:200,contentType:'application/json',body:JSON.stringify(mode==='denied'?{message:'当前账号无权执行此操作。'}:{items:[{transactionId:1,appointmentId:3,doctorName:'<img src=x onerror=alert(1)>',patientName:'患者',amount:12.5}],total:12.5})});
   }
   return route.fulfill({status:404,body:''});
  });
  await page.goto('http://hospital.test/ustbhospital/hospro/sum.html');await page.locator('#date').fill('2026-10-29');await page.locator('#queryButton').click();
  await page.getByText('12.50 元',{exact:true}).first().waitFor();assert.deepEqual(queries,['2026-10-29']);assert.equal(await page.locator('#report img').count(),0);assert.ok((await page.locator('#report').innerText()).includes('<img src=x onerror=alert(1)>'));assert.deepEqual(errors,[]);
  mode='denied';await page.locator('#queryButton').click();await page.getByText('当前账号无权执行此操作。',{exact:true}).waitFor();assert.equal(await page.locator('#report table').count(),0);
  console.log('PASS income report context path, date query, amounts, escaped names, denied response and mobile layout');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
