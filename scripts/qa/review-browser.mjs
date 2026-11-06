import {assert,api,status,register,login,rows,scalar,sql,quote,room,start,caseTest,delay,UI} from './common.mjs';
const {chromium}=await import('/Users/lucasnavarro/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs');
const user=await login(await register()),stamp=Date.now();
const color='#'+((stamp+process.pid)%16000000).toString(16).padStart(6,'0');
const category=rows(`insert into categorias(nombre,color) values ('Review_${stamp}','${color}') returning id,nombre`)[0];
const subject=Number(scalar(`insert into asignaturas(nombre,categoria_id) values ('Review_${stamp}',${category.id}) returning id`));
for(let i=0;i<4;i++){const q=Number(scalar(`insert into preguntas(identificador,enunciado,estado,asignatura_id,creada_por) values ('RV_${stamp}_${i}','Review question ${i}','PUBLICADA',${subject},${user.id}) returning id`));sql(`insert into respuestas(pregunta_id,texto_respuesta,es_correcta,orden) values (${q},'Correct',true,1),(${q},'Wrong',false,2)`);}
const game=await room(user,category,{modoEntrenamiento:true,maxJugadores:1,aciertosParaQuesito:30,tiempoRespuesta:60});await start(game,user);
const browser=await chromium.launch({headless:true,executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'}),ctx=await browser.newContext({ignoreHTTPSErrors:true}),page=await ctx.newPage();page.setDefaultTimeout(12000);
const errors=[];page.on('pageerror',e=>errors.push(e.message));
await page.goto(UI+'/');await page.evaluate(u=>{for(const[k,v]of Object.entries({jwt:u.token,rol:u.role,idUsuario:u.id,nick:u.nick,email:u.email}))localStorage.setItem(k,String(v));},user);await page.goto(UI+`/partida/${game.idPartida}`);
await caseTest('UI-06','corte al girar permite reintentar sin bloquear la ruleta',async()=>{
 await page.getByRole('button',{name:'Girar Ruleta'}).waitFor();await page.route('**/api/juego/*/girar',r=>r.abort());await page.getByRole('button',{name:'Girar Ruleta'}).click();await page.getByText(/No se pudo conectar/).waitFor();await page.getByRole('button',{name:'Girar Ruleta'}).waitFor();await page.unroute('**/api/juego/*/girar');await page.getByRole('button',{name:'Girar Ruleta'}).click();await page.locator('.respuesta-card').first().waitFor();
});
await caseTest('UI-06','corte al responder no bloquea las opciones y permite respuesta real',async()=>{
 await page.route('**/api/juego/*/responder',r=>r.abort());await page.locator('.respuesta-card').filter({hasText:'Wrong'}).click();await page.getByText(/No se pudo confirmar la respuesta/).waitFor();await page.locator('.respuesta-card').first().waitFor({state:'visible'});await page.unroute('**/api/juego/*/responder');await page.locator('.respuesta-card').filter({hasText:'Correct'}).click();await page.getByRole('status').filter({hasText:'Respuesta correcta'}).waitFor();
});
await caseTest('UI-06','abandono rechazado mantiene la partida hasta confirmación real',async()=>{
 page.on('dialog',dialog=>dialog.accept());await page.route('**/api/lobby/abandonar/*',r=>r.fulfill({status:503,contentType:'application/json',body:'{"error":"QA unavailable"}'}));await page.getByRole('button',{name:'Abandonar partida'}).click();await page.getByText(/No se pudo confirmar el abandono/).waitFor();assert.ok(page.url().endsWith('/partida/'+game.idPartida));assert.equal(scalar(`select resultado from jugadores_partida where partida_id=${game.idPartida} and jugador_id=${user.id}`),'PENDIENTE');await page.unroute('**/api/lobby/abandonar/*');await page.getByRole('button',{name:'Abandonar partida'}).click();await page.waitForURL(UI+'/jugador');assert.equal(scalar(`select resultado from jugadores_partida where partida_id=${game.idPartida} and jugador_id=${user.id}`),'ABANDONADA');assert.deepEqual(errors,[]);
});
await caseTest('UI-06','respuesta perdida después de commit se reconcilia sin duplicar jugada',async()=>{
 const g=await room(user,category,{modoEntrenamiento:true,maxJugadores:1,aciertosParaQuesito:30,tiempoRespuesta:60});await start(g,user);await page.goto(UI+`/partida/${g.idPartida}`);
 await page.route('**/api/juego/*/girar',async route=>{const response=await route.fetch();assert.equal(response.status(),200);await route.abort();});await page.getByRole('button',{name:'Girar Ruleta'}).click();await page.locator('.respuesta-card').first().waitFor();await page.unroute('**/api/juego/*/girar');assert.equal(scalar(`select count(*) from preguntas_partida where partida_id=${g.idPartida}`),'1');
 await page.route('**/api/juego/*/responder',async route=>{const response=await route.fetch();assert.equal(response.status(),200);await route.abort();});await page.locator('.respuesta-card').filter({hasText:'Correct'}).click();await page.getByRole('button',{name:'Girar Ruleta'}).waitFor();await page.unroute('**/api/juego/*/responder');assert.equal(scalar(`select count(*) from respuestas_jugador r join jugadores_partida j on j.id=r.jugador_partida_id where j.partida_id=${g.idPartida}`),'1');assert.deepEqual(errors,[]);
});
await ctx.close();await browser.close();console.log('REVIEW BROWSER completed');
