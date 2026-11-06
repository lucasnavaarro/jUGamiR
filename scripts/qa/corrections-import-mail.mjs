import fs from 'node:fs';
import {execFileSync} from 'node:child_process';
import {assert,api,status,register,login,scalar,sql,quote,caseTest,compose,OUT,PASSWORD,delay} from './common.mjs';
const teacher=await login(await register('profesor')),user=await login(await register());
const subject=scalar('select nombre from asignaturas order by id limit 1');
const header='Asignatura;Identificador;TituloIndice;Enunciado;Opciones;RespuestaCorrecta;Comentario;Anulada;Imagen;Dificultad';
const csv=(id,question)=>header+`\n${subject};${id};QA;${question};1. A | 2. B;1;Comment;NO;;MEDIO`;
const upload=async text=>{const form=new FormData();form.append('file',new Blob([text]),'qa.csv');return api('POST','/api/profesor/preguntas/importar',form,teacher);};
await caseTest('CSV-05','identificador omitido se genera y el reintento no duplica',async()=>{
  const text=csv('',`Generated identifier ${Date.now()}`),first=status(await upload(text),200),second=status(await upload(text),200);
  assert.equal(first.añadidas,1);assert.ok(first.detalleErrores.some(s=>s.includes('Identificadores generados: 1')));assert.equal(second.añadidas,0);assert.equal(second.yaExistentes,1);
});
await caseTest('CSV-10','fallo al insertar opciones revierte la fila completa',async()=>{
  const id=`ATOMIC_${Date.now()}`,text=csv(id,'Atomic row');
  sql("CREATE FUNCTION qa_csv_failure() RETURNS trigger LANGUAGE plpgsql AS $$BEGIN RAISE EXCEPTION 'QA option failure'; END;$$; CREATE TRIGGER qa_csv_failure BEFORE INSERT ON respuestas FOR EACH ROW EXECUTE FUNCTION qa_csv_failure()");
  try{const result=status(await upload(text),200);assert.equal(result.añadidas,0);assert.equal(result.errores,1);assert.equal(scalar(`select count(*) from preguntas where identificador=${quote(id)}`),'0');}
  finally{sql('DROP TRIGGER qa_csv_failure ON respuestas; DROP FUNCTION qa_csv_failure()');}
  const retry=status(await upload(text),200);assert.equal(retry.añadidas,1);assert.equal(scalar(`select count(*) from respuestas r join preguntas p on p.id=r.pregunta_id where p.identificador=${quote(id)}`),'2');
});
await caseTest('CSV-10','importador ausente rechaza la operación sin altas parciales',async()=>{
  const id=`MISSING_${Date.now()}`,text=csv(id,'Missing importer');
  compose('exec','-T','-u','root','backend','mv','/app/db/import_csv.py','/app/db/import_csv.qa-backup');
  try{status(await upload(text),400);assert.equal(scalar(`select count(*) from preguntas where identificador=${quote(id)}`),'0');}
  finally{compose('exec','-T','-u','root','backend','mv','/app/db/import_csv.qa-backup','/app/db/import_csv.py');}
  assert.equal(status(await upload(text),200).añadidas,1);
});
await caseTest('AUT-12','SMTP caído responde 503 en menos de doce segundos y no emite sesión',async()=>{
  const begin=Date.now();compose('stop','mail');try{const result=await api('POST','/api/auth/login',{email:user.email,password:PASSWORD});assert.equal(result.status,503);assert.ok(Date.now()-begin<12000);assert.equal(scalar(`select count(*) from refresh_tokens where usuario_id=${user.id}`),'1');assert.ok(!result.data.token);}
  finally{compose('start','mail');}
});
await caseTest('IMG-02','EXIF 6 rotado y canal alfa conservado en HTTP',async()=>{
  for(const name of ['oriented.jpg','alpha.png'])fs.writeFileSync(OUT+'/'+name,Buffer.from(await(await fetch('http://127.0.0.1:18081/imagenes/qa/'+name)).arrayBuffer()));
  const result=execFileSync('/Users/lucasnavarro/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3',['-c',"from PIL import Image;import sys,json; a=Image.open(sys.argv[1]); b=Image.open(sys.argv[2]);print(json.dumps({'size':a.size,'exif':dict(a.getexif()),'alpha':b.getpixel((0,0))[3]}))",OUT+'/oriented.jpg',OUT+'/alpha.png'],{encoding:'utf8'});
  const image=JSON.parse(result);assert.deepEqual(image.size,[24,32]);assert.deepEqual(image.exif,{});assert.equal(image.alpha,100);fs.writeFileSync(OUT+'/image-metadata-check.json',result);
});
await caseTest('AUT-12','auth/me no consume límite de login; login conserva protección por IP',async()=>{
  compose('restart','frontend');await delay(500);
  for(let i=0;i<30;i++){const r=await fetch('https://localhost:18443/api/auth/me',{headers:{Authorization:`Bearer ${user.token}`}});assert.equal(r.status,200);}
  const statuses=[];for(let i=0;i<25;i++){const r=await fetch('https://localhost:18443/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:`unknown${Date.now()}_${i}@example.invalid`,password:PASSWORD})});statuses.push(r.status);}
  assert.ok(statuses.includes(429));assert.ok(statuses.includes(400));return {counts:statuses.reduce((m,s)=>(m[s]=(m[s]||0)+1,m),{})};
});
console.log('CORRECTIONS IMPORT/MAIL finished');
