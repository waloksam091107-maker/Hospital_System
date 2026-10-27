// Run against the actual page with controlled HTTP responses. No business server is modified.
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
(async () => {
  const browser = await chromium.launch({headless:true,executablePath:process.env.PLAYWRIGHT_BROWSER_PATH || 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'});
  let failures=0;
  const cases=[
    ['科室说明回填并保留编辑内容',async page=>{
      await page.locator('[data-target=departments]').click();await page.locator('#departments tbody button').first().click();
      assert.equal(await page.locator('[name=departmentDescription]').inputValue(),'原说明');
    }],
    ['选项独立完整加载，显示名称',async page=>{
      assert.equal(await page.locator('select[name=departmentId]').count(),1);
      assert.equal(await page.locator('select[name=departmentId] option[value="25"]').textContent(),'科室25');
      assert.match(await page.locator('#doctors tbody').textContent(),/科室25/);
    }],
    ['搜索走服务端且分页保留关键字',async(page,requests)=>{
      assert.equal(await page.locator('#doctors .search-keyword').count(),1);
      await page.locator('#doctors .search-keyword').fill('张医生');await page.locator('#doctors .search-form button').click();
      await page.waitForFunction(()=>document.querySelector('#doctors .page-info').textContent.includes('共 11'));
      await page.locator('#doctors .next-page').click();
      await page.waitForFunction(()=>document.querySelector('#doctors .page-info').textContent.includes('第 2'));
      assert(requests.some(u=>u.includes('keyword=') && u.includes('pageNum=2')));
    }],
    ['保存成功消息保留，HTTP失败不清空表单',async page=>{
      await page.locator('[data-target=departments]').click();await page.locator('#departments tbody button').first().click();
      await page.locator('#departments form[data-resource] button[type=submit]').click();
      await page.waitForFunction(()=>document.querySelector('#departments .result').textContent.includes('保存成功'));
      assert.match(await page.locator('#departments .result').textContent(),/保存成功/);
      await page.locator('[name=departmentName]').fill('失败输入');await page.locator('#departments form[data-resource] button[type=submit]').click();
      await page.waitForFunction(()=>document.querySelector('#departments .result').textContent.includes('被引用'));
      assert.equal(await page.locator('[name=departmentName]').inputValue(),'失败输入');
    }]
  ];
  for(const [name,test] of cases){const page=await browser.newPage();const requests=[];
    try{
      await page.route('**/*',route=>{
        const req=route.request(),url=new URL(req.url());requests.push(url.search);
        const json=(body,status=200)=>route.fulfill({status,contentType:'application/json',body:JSON.stringify(body)});
        if(url.pathname.endsWith('/security/csrf'))return json({token:'test-token'});
        if(url.pathname.includes('/portal/admin/')){
          if(req.method()==='POST')return req.postData().includes(encodeURIComponent('失败输入'))?json({message:'被引用'},409):json({message:'保存成功'});
          const departments=Array.from({length:25},(_,i)=>({departmentId:i+1,departmentName:'科室'+(i+1),departmentDiscription:'原说明'}));
          if(url.searchParams.get('options')==='true')return json({items:url.pathname.endsWith('departments')?departments:[{id:1,titleName:'主任医师'}]});
          const items=url.pathname.endsWith('departments')?[departments[0]]:url.pathname.endsWith('doctors')?[{doctorId:1,name:'张医生',departmentId:25,professionalTitleId:1}]:[];
          return json({items,pageNum:Number(url.searchParams.get('pageNum')||1),pageSize:10,total:11,pages:2});
        }
        return route.fulfill({contentType:'text/html;charset=utf-8',body:fs.readFileSync(path.join(__dirname,'../src/main/webapp/hospro/admin-master-data.html'))});
      });
      await page.goto('http://hospital.test/ustbhospital/hospro/admin-master-data.html');await page.locator('#doctors tbody tr').first().waitFor();
      await test(page,requests);console.log('PASS '+name);
    }catch(e){failures++;console.error('FAIL '+name+': '+e.message);}finally{await page.close();}
  }
  await browser.close();assert.equal(failures,0,failures+' admin UI failures');
})().catch(e=>{console.error(e);process.exitCode=1;});
