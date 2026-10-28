const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const webRoot = path.resolve(__dirname, '../src/main/webapp');

async function run() {
  const browser = await chromium.launch({headless:true,executablePath:process.env.PLAYWRIGHT_BROWSER_PATH || 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'});
  let passed = 0;
  const page = await browser.newPage();
  const mutations = [], queries = [];
  let denied = false;
  await page.route('**/*', async route => {
    const req = route.request(), url = new URL(req.url()), api = url.pathname.replace('/ustbhospital', '');
    const json = (body,status=200) => route.fulfill({status,contentType:'application/json',body:JSON.stringify(body)});
    if (api === '/security/csrf') return json({token:'account-csrf'});
    if (api.startsWith('/portal/admin/account-security/')) {
      if (req.method() === 'POST') { mutations.push({path:api,body:Object.fromEntries(new URLSearchParams(req.postData())),token:(await req.allHeaders())['x-csrf-token']});return json({success:true,message:'已更新'}); }
      queries.push(Object.fromEntries(url.searchParams));
      if (denied) return json({success:false,message:'无权访问'},403);
      const rows = url.searchParams.get('page') === '1' ? Array.from({length:20},(_,i)=>({id:i+1,name:i===0?'<img src=x onerror=alert(1)>':'账号'+i,enabled:true,mustChangePassword:i===0,lockedUntil:null})) : [];
      return json({success:true,items:rows});
    }
    const file=path.resolve(webRoot,decodeURIComponent(api).replace(/^\//,''));
    if (!file.startsWith(webRoot+path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) return route.fulfill({status:404,body:'Missing'});
    return route.fulfill({contentType:{'.html':'text/html','.js':'application/javascript','.css':'text/css'}[path.extname(file)]||'application/octet-stream',body:fs.readFileSync(file)});
  });
  try {
    await page.goto('http://hospital.test/ustbhospital/hospro/account-security.html');
    await page.locator('#accounts tr').first().waitFor();
    assert.equal(await page.locator('#accounts tr').count(),20);
    assert.equal(await page.locator('#accounts img').count(),0);
    assert.match(await page.locator('#accounts').innerText(),/须修改临时密码/);
    passed++;
    await page.getByRole('button',{name:'停用',exact:true}).first().click();
    await page.waitForFunction(()=>document.getElementById('message').textContent==='已更新');
    assert.deepEqual(mutations[0],{path:'/portal/admin/account-security/disable',body:{accountId:'1',role:'DOCTOR'},token:'account-csrf'});
    passed++;
    await page.getByRole('button',{name:'解除锁定',exact:true}).first().click();
    await page.waitForTimeout(60);
    assert.equal(mutations[1].path,'/portal/admin/account-security/unlock');
    passed++;
    await page.getByRole('button',{name:'重置密码',exact:true}).first().click();
    await page.locator('#temporary-password').fill('密'.repeat(25));
    await page.locator('#reset-form button[type=submit]').click();
    assert.equal(mutations.length,2);
    assert.match(await page.locator('#message').innerText(),/72 字节/);
    await page.locator('#temporary-password').fill('temporary-password');
    await page.locator('#reset-form button[type=submit]').click();
    await page.locator('#reset-form').waitFor({state:'hidden'});
    assert.equal(await page.locator('#temporary-password').inputValue(),'');
    assert.equal(mutations[2].body.temporaryPassword,'temporary-password');
    assert.equal(mutations[2].token,'account-csrf');
    assert.doesNotMatch(await page.locator('body').innerText(),/temporary-password/);
    passed++;
    await page.getByRole('button',{name:'下一页',exact:true}).click();
    await page.getByText('本页暂无账号',{exact:true}).waitFor();
    assert.equal(queries.at(-1).page,'2');
    await page.locator('#role').selectOption('PATIENT');
    await page.locator('#accounts tr').nth(19).waitFor();
    assert.deepEqual(queries.at(-1),{role:'PATIENT',page:'1',pageSize:'20'});
    passed++;
    denied=true;
    await page.getByRole('button',{name:'刷新',exact:true}).click();
    await page.getByText('无权访问',{exact:true}).waitFor();
    passed++;
    console.log(`Account security browser interactions passed: ${passed}/6`);
  } finally { await browser.close(); }
}
run().catch(error=>{console.error(error);process.exitCode=1;});
