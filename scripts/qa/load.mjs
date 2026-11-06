import fs from 'node:fs';
import {createHmac} from 'node:crypto';
import {execFileSync} from 'node:child_process';
import {ROOT,OUT,assert,delay,caseTest} from './common.mjs';
const BASE='http://127.0.0.1:18082',SECRET='disposable-qa-key-not-for-production-64-characters-long-01234567890';
const duration=Number(process.env.QA_LOAD_MS||1800000);
assert.ok(duration>=1800000,'REN-03 requires at least thirty real minutes');
const command=(s)=>execFileSync('docker',['compose','-f',ROOT+'/scripts/qa/compose.yaml','exec','-T','db','psql','-X','-qAt','-U','audit_test','-d','audit_load','-v','ON_ERROR_STOP=1','-c',s],{encoding:'utf8',timeout:20000}).trim();
assert.equal(command('select current_database()'),'audit_load');
// A fresh CI database is migrated by the backend. Never seed it before startup.
let backendReady=false;
for(let i=0;i<120;i++){
 try{const response=await fetch(BASE+'/actuator/health',{signal:AbortSignal.timeout(2000)});if(response.ok&&(await response.json()).status==='UP'){backendReady=true;break;}}catch{}
 await delay(500);
}
assert.ok(backendReady,'Load backend must finish startup and migrations before fixtures');
command(`TRUNCATE usuarios,categorias,security_rate_limits RESTART IDENTITY CASCADE;
 INSERT INTO usuarios(nombre,apellidos,email,contraseña_hash) SELECT 'Load','QA','load'||n||'@example.invalid','!disabled-content-owner' FROM generate_series(1,50) n;
 INSERT INTO jugadores(id_usuario,nick) SELECT id_usuario,'load'||id_usuario FROM usuarios;
 INSERT INTO categorias(nombre,color) VALUES ('Load','#123456');
 INSERT INTO asignaturas(nombre,categoria_id) VALUES ('Load',1);
 INSERT INTO preguntas(identificador,enunciado,asignatura_id,creada_por,estado) SELECT 'LD_'||n,'Load question '||n,1,1,'PUBLICADA' FROM generate_series(1,5000) n;
 INSERT INTO respuestas(pregunta_id,texto_respuesta,es_correcta,orden) SELECT id,'Correct',true,1 FROM preguntas;
 INSERT INTO respuestas(pregunta_id,texto_respuesta,es_correcta,orden) SELECT id,'Wrong',false,2 FROM preguntas;
 INSERT INTO partidas(codigo_union,tipo,estado,creada_por,tiempo_respuesta,max_jugadores,aciertos_para_quesito,turno_actual,empezada_en) SELECT 'LD'||lpad(n::text,4,'0'),'PRIVADA','EN_CURSO',(n-1)*6+1,30,6,30,1,CURRENT_TIMESTAMP FROM generate_series(1,9) n;
 INSERT INTO jugadores_partida(partida_id,jugador_id,orden_union,orden_turno) SELECT ((id_usuario-1)/6)+1,id_usuario,((id_usuario-1)%6)+1,((id_usuario-1)%6)+1 FROM usuarios;
 INSERT INTO partidas_categorias SELECT id,1 FROM partidas;
 INSERT INTO partida_dificultades SELECT id,'MEDIO' FROM partidas;`);
