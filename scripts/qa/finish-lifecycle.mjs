import fs from 'node:fs';
import {assert,register,login,caseTest,delay,UI,OUT} from './common.mjs';
const {chromium}=await import('/Users/lucasnavarro/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs');
const browser=await chromium.launch({headless:true,executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'}),ctx=await browser.newContext({ignoreHTTPSErrors:true}),page=await ctx.newPage();page.setDefaultTimeout(12000);
await ctx.addInitScript(()=>{
 const intervals=new Set(),timeouts=new Set(),listeners=new Map();
 for(const [key,target]of [['window',window],['document',document]]){
  const add=target.addEventListener.bind(target),remove=target.removeEventListener.bind(target);
  target.addEventListener=(type,fn,options)=>{if(!options?.once){const k=key+':'+type+':'+Boolean(typeof options==='boolean'?options:options?.capture);if(!listeners.has(k))listeners.set(k,new Set());listeners.get(k).add(fn);}return add(type,fn,options);};
  target.removeEventListener=(type,fn,options)=>{listeners.get(key+':'+type+':'+Boolean(typeof options==='boolean'?options:options?.capture))?.delete(fn);return remove(type,fn,options);};
 }
 const si=window.setInterval.bind(window),ci=window.clearInterval.bind(window),st=window.setTimeout.bind(window),ct=window.clearTimeout.bind(window);
 window.setInterval=(fn,ms,...args)=>{const id=si(fn,ms,...args);intervals.add(id);return id;};window.clearInterval=id=>{intervals.delete(id);return ci(id);};
 window.setTimeout=(fn,ms,...args)=>{if(typeof fn!=='function')return st(fn,ms,...args);const id=st(()=>{timeouts.delete(id);fn(...args);},ms);timeouts.add(id);return id;};window.clearTimeout=id=>{timeouts.delete(id);return ct(id);};
 window.__qaLifecycle=()=>({intervals:intervals.size,timeouts:timeouts.size,listeners:Object.fromEntries([...listeners].map(([k,v])=>[k,v.size]).filter(([,v])=>v))});
});
const u=await login(await register());await page.goto(UI+'/');await page.evaluate(u=>{for(const[k,v]of Object.entries({jwt:u.token,rol:u.role,idUsuario:u.id,nick:u.nick,email:u.email}))localStorage.setItem(k,String(v));},u);
// SPA clicks preserve the JS realm; full navigations would conceal leaks by destroying it.
async function cycle(){await page.goto(UI+'/jugador');await page.getByRole('heading',{name:/Bienvenido/}).waitFor();}
await cycle();
async function spa(){await page.getByRole('button',{name:'Crear partida',exact:true}).click();await page.getByRole('heading',{name:'¡Configura tu partida!'}).waitFor();await page.getByRole('link',{name:'Volver',exact:true}).click();await page.getByRole('button',{name:'Buscar partida',exact:true}).click();await page.getByRole('link',{name:'Unirse a partida pública'}).click();await page.getByRole('heading',{name:'Partidas públicas'}).waitFor();await page.getByRole('link',{name:'Volver',exact:true}).click();await page.getByRole('button',{name:'Volver',exact:true}).click();await page.getByRole('button',{name:'Ver estadísticas',exact:true}).click();await page.getByRole('heading',{name:'Mis estadísticas'}).waitFor();await page.getByRole('button',{name:'Volver',exact:true}).click();}
const cdp=await ctx.newCDPSession(page);await cdp.send('HeapProfiler.enable');
async function sample(label){await delay(1000);await cdp.send('HeapProfiler.collectGarbage');const usage=await cdp.send('Runtime.getHeapUsage'),life=await page.evaluate(()=>window.__qaLifecycle());let chunks=[];const receive=event=>chunks.push(event.chunk);cdp.on('HeapProfiler.addHeapSnapshotChunk',receive);await cdp.send('HeapProfiler.takeHeapSnapshot',{});cdp.off('HeapProfiler.addHeapSnapshotChunk',receive);const heap=JSON.parse(chunks.join(''));chunks=[];const fields=heap.snapshot.meta.node_fields,width=fields.length,name=fields.indexOf('name');let detached=0;for(let i=0;i<heap.nodes.length;i+=width)if(/^Detached /.test(heap.strings[heap.nodes[i+name]]))detached++;return {label,usage,life,detachedNodes:detached,totalNodes:heap.nodes.length/width};}
for(let i=0;i<5;i++)await spa();const before=await sample('after-five-warmup-cycles'),samples=[before];
await caseTest('WS-09','veinte ciclos SPA con contadores reales de listeners y timers',async()=>{for(let i=0;i<20;i++){await spa();if(i%5===4)samples.push(await sample('cycle-'+(i+1)));}const after=samples.at(-1);fs.writeFileSync(OUT+'/browser-lifecycle.json',JSON.stringify(samples,null,2));assert.equal(after.life.intervals,before.life.intervals);for(const [key,count]of Object.entries(after.life.listeners))assert.ok(count<=(before.life.listeners[key]||0),`${key}: ${before.life.listeners[key]||0} -> ${count}`);assert.ok(after.life.timeouts<=before.life.timeouts+2);});
await caseTest('REN-03','perfil de heap tras GC y snapshots antes/después de navegación',async()=>{const after=samples.at(-1);assert.ok(after.usage.usedSize-before.usage.usedSize<5*1024*1024,`Crecimiento neto de heap: ${after.usage.usedSize-before.usage.usedSize}`);assert.ok(after.detachedNodes<=before.detachedNodes+20);return {before:before.usage.usedSize,after:after.usage.usedSize,detachedBefore:before.detachedNodes,detachedAfter:after.detachedNodes};},{scope:'QA Chromium: snapshots reales de heap procesados sin persistir cadenas/credenciales',note:'Perfil acotado; no prueba ausencia de toda fuga posible.'});
await ctx.close();await browser.close();console.log('FINISH LIFECYCLE completed');
