import fs from 'node:fs';
import {assert,api,status,register,login,caseTest} from './common.mjs';
const teacher=await login(await register('profesor'));
await caseTest('IMG-08','ZIP válido de 192 MiB bajo presupuesto temporal de producción',async()=>{
 const path=process.env.QA_LARGE_ZIP||'/private/tmp/jugamir-fullqa-fixtures/valid-large-under-200m.zip';
 const size=fs.statSync(path).size;assert.ok(size<200*1024*1024);
 const body=new FormData();body.append('file',new Blob([fs.readFileSync(path)]),'valid-large-under-200m.zip');
 const started=Date.now();const response=await api('POST','/api/profesor/imagenes/zip',body,teacher,{timeout:120000});
 status(response,200);return {archiveBytes:size,entries:32,elapsedMs:Date.now()-started};
},{scope:'QA aislado, imagen final y tmpfs de producción'});
