import fs from 'node:fs';
import {assert,api,status,register,login,room,join,start,current,spin,openWindow,answer,rows,scalar,sql,quote,caseTest as recordCase,delay,UI,OUT} from './common.mjs';
const engines=await import('/Users/lucasnavarro/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs');
const browserName=process.env.QA_BROWSER||'chromium';
const browser=await engines[browserName].launch({headless:true,...(browserName==='chromium'?{executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'}:{})});
const caseTest=(id,variant,fn)=>recordCase(id,variant+' ['+browserName+']',fn,{scope:'QA navegador '+browserName});
const u=await login(await register()),v=await login(await register());
const cat={id:Number(scalar('select id from categorias order by id limit 1'))};
async function context(user,extra={}){const ctx=await browser.newContext({ignoreHTTPSErrors:true,...extra});await ctx.addInitScript(user=>{for(const[k,value]of Object.entries({jwt:user.token,rol:'JUGADOR',idUsuario:user.id,nick:user.nick,email:user.email}))localStorage.setItem(k,String(value));},user);const page=await ctx.newPage();page.setDefaultTimeout(15000);return{ctx,page};}
const {ctx,page}=await context(u,{viewport:{width:360,height:800}});
fs.mkdirSync(OUT+'/screenshots',{recursive:true});
await caseTest('NAV-01','proxy QA devuelve salud JSON UP',async()=>{const response=await ctx.request.get(UI+'/actuator/health');assert.equal(response.status(),200);assert.match(response.headers()['content-type'],/json/);assert.equal((await response.json()).status,'UP');});
await caseTest('UI-06','login con error de red permite reintentar',async()=>{
  const anonymous=await browser.newContext({ignoreHTTPSErrors:true}),p=await anonymous.newPage();
  try{await p.route('**/api/auth/login',r=>r.abort());await p.goto(UI+'/login');await p.locator('#email').fill('qa@example.invalid');await p.locator('#password').fill('QA-password@123');await p.getByRole('button',{name:'Entrar',exact:true}).click();await p.getByText(/No se pudo conectar/).waitFor();assert.ok(await p.getByRole('button',{name:'Entrar',exact:true}).isEnabled());}finally{await anonymous.close();}
});
await caseTest('UI-03','controles mostrar/ocultar tienen nombre accesible',async()=>{
  const anon=await browser.newContext({ignoreHTTPSErrors:true}),p=await anon.newPage();try{for(const path of ['/login','register','forgot-password']){await p.goto(UI+'/'+path.replace(/^\//,''));const eyes=p.locator('.input-eye');for(let i=0;i<await eyes.count();i++)assert.ok(await eyes.nth(i).getAttribute('aria-label'));}}finally{await anon.close();}
});
await caseTest('UI-02','selección de categorías funciona con foco y espacio',async()=>{await page.goto(UI+'/crear/partida');const input=page.locator('.crear-partida__cat input').first();await input.waitFor({state:'attached'});await input.focus();const checked=await input.isChecked();await page.keyboard.press('Space');assert.equal(await input.isChecked(),!checked);assert.ok(await input.evaluate(e=>e===document.activeElement));});
await caseTest('CRE-07','UI permite límites 5 y 600 segundos',async()=>{await page.goto(UI+'/crear/partida');const input=page.locator('.crear-partida__field').filter({hasText:'Tiempo máximo de respuesta'}).locator('input[type=range]');assert.equal(await input.getAttribute('min'),'5');assert.equal(await input.getAttribute('max'),'600');});
await caseTest('WS-01','inicio con conexión no preparada converge por REST',async()=>{
  const g=await room(u,cat);status(await join(g,v),200);await page.goto(UI+`/partida/${g.idPartida}`);await page.getByRole('button',{name:/Empezar partida/}).waitFor();
  await ctx.setOffline(true);try{status(await start(g,u),200);}finally{await ctx.setOffline(false);}
  await page.locator('.juego__scoreboard').waitFor({timeout:12000});
});
await caseTest('FIN-05','recarga de partida terminada muestra resultado y ganador',async()=>{
  const g=await room(u,cat,{aciertosParaQuesito:1});status(await join(g,v),200);status(await start(g,u),200);
  const active=current(g)===u.id?u:v,q=await spin(g,active);openWindow(q);status(await answer(g,active,q,true),200);
  await page.goto(UI+`/partida/${g.idPartida}`);await page.getByText(/has ganado|ha ganado|Enhorabuena|partida terminada/i).first().waitFor();await page.reload();await page.getByText(/has ganado|ha ganado|Enhorabuena|partida terminada/i).first().waitFor();await page.screenshot({path:OUT+'/screenshots/final-reload-'+browserName+'.png',fullPage:true});
});
await caseTest('UI-01','estadísticas sin desbordamiento a 360px',async()=>{await page.goto(UI+'/jugador/estadisticas');await page.getByRole('heading',{name:'Mis estadísticas'}).waitFor();assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),JSON.stringify(await page.evaluate(()=>({width:innerWidth,scroll:document.documentElement.scrollWidth}))));await page.screenshot({path:OUT+'/screenshots/stats-360-'+browserName+'.png',fullPage:true});});
await caseTest('WS-09','veinte navegaciones no agotan límite de auth/me',async()=>{for(let i=0;i<20;i++){await page.goto(UI+(i%2?'/jugador':'/perfil'));await page.locator('main').waitFor();assert.ok((await page.locator('main').innerText()).trim().length>0);}});
await ctx.close();await browser.close();console.log('CORRECTIONS BROWSER finished');
