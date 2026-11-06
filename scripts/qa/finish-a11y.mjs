import fs from 'node:fs';
import {assert,register,login,caseTest,UI,OUT} from './common.mjs';
const {chromium}=await import('/Users/lucasnavarro/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs');
const browser=await chromium.launch({headless:true,executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'});
const reports=[];
for(const role of ['anonymous','jugador','profesor']){
 const ctx=await browser.newContext({ignoreHTTPSErrors:true,viewport:{width:1440,height:900}}),page=await ctx.newPage();page.setDefaultTimeout(10000);
 if(role!=='anonymous'){const u=await login(await register(role));await page.goto(UI+'/');await page.evaluate(u=>{for(const[k,v]of Object.entries({jwt:u.token,rol:u.role,idUsuario:u.id,nick:u.nick,email:u.email}))localStorage.setItem(k,String(v));},u);}
 const paths=role==='anonymous'?['/','/login','/register','/forgot-password','/reset-password?token=qa','/password-changed','/register-confirmation']:role==='profesor'?['/profesor','/perfil','/profesor/asignaturas','/profesor/preguntas/editar','/profesor/preguntas/importar']:['/jugador','/perfil','/crear/partida','/unirse/partida/privada','/unirse/partida/publica','/jugador/estadisticas','/jugador/entrenamiento'];
 for(const path of paths){await page.goto(UI+path);await page.locator('main').waitFor();await page.waitForLoadState('networkidle');await page.evaluate(()=>document.fonts.ready);await page.waitForFunction(()=>document.getAnimations().filter(a=>a.effect?.getComputedTiming().iterations!==Infinity).every(a=>a.playState==='finished')); await page.addScriptTag({path:'/private/tmp/jugamir-a11y/node_modules/axe-core/axe.min.js'});
 await caseTest('UI-07',`WCAG AA ${role} ${path}`,async()=>{const r=await page.evaluate(()=>axe.run(document,{runOnly:{type:'tag',values:['wcag2a','wcag2aa','wcag21aa']}}));const report={role,path,violations:r.violations.map(v=>({id:v.id,impact:v.impact,help:v.help,nodes:v.nodes.map(n=>({target:n.target,failureSummary:n.failureSummary}))})),incomplete:r.incomplete.map(v=>({id:v.id,nodes:v.nodes.length}))};reports.push(report);fs.writeFileSync(OUT+'/accessibility.json',JSON.stringify(reports,null,2));assert.deepEqual(report.violations.map(v=>v.id),[],JSON.stringify(report.violations).slice(0,1300));},{scope:'QA Chrome axe-core WCAG A/AA, no sustituye lector físico'});
 await caseTest('UI-01',`reflujo 360px ${role} ${path}`,async()=>{await page.setViewportSize({width:360,height:800});assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),JSON.stringify(await page.evaluate(()=>({width:innerWidth,scroll:document.documentElement.scrollWidth}))));await page.setViewportSize({width:1440,height:900});});
 }
 await ctx.close();
}
await browser.close();console.log('FINISH A11Y completed');
