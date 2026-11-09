import fs from 'node:fs';
import {assert,api,status,register,login,sql,scalar,rows,quote,room,join,start,current,spin,openWindow,answer,caseTest,delay,UI,OUT} from './common.mjs';
import {formatTiempo} from '../../frontend/src/services/formatTiempo.js';
import {launchBrowser} from './browser-runtime.mjs';
const browser=await launchBrowser(),ctx=await browser.newContext({ignoreHTTPSErrors:true,reducedMotion:'reduce'}),page=await ctx.newPage();page.setDefaultTimeout(15000);
const users=await Promise.all([login(await register()),login(await register())]);const stamp=Date.now();
const cat=rows(`insert into categorias(nombre,color) values ('Stats_${stamp}','#${(stamp%16000000).toString(16).padStart(6,'0')}') returning id,nombre`)[0];const sub=Number(scalar(`insert into asignaturas(nombre,categoria_id) values ('Stats_${stamp}',${cat.id}) returning id`));
for(let i=0;i<4;i++){const q=Number(scalar(`insert into preguntas(identificador,enunciado,estado,dificultad,asignatura_id,creada_por) values ('STATS_${stamp}_${i}','Pregunta accesible ${i}','PUBLICADA','MEDIO',${sub},${users[0].id}) returning id`));sql(`insert into respuestas(pregunta_id,texto_respuesta,es_correcta,orden) values (${q},'Dos',true,1),(${q},'Tres',false,2)`);}
const game=await room(users[0],cat,{aciertosParaQuesito:1});await join(game,users[1]);await start(game,users[0]);const actor=users.find(u=>u.id===current(game));
await page.goto(UI+'/');await page.evaluate(u=>{for(const[k,v]of Object.entries({jwt:u.token,rol:u.role,idUsuario:u.id,nick:u.nick,email:u.email}))localStorage.setItem(k,String(v));},actor);
await page.goto(UI+`/partida/${game.idPartida}`);await page.getByRole('button',{name:'Girar Ruleta'}).waitFor();
await caseTest('UI-05','movimiento reducido y partida completa solo con teclado',async()=>{assert.ok(await page.evaluate(()=>matchMedia('(prefers-reduced-motion: reduce)').matches));assert.ok(await page.getByRole('button',{name:'Girar Ruleta'}).evaluate(e=>parseFloat(getComputedStyle(e).transitionDuration)<0.01));await page.getByRole('button',{name:'Girar Ruleta'}).press('Enter');await page.locator('.respuesta-card').first().waitFor();await page.waitForFunction(()=>document.activeElement?.classList.contains('pregunta-card__texto'));await page.keyboard.press('Tab');assert.ok(await page.evaluate(()=>document.activeElement?.classList.contains('respuesta-card')));await page.keyboard.press('Enter');await page.getByText(/has ganado|ha ganado/i).first().waitFor();},{scope:'QA Chrome teclado + prefers-reduced-motion; lector real excluido por el usuario'});
await caseTest('EST-12','todos los contadores, porcentajes, tiempo y redondeo tras partida',async()=>{const expected=status(await api('GET','/api/stats/jugador',undefined,actor),200);await page.goto(UI+'/jugador/estadisticas');await page.getByRole('heading',{name:'Mis estadísticas'}).waitFor();const values=await page.locator('.stats__card-valor').allInnerTexts();assert.deepEqual(values,[String(expected.partidasJugadas),String(expected.victorias),expected.porcentajeVictorias.toFixed(1)+'%',String(expected.totalRespondidas),String(expected.totalAciertos),String(expected.totalFallos),expected.porcentajeAciertos.toFixed(1)+'%',String(expected.totalNoRespondidas),formatTiempo(expected.tiempoMedioMs,2)]);const record=expected.categorias.find(c=>c.categoriaId===cat.id),card=page.locator('.stats__cat-card').filter({hasText:cat.nombre});assert.match(await card.innerText(),new RegExp(record.total+'\\s*pregs'));assert.ok((await card.innerText()).includes(formatTiempo(record.tiempoMedioMs)));await page.screenshot({path:OUT+'/statistics-after-game.png',fullPage:true});});
await caseTest('UI-07','aspecto original: columnas arriba y resultado solo con colores',async()=>{
  const g=await room(actor,cat,{modoEntrenamiento:true,maxJugadores:1,aciertosParaQuesito:30});
  await start(g,actor);
  await page.goto(UI+`/partida/${g.idPartida}`);
  await page.getByRole('button',{name:'Girar Ruleta'}).waitFor();
  assert.equal(await page.locator('.juego__estado, .scoreboard__turno-texto').count(),0);
  const layout=await page.locator('.juego').evaluate(main=>{
    const top=main.getBoundingClientRect().top+parseFloat(getComputedStyle(main).paddingTop);
    return {top,left:main.querySelector('.juego__columna-izq').getBoundingClientRect().top,right:main.querySelector('.juego__columna-der').getBoundingClientRect().top};
  });
  assert.ok(Math.abs(layout.left-layout.top)<2,JSON.stringify(layout));
  assert.ok(Math.abs(layout.right-layout.top)<2,JSON.stringify(layout));
  await page.screenshot({path:OUT+'/game-original-layout.png',fullPage:true});
  await page.getByRole('button',{name:'Girar Ruleta'}).press('Enter');
  await page.locator('.respuesta-card').first().waitFor();
  await page.locator('.respuesta-card').filter({hasText:'Tres'}).click();
  await page.locator('.respuesta-card--incorrecta').waitFor();
  assert.equal(await page.locator('.respuesta-card--correcta').count(),1);
  assert.equal(await page.locator('.respuesta-card--incorrecta').count(),1);
  assert.equal(await page.locator('.respuesta-card__estado, .juego__estado').count(),0);
  assert.equal(await page.locator('.respuesta-card--correcta').evaluate(el=>getComputedStyle(el).borderColor),'rgb(34, 197, 94)');
  assert.equal(await page.locator('.respuesta-card--incorrecta').evaluate(el=>getComputedStyle(el).borderColor),'rgb(239, 68, 68)');
  await page.screenshot({path:OUT+'/answer-original-colors.png',fullPage:true});
},{scope:'QA Chrome; presentación original solicitada por el usuario'});

await ctx.close();await browser.close();console.log('FINISH STATS completed');
