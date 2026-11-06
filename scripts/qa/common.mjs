import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import assert from 'node:assert/strict';

export { assert };
export const ROOT = path.resolve(import.meta.dirname, '../..');
export const OUT = process.env.QA_OUT ? path.resolve(process.env.QA_OUT) : path.join(ROOT, 'docs/ejecucion-completa-2026-11-04');
fs.mkdirSync(OUT, {recursive:true});
function loopbackEndpoint(value) {
  const endpoint = new URL(value);
  assert.ok(['http:', 'https:'].includes(endpoint.protocol) && ['localhost', '127.0.0.1', '[::1]'].includes(endpoint.hostname), 'QA may only target loopback endpoints');
  return value.replace(/\/$/, '');
}
export const BASE = loopbackEndpoint(process.env.QA_BASE || 'http://127.0.0.1:18081');
export const UI = loopbackEndpoint(process.env.QA_UI || 'https://localhost:18443');
export const PASSWORD = 'QA-acceptance@2026';
export const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
export const quote = value => "'" + String(value).replaceAll("'", "''") + "'";
export function sql(statement) {
  return execFileSync('docker', ['compose','-f',path.join(ROOT,'scripts/qa/compose.yaml'),'exec','-T','db',
    'psql','-X','-U','audit_test','-d','audit_test','-v','ON_ERROR_STOP=1','-qAt','-c',statement],
    {encoding:'utf8',timeout:30000,stdio:['pipe','pipe','pipe']}).trim();
}
export function rows(statement) { return JSON.parse(sql(/^(insert|update|delete)/i.test(statement)
  ? `WITH q AS (${statement}) SELECT COALESCE(json_agg(q),'[]') FROM q`
  : `SELECT COALESCE(json_agg(q),'[]') FROM (${statement}) q`)); }
