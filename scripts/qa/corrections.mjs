// Focused verification of the November 5 fixes. Writes to QA_OUT, never to production.
import fs from 'node:fs';
import {assert,api,status,rejected,sql,scalar,rows,quote,register,login,resetFixture,caseTest,
  room,join,start,current,spin,openWindow,answer,socket,delay,compose,UI,OUT,PASSWORD} from './common.mjs';
const f=await resetFixture({people:8,professors:2,questions:12});
const [host,guest,third,...others]=f.users, [professor,professor2]=f.teachers;
const cat=f.cats[0];
const payload={tipo:'PRIVADA',dificultades:['MEDIO'],tiempoRespuesta:30,maxJugadores:3,
  categoriaIds:[cat.id],aciertosParaQuesito:3,modoEntrenamiento:false};
const newRoom=extras=>room(host,cat,extras);
async function ready(extras={}) {const g=await newRoom(extras);await join(g,guest);status(await start(g,host),200);return g;}
const turn=g=>f.users.find(u=>u.id===current(g));
async function editPayload(id) {const q=rows(`select * from preguntas where id=${id}`)[0];return {
  identificador:q.identificador,enunciado:q.enunciado,dificultad:q.dificultad,asignaturaId:q.asignatura_id,
  anulada:false,respuestas:rows(`select id,texto_respuesta as texto,es_correcta as "esCorrecta",orden from respuestas where pregunta_id=${id} order by orden`)};}

