import fs from 'node:fs';
import {assert,caseTest,OUT} from './common.mjs';
const {chromium,firefox,webkit}=await import('/Users/lucasnavarro/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs');
fs.mkdirSync(OUT+'/screenshots',{recursive:true});
const URL='https://jugamir.es';
for(const [name,engine,options]of [['Chrome',chromium,{executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'}],['Firefox',firefox,{}],['WebKit',webkit,{}]]){
 let browser;
 try{browser=await engine.launch({headless:true,...options});}
 catch(e){console.log(name+' UNAVAILABLE '+e.message);continue;}
 const context=await browser.newContext(),page=await context.newPage();page.setDefaultTimeout(8000);
 const version=browser.version();
 await caseTest('NAV-01',`${name} ${version} dominio HTTPS y health`,async()=>{const r=await page.goto(URL);assert.equal(r.status(),200);await page.locator('#root').waitFor();const health=await page.request.get(URL+'/actuator/health');assert.equal(health.status(),200);assert.ok(health.headers()['content-type'].includes('application/json'),`health devuelve ${health.headers()['content-type']}`);assert.equal((await health.json()).status,'UP');},{scope:'Producción, navegador real de escritorio'});
 await caseTest('NAV-02',`${name} HTTP conserva /login`,async()=>{const r=await page.request.get('http://jugamir.es/login',{maxRedirects:0});assert.ok([301,302,307,308].includes(r.status()));assert.equal(r.headers().location,URL+'/login');},{scope:'Producción, lectura'});
 for(const path of ['/login','/register','/forgot-password'])await caseTest('NAV-03',`${name} acceso directo/recarga ${path}`,async()=>{const r=await page.goto(URL+path);assert.equal(r.status(),200);assert.ok((await page.locator('h1').innerText()).trim().length>0);await page.reload();assert.ok((await page.locator('h1').innerText()).trim().length>0);},{scope:'Producción, navegador escritorio'});
 for(const path of ['/jugador','/profesor','/perfil','/partida/999999'])await caseTest('NAV-04',`${name} anónimo ${path}`,async()=>{await page.goto(URL+path);await page.waitForURL(URL+'/');assert.equal(await page.locator('input[type=password]').count(),0);},{scope:'Producción, sin sesión'});
 await caseTest('NAV-06',`${name} 404 y retorno`,async()=>{await page.goto(URL+'/qa-path-does-not-exist');await page.getByRole('heading',{name:/404/}).waitFor();await page.getByRole('link',{name:'Volver al inicio'}).click();await page.waitForURL(URL+'/');},{scope:'Producción, lectura'});
 for(const width of [360,390,768,1440])for(const path of ['/login','/register','/forgot-password','/'])await caseTest('UI-01',`${name} público ${width}px ${path}`,async()=>{await page.setViewportSize({width,height:900});await page.goto(URL+path);await page.locator('main').first().waitFor();await page.screenshot({path:`${OUT}/screenshots/public-${name}-${width}-${path.replaceAll('/','')||'home'}.png`,fullPage:true});const dims=await page.evaluate(()=>({client:document.documentElement.clientWidth,scroll:document.documentElement.scrollWidth}));assert.ok(dims.scroll<=dims.client+2,`overflow horizontal ${dims.scroll-dims.client}px`);},{complete:false,scope:'Producción, motores desktop y viewport emulado',note:'No sustituye pantallas autenticadas, zoom 200% ni dispositivo físico.'});
 await caseTest('UI-03',`${name} etiquetas de formularios públicos`,async()=>{await page.goto(URL+'/login');const missing=await page.locator('input:not([type=hidden])').evaluateAll(nodes=>nodes.filter(n=>!n.labels?.length&&!n.getAttribute('aria-label')&&!n.getAttribute('aria-labelledby')).map(n=>n.id));assert.deepEqual(missing,[]);const unnamed=await page.getByRole('button').evaluateAll(nodes=>nodes.filter(n=>!n.textContent.trim()&&!n.getAttribute('aria-label')&&!n.getAttribute('title')).length);assert.equal(unnamed,0,'Botón sin nombre accesible');},{complete:false,scope:'Producción, inspección DOM real',note:'Inspección de nombres/labels; no es una sesión de lector de pantalla.'});
 await browser.close();
}
console.log('PUBLIC BROWSER acceptance finished');
