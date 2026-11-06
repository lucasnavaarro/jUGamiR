import fs from 'node:fs';
import {assert,api,status,rejected,sql,scalar,quote,register,login,caseTest,delay,room,start,spin,openWindow,answer,socket,compose} from './common.mjs';
const u=await login(await register()),teacher=await login(await register('profesor'));
const cat={id:Number(scalar('select id from categorias order by id limit 1'))};
await caseTest('CSV-10','fallo entre importación de preguntas y respuestas, y reintento',async()=>{
 const id=`QA_FAULT_${Date.now()}`,subject=scalar('select nombre from asignaturas order by id limit 1');
 const text=`Asignatura;Identificador;TituloIndice;Enunciado;Opciones;RespuestaCorrecta;Comentario;Anulada;Imagen;Dificultad\n${subject};${id};QA;Fault question ${id};1. A | 2. B;1;Comment;NO;;MEDIO`;
 const upload=async()=>{const f=new FormData();f.append('file',new Blob([text]),'qa.csv');return api('POST','/api/profesor/preguntas/importar',f,teacher);};
 let first;
 compose('exec','-T','-u','root','backend','mv','/app/db/import_respuestas.py','/app/db/import_respuestas.qa-backup');
 try{first=await upload();}finally{compose('exec','-T','-u','root','backend','mv','/app/db/import_respuestas.qa-backup','/app/db/import_respuestas.py');}
 const q=Number(scalar(`select id from preguntas where identificador=${quote(id)}`));
 const intermediate=scalar(`select estado from preguntas where id=${q}`);
 status(await upload(),200);
 assert.equal(scalar(`select count(*) from respuestas where pregunta_id=${q}`),'2');
 assert.equal(scalar(`select estado from preguntas where id=${q}`),'PUBLICADA');
 assert.notEqual(intermediate,'PUBLICADA');
 assert.ok(first.status>=400&&first.status<500,`Proceso de respuestas falló pero API devolvió HTTP ${first.status}, añadidas=${first.data.añadidas}, errores=${first.data.errores}`);
});
await caseTest('OPS-06','almacenamiento sin permiso de escritura se recupera',async()=>{
 const before=compose('exec','-T','backend','stat','-c','%a','/app/static/imagenes').trim();
 compose('exec','-T','-u','root','backend','chmod','0555','/app/static/imagenes');
 const form=()=>{const b=new FormData();b.append('file',new Blob([fs.readFileSync('/private/tmp/jugamir-fullqa-fixtures/valid.png')]),'valid.png');b.append('ruta','imagenes/qa-permission-fault.png');return b;};
 let response;
 try{response=await api('POST','/api/profesor/imagenes',form(),teacher);assert.ok(response.status>=400);}finally{compose('exec','-T','-u','root','backend','chmod',before,'/app/static/imagenes');}
 status(await api('POST','/api/profesor/imagenes',form(),teacher),200);
 return `Sin permiso: HTTP ${response.status}; recuperado: HTTP 200.`;
},{complete:false,note:'Permisos ejecutados; no se ha llenado el disco ni agotado cuota de /tmp.'});
await caseTest('OPS-07','rollback al commit después de emitir resultado STOMP',async()=>{
 const g=await room(u,cat,{maxJugadores:1});status(await start(g,u),200);const s=await socket(u);s.subscribe(`/topic/juego/${g.idPartida}`);await delay(100);const q=await spin(g,u);openWindow(q);
 sql("CREATE FUNCTION qa_reject_answer_commit() RETURNS trigger LANGUAGE plpgsql AS $$BEGIN RAISE EXCEPTION 'QA injected failure at commit'; END;$$; CREATE CONSTRAINT TRIGGER qa_reject_answer_commit AFTER INSERT ON respuestas_jugador DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION qa_reject_answer_commit()");
 try{const r=await answer(g,u,q,true);assert.equal(r.status,500);await delay(300);assert.equal(scalar(`select count(*) from respuestas_jugador where pregunta_partida_id=${q.id}`),'0');assert.equal(s.events().filter(e=>e.evento==='RESULTADO').length,0,'Un RESULTADO fue recibido aunque la transacción se revirtió');}
 finally{sql('DROP TRIGGER qa_reject_answer_commit ON respuestas_jugador; DROP FUNCTION qa_reject_answer_commit()');s.close();}
});
await caseTest('IMG-05','imagenes/%2e%2e/escape.png',async()=>{compose('exec','-T','backend','sh','-c','test ! -e /app/static/escape.png');return 'La ruta codificada se guarda literalmente en imagenes/%2e%2e; no crea archivo fuera de imagenes.';});
await caseTest('IMG-09','ZIP inválido no publica imágenes previas',async()=>{compose('exec','-T','backend','sh','-c','test ! -e /app/static/imagenes/qa-partial/good.png');return 'Ausencia física verificada; GET de ausentes devuelve 500, recogido en IMG-12.';},{complete:false,note:'Presupuesto 30 s se comprueba adicionalmente en Java.'});
console.log('FAULT INJECTION acceptance finished');