command(`BEGIN;
CREATE TEMP TABLE qa_hist ON COMMIT DROP AS WITH g AS (INSERT INTO partidas(estado,creada_por,empezada_en,terminada_en) SELECT 'TERMINADA',1,now()-interval '2 days',now()-interval '1 day' FROM generate_series(1,1000) RETURNING id) SELECT id FROM g;
INSERT INTO jugadores_partida(partida_id,jugador_id,resultado,num_acertadas,tiempo_total) SELECT id,1,'VICTORIA',1,1000 FROM qa_hist;
INSERT INTO preguntas_partida(partida_id,pregunta_id,orden_pregunta,cerrada,jugador_turno_id) SELECT id,1,1,true,1 FROM qa_hist;
INSERT INTO respuestas_jugador(jugador_partida_id,pregunta_partida_id,respuesta_id,es_correcta,tiempo_ms) SELECT jp.id,pp.id,r.id,true,1000 FROM qa_hist h JOIN jugadores_partida jp ON jp.partida_id=h.id JOIN preguntas_partida pp ON pp.partida_id=h.id JOIN respuestas r ON r.pregunta_id=1 AND r.es_correcta;
COMMIT;`);
const b64=o=>Buffer.from(JSON.stringify(o)).toString('base64url');
const users=Array.from({length:50},(_,i)=>{const id=i+1,head=b64({alg:'HS256'}),payload=b64({sub:`load${id}@example.invalid`,rol:'JUGADOR',tokenVersion:1,iat:Math.floor(Date.now()/1000),exp:Math.floor(Date.now()/1000)+7200});return{id,game:Math.floor(i/6)+1,token:`${head}.${payload}.`+createHmac('sha256',SECRET).update(head+'.'+payload).digest('base64url')};});
command("INSERT INTO usuarios(nombre,apellidos,email,contraseña_hash) VALUES ('Load','Professor','load-professor@example.invalid','!disabled'); INSERT INTO profesores(id_usuario,departamento) SELECT id_usuario,'QA' FROM usuarios WHERE email='load-professor@example.invalid';");
const professorId=Number(command("select id_usuario from usuarios where email='load-professor@example.invalid'"));
const ph=b64({alg:'HS256'}),pp=b64({sub:'load-professor@example.invalid',rol:'PROFESOR',tokenVersion:1,iat:Math.floor(Date.now()/1000),exp:Math.floor(Date.now()/1000)+7200});
const professor={id:professorId,token:ph+'.'+pp+'.'+createHmac('sha256',SECRET).update(ph+'.'+pp).digest('base64url')};
const answers=new Map(JSON.parse(command("select json_agg(t) from (select pregunta_id,array_agg(id order by es_correcta desc) ids from respuestas group by pregunta_id) t")).map(q=>[Number(q.pregunta_id),q.ids]));
fs.rmSync(OUT+'/load-results.json',{force:true});
fs.writeFileSync(OUT+'/load-errors.json','[]');
const metrics={publicEvents:0,publicIsolationErrors:0,started:new Date().toISOString(),durationRequiredMs:duration,http:[],stomp:[],errors:[],samples:[],cycles:0,sockets:50};
const starts=new Map(),sockets=[],actions=new Map();
async function req(method,url,user,body){const t=performance.now();try{const r=await fetch(BASE+url,{method,headers:{Authorization:'Bearer '+user.token,'Content-Type':'application/json'},body:body?JSON.stringify(body):undefined,signal:AbortSignal.timeout(10000)});const data=await r.json().catch(()=>null);metrics.http.push({at:Date.now(),method,ms:performance.now()-t,status:r.status,path:url,game:user.game,players:user.game===9?2:6});if(r.status>=400){metrics.errors.push({status:r.status,path:url,body:data,at:new Date().toISOString()});fs.writeFileSync(OUT+'/load-errors.json',JSON.stringify(metrics.errors,null,2));}return{status:r.status,data};}catch(e){metrics.errors.push({path:url,error:e.message});return{status:0,data:null};}}
for(const u of users){const ws=new WebSocket(BASE.replace('http:','ws:')+'/ws');sockets.push(ws);let buffer='';ws.addEventListener('message',e=>{buffer+=String(e.data);let end;while((end=buffer.indexOf('\0'))>=0){const frame=buffer.slice(0,end);buffer=buffer.slice(end+1);if(frame.startsWith('CONNECTED'))ws.send(`SUBSCRIBE\nid:load${u.id}\ndestination:/topic/juego/${u.game}\n\n\0`);if(frame.trim().startsWith('MESSAGE')){try{const event=JSON.parse(frame.slice(frame.indexOf('\n\n')+2));if(event.evento==='PREGUNTA'){const start=starts.get(u.game);if(start)metrics.stomp.push(performance.now()-start);}}catch{}}}});await new Promise((resolve,reject)=>{ws.addEventListener('open',resolve,{once:true});ws.addEventListener('error',reject,{once:true});});ws.send(`CONNECT\naccept-version:1.2\nheart-beat:0,0\nAuthorization:Bearer ${u.token}\n\n\0`);}
const spectators=[];
for(let i=0;i<3;i++){const ws=new WebSocket(BASE.replace('http:','ws:')+'/ws');spectators.push(ws);let buffer='';ws.addEventListener('message',e=>{buffer+=String(e.data);let end;while((end=buffer.indexOf('\0'))>=0){const frame=buffer.slice(0,end).trim();buffer=buffer.slice(end+1);if(frame.startsWith('CONNECTED'))ws.send(`SUBSCRIBE\nid:public${i}\ndestination:/topic/partidas-publicas\n\n\0`);if(frame.startsWith('MESSAGE')){metrics.publicEvents++;try{const games=JSON.parse(frame.slice(frame.indexOf('\n\n')+2));if(games.some(g=>g.id<=9))metrics.publicIsolationErrors++;}catch{metrics.publicIsolationErrors++;}}}});await new Promise((resolve,reject)=>{ws.addEventListener('open',resolve,{once:true});ws.addEventListener('error',reject,{once:true});});ws.send(`CONNECT\naccept-version:1.2\nheart-beat:0,0\nAuthorization:Bearer ${users[0].token}\n\n\0`);}
const started=Date.now();let nextSample=0;
fs.writeFileSync(OUT+'/load-progress.json',JSON.stringify({started:metrics.started,status:'RUNNING'}));
while(Date.now()-started<duration){const elapsed=Date.now()-started;const workers=elapsed<30000?2:elapsed<60000?10:elapsed<90000?25:50;
 await Promise.all(users.slice(0,workers).map(u=>req('GET',`/api/juego/${u.game}/estado`,u)));
 for(const g of [...new Set(users.slice(0,workers).map(u=>u.game))]){const member=users.find(u=>u.game===g);const s=await req('GET',`/api/juego/${g}/estado`,member);if(s.status!==200||s.data.estado!=='EN_CURSO')continue;const actor=users.find(u=>u.id===s.data.jugadores.find(j=>j.ordenTurno===s.data.turnoActual)?.idJugador);if(!actor)continue;
  if(!s.data.preguntaActual){starts.set(g,performance.now());await req('POST',`/api/juego/${g}/girar`,actor);}
  else if(Date.now()>=Date.parse(s.data.preguntaActual.emitidaEn)+250 && Date.now()<Date.parse(s.data.preguntaActual.venceEn)-500) {
    const question=s.data.preguntaActual;
    const action=(actions.get(g)||0)+1;actions.set(g,action);
    if(action%5===0)await req('POST',`/api/juego/${g}/pasarse`,actor,{preguntaPartidaId:question.preguntaPartidaId});
    else await req('POST',`/api/juego/${g}/responder`,actor,{preguntaPartidaId:question.preguntaPartidaId,respuestaId:answers.get(question.pregunta.id)[action%5===1?0:1]});
  }
 }
 await req('GET','/api/profesor/preguntas/buscar?q=LD_&page=0&size=35',professor);
 await req('GET','/api/stats/jugador',users[0]);
 if(metrics.cycles%10===0){const r=await req('POST','/api/lobby/crear',users[0],{tipo:'PUBLICA',dificultades:['MEDIO'],tiempoRespuesta:30,maxJugadores:6,categoriaIds:[1],aciertosParaQuesito:30,modoEntrenamiento:false});if(r.status===200)await req('DELETE',`/api/lobby/abandonar/${r.data.idPartida}`,users[0]);}
 metrics.cycles++;
 // Force actual connection churn periodically; replacement gets a fresh CONNECT/SUBSCRIBE.
 if(metrics.cycles%12===0){const index=metrics.cycles%50;const old=sockets[index],u=users[index];old.close();const ws=new WebSocket(BASE.replace('http:','ws:')+'/ws');sockets[index]=ws;ws.addEventListener('open',()=>ws.send(`CONNECT\naccept-version:1.2\nheart-beat:0,0\nAuthorization:Bearer ${u.token}\n\n\0`));ws.addEventListener('message',e=>{if(String(e.data).includes('CONNECTED'))ws.send(`SUBSCRIBE\nid:reload${index}\ndestination:/topic/juego/${u.game}\n\n\0`);});}
 if(elapsed>=nextSample){let resources='';try{resources=execFileSync('docker',['stats','--no-stream','--format','{{json .}}','jugamir-fullqa-20261104-loadbackend-1','jugamir-fullqa-20261104-db-1'],{encoding:'utf8',timeout:10000});}catch{}metrics.samples.push({elapsedMs:elapsed,httpRequests:metrics.http.length,stompEvents:metrics.stomp.length,openSockets:sockets.filter(s=>s.readyState===1).length,resources});nextSample=elapsed+60000;fs.writeFileSync(OUT+'/load-progress.json',JSON.stringify({status:'RUNNING',elapsedMs:elapsed,requests:metrics.http.length,errors:metrics.errors.length}));console.log(`Load ${Math.round(elapsed/60000)}min requests=${metrics.http.length} errors=${metrics.errors.length}`);}
 await delay(4000);
}
metrics.finished=new Date().toISOString();metrics.actualDurationMs=Date.now()-started;sockets.forEach(s=>s.close());spectators.forEach(s=>s.close());
const p95=a=>a.sort((a,b)=>a-b)[Math.floor(a.length*.95)]||0;metrics.httpP95=p95(metrics.http.map(x=>x.ms));metrics.stompP95=p95(metrics.stomp);
fs.writeFileSync(OUT+'/load-results.json',JSON.stringify(metrics,null,2));fs.writeFileSync(OUT+'/load-progress.json',JSON.stringify({status:'FINISHED',durationMs:metrics.actualDurationMs,errors:metrics.errors.length}));
await caseTest('REN-01','salas de seis y escalones de consultas hasta cincuenta',async()=>{assert.equal(metrics.errors.length,0);assert.ok(metrics.httpP95<Number(process.env.QA_HTTP_P95||500),`HTTP p95 ${metrics.httpP95}`);assert.ok(metrics.stompP95<Number(process.env.QA_STOMP_P95||250),`STOMP p95 ${metrics.stompP95}`);return{httpP95:metrics.httpP95,stompP95:metrics.stompP95,requests:metrics.http.length};},{complete:true,note:'Dos y seis jugadores medidos por separado en HTTP; objetivos QA propuestos, no capacidad de producción.'});
await caseTest('REN-02','nueve salas aisladas con cincuenta sockets',async()=>{assert.equal(metrics.errors.length,0);assert.ok(metrics.stomp.length>100);assert.ok(metrics.publicEvents>100);assert.equal(metrics.publicIsolationErrors,0);return{cycles:metrics.cycles,stompEvents:metrics.stomp.length};},{complete:true,note:'Incluye tres espectadores públicos y altas/cancelaciones mientras se juega.'});
await caseTest('REN-03','treinta minutos reales de juego y reconexiones',async()=>{assert.ok(metrics.actualDurationMs>=1800000);assert.equal(metrics.errors.length,0);assert.ok(metrics.samples.every(s=>s.openSockets>=49));return{minutes:metrics.actualDurationMs/60000,samples:metrics.samples.length,resources:'load-results.json'};},{complete:false,note:'Recursos registrados; evaluación de tendencia y timers JS se realiza por separado.'});
