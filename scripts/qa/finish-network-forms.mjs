import {assert,register,caseTest,delay,UI,PASSWORD} from './common.mjs';
const {chromium,firefox,webkit}=await import('/Users/lucasnavarro/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs');
for (const [engineName, engine, options] of [['Chrome',chromium,{executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'}],['Firefox',firefox,{}],['WebKit',webkit,{}]]) {
const browser=await engine.launch({headless:true,...options});
for(const kind of ['register','forgot','reset','verify'])await caseTest('UI-06',`${engineName}: fallo de red durante ${kind} permite reintentar`,async()=>{
 const ctx=await browser.newContext({ignoreHTTPSErrors:true}),page=await ctx.newPage();page.setDefaultTimeout(5000);try{
 if(kind==='register'){await page.goto(UI+'/register');for(const[key,value]of Object.entries({nombre:'QA',apellidos:'Network',email:`network${Date.now()}@example.invalid`,password:PASSWORD,confirmar:PASSWORD,nick:'network_'+Date.now()}))await page.locator('#'+key).fill(value);await page.route('**/api/auth/register/jugador',r=>r.abort());await page.getByRole('button',{name:'Crear cuenta',exact:true}).click();}
 if(kind==='forgot'){await page.goto(UI+'/forgot-password');await page.locator('#email').fill('network@example.invalid');await page.route('**/api/auth/forgot-password',r=>r.abort());await page.getByRole('button',{name:'Enviar',exact:true}).click();}
 if(kind==='reset'){await page.goto(UI+'/reset-password?token=qa-network');for(const input of await page.locator('input').all())await input.fill(PASSWORD);await page.route('**/api/auth/reset-password',r=>r.abort());await page.getByRole('button',{name:/Restablecer|Cambiar/i}).last().click();}
 if(kind==='verify'){const u=await register();await page.goto(UI+'/login');await page.locator('#email').fill(u.email);await page.locator('#password').fill(PASSWORD);await page.getByRole('button',{name:'Entrar',exact:true}).click();await page.getByPlaceholder('Código 6 dígitos').waitFor();await page.route('**/api/auth/verify',r=>r.abort());await page.getByPlaceholder('Código 6 dígitos').fill('000123');await page.getByRole('button',{name:'Verificar',exact:true}).click();}
 await page.waitForFunction(()=>{const button=document.querySelector('button[type=submit]');return button&&!button.disabled;},{timeout:5000});await page.locator('.auth-form__error').waitFor();assert.ok((await page.locator('.auth-form__error').innerText()).length>0);
 }finally{await ctx.close();}
});
await browser.close();}
console.log('FINISH NETWORK FORMS completed');