await caseTest('REG-09','email/nick normalizados y login con espacios/mayúsculas',async()=>{
  const stamp=Date.now(), email=`norm${stamp}@example.invalid`, nick=`Mixed_${stamp}`;
  const u=await register('jugador',{email:` ${email.toUpperCase()} `,nick:` ${nick} `});
  assert.equal(scalar(`select email from usuarios where id_usuario=${u.id}`),email);
  assert.equal(scalar(`select nick from jugadores where id_usuario=${u.id}`),nick);
  const logged=await login({...u,email:` ${email.toUpperCase()} `});assert.ok(logged.token);
  rejected(await api('POST','/api/auth/register/jugador',{nombre:'QA',apellidos:'QA',email:email.toUpperCase(),password:PASSWORD,nick:'different'}));
  rejected(await api('POST','/api/auth/register/jugador',{nombre:'QA',apellidos:'QA',email:`other${stamp}@example.invalid`,password:PASSWORD,nick:nick.toLowerCase()}));
});
for(const key of ['email','nick'])await caseTest('REG-10',`registro concurrente ${key}`,async()=>{
  const stamp=Date.now(), a={nombre:'QA',apellidos:'QA',email:`racea${stamp}@example.invalid`,nick:`RaceA_${stamp}`,password:PASSWORD};
  const b={...a,email:`raceb${stamp}@example.invalid`,nick:`RaceB_${stamp}`,[key]:a[key]};
  const rr=await Promise.all([api('POST','/api/auth/register/jugador',a),api('POST','/api/auth/register/jugador',b)]);
  assert.equal(rr.filter(r=>r.status===202).length,1);rejected(rr.find(r=>r.status!==202));
});
await caseTest('PER-03','validación de perfil no produce 500',async()=>{
  for(const change of [{nombre:'x'.repeat(121)},{apellidos:'x'.repeat(121)},{nick:null},{nick:guest.nick},{email:guest.email}])
    rejected(await api('PUT','/api/usuario/perfil',{nombre:'QA',apellidos:'QA',email:host.email,nick:host.nick,...change},host));
});
await caseTest('PER-10','historial rechaza borrado y cuenta vacía se elimina',async()=>{
  await newRoom();rejected(await api('DELETE','/api/usuario/perfil',undefined,host));
  assert.equal(scalar(`select count(*) from usuarios where id_usuario=${host.id}`),'1');
  const empty=await login(await register());status(await api('DELETE','/api/usuario/perfil',undefined,empty),204);
  assert.equal(scalar(`select count(*) from usuarios where id_usuario=${empty.id}`),'0');
});
for(const id of ['CRE-06','CRE-08'])await caseTest(id,'rechazo de límites, nulos y enums inválidos',async()=>{
  const variants=id==='CRE-06'?[{maxJugadores:0},{maxJugadores:7},{maxJugadores:null},{aciertosParaQuesito:0},{aciertosParaQuesito:null}]:[{tipo:'WRONG'},{dificultades:['WRONG']},{tiempoRespuesta:'bad'},{categoriaIds:null}];
  for(const value of variants)rejected(await api('POST','/api/lobby/crear',{...payload,...value},host));
});
await caseTest('CRE-07','tiempos API 4/5/29/30/300/301/600/601',async()=>{
  for(const value of [4,5,29,30,300,301,600,601]){const r=await api('POST','/api/lobby/crear',{...payload,tiempoRespuesta:value},host);if(value<5||value>600)rejected(r);else status(r,200);}
});
await caseTest('CRE-10','competitiva necesita dos participantes; entrenamiento individual inicia',async()=>{
  const g=await newRoom({maxJugadores:1});rejected(await start(g,host));
  const training=await newRoom({maxJugadores:1,modoEntrenamiento:true});status(await start(training,host),200);
});
await caseTest('CRE-11','listado público contiene nick y omite email',async()=>{
  const g=await newRoom({tipo:'PUBLICA'});const list=status(await api('GET','/api/lobby/publicas',undefined,host),200);
  const record=list.find(r=>r.id===g.idPartida);assert.equal(record.anfitrion,host.nick);assert.ok(!JSON.stringify(record).includes(host.email));assert.ok(!('emailCreador'in record));
});
await caseTest('ENT-10','entrenamiento siempre privado e individual',async()=>{
  for(const change of [{tipo:'PUBLICA',maxJugadores:1},{tipo:'PRIVADA',maxJugadores:2}])rejected(await api('POST','/api/lobby/crear',{...payload,modoEntrenamiento:true,...change},host));
});
await caseTest('LOB-02','código vacío, espacios, inválido y desconocido',async()=>{
  for(const code of ['', 'abc','123','999999','%20%20'])rejected(await api('POST',`/api/lobby/unirse/privada/${code}`,undefined,guest));
});
await caseTest('LOB-06','dos jugadores compiten por última plaza',async()=>{
  for(let i=0;i<5;i++){const g=await newRoom({maxJugadores:2});const rr=await Promise.all([join(g,guest),join(g,third)]);assert.equal(rr.filter(r=>r.status===200).length,1);rejected(rr.find(r=>r.status!==200));assert.equal(scalar(`select count(*) from jugadores_partida where partida_id=${g.idPartida} and resultado='PENDIENTE'`),'2');}
});
await caseTest('LOB-08','inicio y unión concurrentes no dejan turnos nulos',async()=>{
  for(let i=0;i<5;i++){const g=await newRoom();status(await join(g,guest),200);const [j,s]=await Promise.all([join(g,third),start(g,host)]);status(s,200);if(j.status!==200)rejected(j);assert.equal(scalar(`select count(*) from jugadores_partida where partida_id=${g.idPartida} and resultado='PENDIENTE' and orden_turno is null`),'0');}
});
for(const id of ['LOB-12','LOB-15','WS-05'])await caseTest(id,'expulsión con socket, sustitución e inicio',async()=>{
  const g=await newRoom({maxJugadores:2});status(await join(g,guest),200);const s=await socket(guest);s.subscribe(`/topic/lobby/${g.idPartida}`);await delay(100);
  try{status(await api('DELETE',`/api/lobby/expulsar/${g.idPartida}/${guest.id}`,undefined,host),200);rejected(await join(g,guest));status(await join(g,third),200);status(await start(g,host),200);await delay(200);assert.equal(s.events().length,0);assert.equal(scalar(`select count(*) from jugadores_partida where partida_id=${g.idPartida} and resultado='EXPULSADO' and orden_turno is not null`),'0');}finally{s.close();}
});
await caseTest('PRO-04','asignatura referenciada se rechaza sin 500',async()=>rejected(await api('DELETE',`/api/profesor/asignaturas/${f.subjects[0]}`,undefined,professor)));
await caseTest('PRO-07','paginación acotada',async()=>{for(const size of [0,-1,1000000])rejected(await api('GET',`/api/profesor/preguntas/buscar?q=QA&page=0&size=${size}`,undefined,professor));});
await caseTest('PRO-11','opciones sin correcta, dos correctas o vacías no se guardan',async()=>{
  const id=f.questionIds[0], base=await editPayload(id);
  for(const options of [base.respuestas.map(r=>({...r,esCorrecta:false})),base.respuestas.map(r=>({...r,esCorrecta:true})),base.respuestas.map(r=>({...r,texto:''}))])rejected(await api('PUT',`/api/profesor/preguntas/${id}`,{...base,respuestas:options},professor));
});
await caseTest('PRO-13','profesores comparten gestión y jugador no puede editar',async()=>{
  const id=f.questionIds[1], p=await editPayload(id);status(await api('PUT',`/api/profesor/preguntas/${id}`,{...p,enunciado:p.enunciado+' editado'},professor2),200);status(await api('PUT',`/api/profesor/preguntas/${id}`,p,host),403);
});
for(const id of ['PRO-14','PRO-15'])await caseTest(id,'pregunta usada conserva solución e historial al editar/retirar',async()=>{
  const g=await ready(),u=turn(g),q=await spin(g,u);openWindow(q);const original=await editPayload(q.pregunta_id), countBefore=scalar('select count(*) from respuestas_jugador');
  rejected(await api('PUT',`/api/profesor/preguntas/${q.pregunta_id}`,{...original,respuestas:original.respuestas.map(r=>({...r,esCorrecta:!r.esCorrecta}))},professor));
  status(await api('DELETE',`/api/profesor/preguntas/${q.pregunta_id}`,undefined,professor),204);
  status(await answer(g,u,q,true),200);assert.equal(Number(scalar('select count(*) from respuestas_jugador')),Number(countBefore)+1);
  assert.equal(scalar(`select es_correcta from respuestas_jugador where pregunta_partida_id=${q.id}`),'t');
});
for(const id of ['EST-05','EST-06'])await caseTest(id,'estadísticas coherentes al terminar con abandono',async()=>{
  const g=await ready(),u=turn(g),before=status(await api('GET','/api/stats/jugador',undefined,u),200);
  const q=await spin(g,u);openWindow(q);status(await answer(g,u,q,true),200);
  const during=status(await api('GET','/api/stats/jugador',undefined,u),200);assert.equal(during.partidasJugadas,before.partidasJugadas);assert.equal(during.totalRespondidas,before.totalRespondidas);
  const quitter=u.id===host.id?guest:host;status(await api('DELETE',`/api/lobby/abandonar/${g.idPartida}`,undefined,quitter),200);
  const after=status(await api('GET','/api/stats/jugador',undefined,u),200);assert.equal(after.partidasJugadas,before.partidasJugadas+1);
});
for(const id of ['API-06','API-07','API-08'])await caseTest(id,'errores HTTP controlados',async()=>{
  if(id==='API-06')for(const value of ['-1','0','9223372036854775808'])rejected(await api('GET',`/api/lobby/${value}`,undefined,host));
  if(id==='API-07'){status(await api('PATCH','/api/lobby/crear',payload,host),405);const r=await fetch('http://127.0.0.1:18081/api/lobby/crear',{method:'POST',headers:{Authorization:`Bearer ${host.token}`,'Content-Type':'text/plain'},body:'invalid'});assert.equal(r.status,415);}
  if(id==='API-08'){const r=await fetch('http://127.0.0.1:18081/api/lobby/crear',{method:'POST',headers:{Authorization:`Bearer ${host.token}`,'Content-Type':'application/json'},body:'{invalid'});assert.equal(r.status,400);}
});
await caseTest('OPS-07','fallo real al commit no emite RESULTADO',async()=>{
  const g=await ready(),u=turn(g),s=await socket(u);s.subscribe(`/topic/juego/${g.idPartida}`);await delay(100);const q=await spin(g,u);openWindow(q);
  sql("CREATE FUNCTION qa_reject_fix_commit() RETURNS trigger LANGUAGE plpgsql AS $$BEGIN RAISE EXCEPTION 'QA injected failure'; END;$$; CREATE CONSTRAINT TRIGGER qa_reject_fix_commit AFTER INSERT ON respuestas_jugador DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION qa_reject_fix_commit()");
  try{const r=await answer(g,u,q,true);assert.ok(r.status>=500);await delay(300);assert.equal(scalar(`select count(*) from respuestas_jugador where pregunta_partida_id=${q.id}`),'0');assert.equal(s.events().filter(e=>e.evento==='RESULTADO').length,0);}finally{sql('DROP TRIGGER qa_reject_fix_commit ON respuestas_jugador; DROP FUNCTION qa_reject_fix_commit()');s.close();}
});
await caseTest('REN-06','carreras de última plaza e inicio comprobadas cinco veces',async()=>{
  assert.equal(scalar("select count(*) from jugadores_partida jp join partidas p on p.id=jp.partida_id where p.estado='EN_CURSO' and jp.resultado='PENDIENTE' and jp.orden_turno is null"),'0');
});
console.log('CORRECTIONS HTTP/STOMP finished');
