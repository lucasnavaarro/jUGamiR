import fs from 'node:fs';
import {execFileSync} from 'node:child_process';
import {assert,api,status,register,login,scalar,room,start,caseTest,compose,delay,UI,OUT} from './common.mjs';
const {chromium}=await import('/Users/lucasnavarro/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs');
await caseTest('OPS-02','proxy recupera backend con dirección diferente sin reiniciar frontend',async()=>{
 const user=await login(await register()),cat={id:Number(scalar('select id from categorias order by id limit 1'))},g=await room(user,cat,{modoEntrenamiento:true,maxJugadores:1,tiempoRespuesta:600});await start(g,user);
 const browser=await chromium.launch({headless:true,executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'}),ctx=await browser.newContext({ignoreHTTPSErrors:true}),page=await ctx.newPage();page.setDefaultTimeout(20000);let connects=0;
 page.on('websocket',ws=>ws.on('framereceived',event=>{if(String(event.payload).includes('CONNECTED'))connects++;}));
 async function healthy(port){for(let i=0;i<100;i++){try{if((await fetch(`http://127.0.0.1:${port}/actuator/health`,{signal:AbortSignal.timeout(1500)})).ok)return;}catch{}await delay(250);}throw Error('Backend QA no preparado: '+port);}
 try{
  await page.goto(UI+'/');await page.evaluate(u=>{for(const[k,v]of Object.entries({jwt:u.token,rol:u.role,idUsuario:u.id,nick:u.nick,email:u.email}))localStorage.setItem(k,String(v));},user);await page.goto(UI+`/partida/${g.idPartida}`);await page.locator('.juego__scoreboard').waitFor();
  compose('--profile','replacement','up','-d','--no-deps','replacementbackend');await healthy(18091);
  const ids=['backend','replacementbackend'].map(service=>compose('ps','-q',service).trim());
  const ips=ids.map(id=>execFileSync('docker',['inspect','--format','{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}',id],{encoding:'utf8'}).trim());assert.notEqual(ips[0],ips[1]);
  const initial=connects;compose('stop','backend');await delay(6500);
  const response=await fetch(`http://127.0.0.1:18091/api/juego/${g.idPartida}/girar`,{method:'POST',headers:{Authorization:'Bearer '+user.token}});assert.equal(response.status,200);const question=(await response.json()).pregunta;
  await page.getByText(question.enunciado,{exact:true}).waitFor({timeout:25000});assert.ok(connects>initial,'No se observó un nuevo CONNECTED tras cambiar el servidor');
  fs.writeFileSync(OUT+'/proxy-replacement.json',JSON.stringify({reconnected:true,connectionsBefore:initial,connectionsAfter:connects,frontendRestarted:false,backendAddressesDiffer:true,questionRecovered:question.id},null,2));
 } finally {compose('start','backend');await healthy(18081);compose('--profile','replacement','stop','replacementbackend');await ctx.close();await browser.close();}
});
console.log('FINISH PROXY REPLACEMENT completed');