export function scalar(statement) { return sql(statement); }
export function compose(...args) {
  return execFileSync('docker',['compose','-f',path.join(ROOT,'scripts/qa/compose.yaml'),...args],
    {encoding:'utf8',timeout:120000,stdio:['pipe','pipe','pipe']});
}
export async function api(method, url, body, user, options={}) {
  assert.ok(url.startsWith('/'), 'Only relative API paths are accepted');
  const headers = {...options.headers};
  if (user?.token) headers.Authorization = `Bearer ${user.token}`;
  if (user?.cookie) headers.Cookie = user.cookie;
  let payload;
  if (body instanceof FormData) payload=body;
  else if (body !== undefined) {headers['Content-Type']??='application/json';payload=typeof body==='string'?body:JSON.stringify(body);}
  const response=await fetch(BASE+url,{method,headers,body:payload,signal:AbortSignal.timeout(options.timeout||20000)});
  const text=await response.text();let data;try{data=JSON.parse(text);}catch{data=text;}
  return {status:response.status,data,headers:response.headers};
}
export function status(response, expected) {
  assert.ok((Array.isArray(expected)?expected:[expected]).includes(response.status),
    `HTTP ${response.status}, expected ${expected}; ${JSON.stringify(response.data).slice(0,300)}`);
  return response.data;
}
export function rejected(response) {assert.ok(response.status>=400&&response.status<500,`Expected controlled 4xx, got ${response.status}: ${JSON.stringify(response.data).slice(0,200)}`);}
let serial=0;
export async function register(role='jugador', extras={}) {
  const n=++serial;
  const body={nombre:'QA',apellidos:'Acceptance',email:`qa${Date.now()}_${process.pid}_${n}@${role==='profesor'?'ugr.es':'example.invalid'}`,
    password:PASSWORD,nick:`qa_${Date.now()}_${process.pid}_${n}`,departamento:'QA',...extras};
  status(await api('POST',`/api/auth/register/${role}`,body),202);
  body.email=body.email.trim().toLowerCase();
  body.nick=body.nick?.trim();
  return {email:body.email,password:body.password,nick:body.nick,role:role==='profesor'?'PROFESOR':'JUGADOR',
    id:Number(scalar(`select id_usuario from usuarios where email=${quote(body.email)}`))};
}
export async function login(user) {
  const challenge=status(await api('POST','/api/auth/login',{email:user.email,password:user.password}),202).challengeId;
  const code=scalar(`select codigo from codigos_2fa where challenge_id=${quote(challenge)}`);
  const verification=await api('POST','/api/auth/verify',{challengeId:challenge,codigo:code});
  const data=status(verification,200);
  return {...user,...data,id:data.idUsuario,challenge,code,token:data.token,cookie:verification.headers.get('set-cookie').split(';')[0]};
}
export async function resetFixture({questions=8,people=3,professors=1}={}) {
  assert.equal(scalar('select current_database()'),'audit_test');
  sql('TRUNCATE usuarios,categorias,security_rate_limits RESTART IDENTITY CASCADE');
  const users=[];for(let i=0;i<people;i++)users.push(await login(await register()));
  const teachers=[];for(let i=0;i<professors;i++)teachers.push(await login(await register('profesor')));
  const cats=rows("insert into categorias(nombre,color) values ('QA A','#123456'),('QA B','#654321') returning id,nombre");
  const subjects=cats.map((cat,i)=>Number(scalar(`insert into asignaturas(nombre,categoria_id) values ('QA Subject ${i}',${cat.id}) returning id`)));
  const questionIds=[];
  for(let c=0;c<cats.length;c++)for(let q=0;q<questions;q++) {
    const id=Number(scalar(`insert into preguntas(identificador,enunciado,comentario,estado,dificultad,asignatura_id,creada_por) values ('QA_${c}_${q}','QA question ${c} ${q}','SECRET_SOLUTION','PUBLICADA','${['FACIL','MEDIO','DIFICIL'][q%3]}',${subjects[c]},${teachers[0]?.id||users[0].id}) returning id`));
    sql(`insert into respuestas(pregunta_id,texto_respuesta,es_correcta,orden) values (${id},'Correct option',true,1),(${id},'Wrong option',false,2)`);
    questionIds.push(id);
  }
  return {users,teachers,cats,subjects,questionIds};
}
export async function room(user,cat,extras={}) {
  return status(await api('POST','/api/lobby/crear',{tipo:'PRIVADA',dificultades:['FACIL','MEDIO','DIFICIL'],tiempoRespuesta:30,
    maxJugadores:3,categoriaIds:[cat.id],aciertosParaQuesito:3,modoEntrenamiento:false,...extras},user),200);
}
export async function join(game,user) {return api('POST',`/api/lobby/unirse/privada/${game.codigoUnion}`,undefined,user);}
export async function start(game,user) {return api('POST',`/api/lobby/iniciar/${game.idPartida}`,undefined,user);}
export function current(game) {return Number(scalar(`select jugador_id from jugadores_partida where partida_id=${game.idPartida} and orden_turno=(select turno_actual from partidas where id=${game.idPartida})`));}
export async function spin(game,user) {
  const response=await api('POST',`/api/juego/${game.idPartida}/girar`,undefined,user);
  status(response,200);
  return rows(`select * from preguntas_partida where partida_id=${game.idPartida} order by orden_pregunta desc limit 1`)[0];
}
export function openWindow(question,seconds=2) {sql(`update preguntas_partida set emitida_en=CURRENT_TIMESTAMP-INTERVAL '${seconds} seconds',vence_en=CURRENT_TIMESTAMP+INTERVAL '30 seconds' where id=${question.id}`);}
export async function answer(game,user,question,correct=true,extras={}) {
  const id=Number(scalar(`select id from respuestas where pregunta_id=${question.pregunta_id} and es_correcta=${correct} limit 1`));
  return api('POST',`/api/juego/${game.idPartida}/responder`,{respuestaId:id,preguntaPartidaId:question.id,...extras},user);
}
export async function win(game,user) {const q=await spin(game,user);openWindow(q);return answer(game,user,q,true);}
export async function caseTest(id,variant,fn,{complete=true,scope='QA HTTP/PostgreSQL',note=''}={}) {
  if(process.env.QA_CASES&&!process.env.QA_CASES.split(',').includes(id))return;
  if(process.env.QA_SKIP_CASES?.split(',').includes(id))return;
  const begin=Date.now();let result='OK',detail=note;
  try {const observed=await fn();if(observed)detail=[note,typeof observed==='string'?observed:JSON.stringify(observed)].filter(Boolean).join(' ');}
  catch(error){result='FALLO';detail=String(error.message).slice(0,1600);process.exitCode=1;}
  const entry={id,variant,result,complete,scope,detail,durationMs:Date.now()-begin,at:new Date().toISOString()};
  fs.appendFileSync(path.join(OUT,'observaciones.ndjson'),JSON.stringify(entry)+'\n');
  console.log(`${id} ${result} ${variant}${result==='FALLO'?' — '+detail.replaceAll('\n',' ').slice(0,240):''}`);
  return entry;
}
export async function socket(user,{origin}={}) {
  // Native WebSocket performs real frames and broker subscriptions, without mocking STOMP.
  const ws=new WebSocket('ws://127.0.0.1:18081/ws');
  const frames=[];let buffer='';
  ws.addEventListener('message',event=>{buffer+=String(event.data);let end;while((end=buffer.indexOf('\0'))>=0){frames.push(buffer.slice(0,end).trim());buffer=buffer.slice(end+1);}});
  await Promise.race([new Promise((resolve,reject)=>{ws.addEventListener('open',resolve,{once:true});ws.addEventListener('error',()=>reject(new Error('WebSocket handshake rejected')),{once:true});}),delay(4000).then(()=>{throw Error('WebSocket handshake timeout');})]);
  ws.send(`CONNECT\naccept-version:1.2\nheart-beat:0,0\n${user?.token?'Authorization:Bearer '+user.token+'\n':''}\n\0`);
  for(let i=0;i<40&&!frames.some(f=>f.startsWith('CONNECTED')||f.startsWith('ERROR'))&&ws.readyState===1;i++)await delay(50);
  return {ws,frames,subscribe(destination){ws.send(`SUBSCRIBE\nid:qa${++serial}\ndestination:${destination}\n\n\0`);},
    events(){return frames.filter(f=>f.startsWith('MESSAGE')).map(f=>{try{return JSON.parse(f.slice(f.indexOf('\n\n')+2));}catch{return f;}});},
    close(){try{ws.close();}catch{}}};
}
