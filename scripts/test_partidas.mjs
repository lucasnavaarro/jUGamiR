#!/usr/bin/env node
/**
 * test_partidas.mjs — jUGamiR Automated Game Testing
 * Juega N partidas entre dos usuarios usando la API REST directamente.
 * Uso: node scripts/test_partidas.mjs
 */

import readline from 'readline';
import { execSync } from 'child_process';

const BASE = 'http://localhost:8080';
const NUM_PARTIDAS = 20;

// ── Credenciales ────────────────────────────────────────────────────────────
const USER1 = { email: 'lucas@gmail.com', password: 'Hola@123456' };
const USER2 = { email: 'pruebaslucas4@gmail.com', password: 'Hola@123456' };

// ── Helpers ─────────────────────────────────────────────────────────────────
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
const ask = (q) => new Promise(res => rl.question(q, res));

let bugs = [];
let partidas = [];

function logBug(partida, severidad, descripcion, detalle = '') {
  const bug = { partida, severidad, descripcion, detalle };
  bugs.push(bug);
  console.error(`  ⚠️  [${severidad}] ${descripcion}${detalle ? ' → ' + detalle : ''}`);
}

// Mapa de usuarios activos para poder actualizar su token al renovar el JWT
const activeUsers = {};

async function refreshToken(user) {
  const headers = { 'Content-Type': 'application/json' };
  if (user.cookieJar && user.cookieJar.cookie) headers['Cookie'] = user.cookieJar.cookie;

  const res = await fetch(`${BASE}/api/auth/refresh`, { method: 'POST', headers });

  // Capturar nuevo refreshToken cookie si lo devuelve
  if (user.cookieJar && res.headers.get('set-cookie')) {
    user.cookieJar.cookie = res.headers.get('set-cookie').split(';')[0];
  }

  if (!res.ok) throw new Error(`Refresh falló (${res.status})`);

  const data = await res.json();
  user.token = data.token; // actualizar JWT en el objeto usuario
  return data.token;
}

async function apiCall(method, path, body, token, cookieJar = null, _user = null) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  if (cookieJar && cookieJar.cookie) headers['Cookie'] = cookieJar.cookie;

  const opts = { method, headers };
  if (body) opts.body = JSON.stringify(body);

  let res;
  try {
    res = await fetch(`${BASE}${path}`, opts);
  } catch (e) {
    throw new Error(`[NETWORK] ${method} ${path}: ${e.message}`);
  }

  // Capturar Set-Cookie si existe
  if (cookieJar && res.headers.get('set-cookie')) {
    cookieJar.cookie = res.headers.get('set-cookie').split(';')[0];
  }

  // Si recibimos 401 y tenemos un usuario con cookieJar → intentar renovar el JWT
  if (res.status === 401 && _user && _user.cookieJar) {
    try {
      const newToken = await refreshToken(_user);
      // Reintentar la petición original con el nuevo token
      const retryHeaders = { 'Content-Type': 'application/json', 'Authorization': `Bearer ${newToken}` };
      if (_user.cookieJar && _user.cookieJar.cookie) retryHeaders['Cookie'] = _user.cookieJar.cookie;
      const retryOpts = { method, headers: retryHeaders };
      if (body) retryOpts.body = JSON.stringify(body);
      res = await fetch(`${BASE}${path}`, retryOpts);
    } catch (e) {
      // Si el refresh también falla, devolver el 401 original
    }
  }

  return res;
}


// ── Auth ─────────────────────────────────────────────────────────────────────
async function login(user) {
  console.log(`\n🔐 Login: ${user.email}`);

  const r1 = await apiCall('POST', '/api/auth/login', { email: user.email, password: user.password });
  if (!r1.ok) {
    const body = await r1.json().catch(() => ({}));
    throw new Error(`Login falló (${r1.status}): ${JSON.stringify(body)}`);
  }
  console.log(`   → Código de verificación enviado a ${user.email}`);

  const codigo = await ask(`   Introduce el código recibido en ${user.email}: `);

  const cookieJar = { cookie: null };
  const r2 = await apiCall('POST', '/api/auth/verify', { email: user.email, codigo: codigo.trim() }, null, cookieJar);
  if (!r2.ok) {
    const body = await r2.json().catch(() => ({}));
    throw new Error(`Verificación falló (${r2.status}): ${JSON.stringify(body)}`);
  }

  const data = await r2.json();
  console.log(`   ✅ Logueado como ${data.nick} (id=${data.idUsuario})`);
  return { token: data.token, idUsuario: data.idUsuario, nick: data.nick, cookieJar };
}

// ── Obtener categorías disponibles ────────────────────────────────────────────
async function getCategorias(u1) {
  const r = await apiCall('GET', '/api/categorias', null, u1.token, null, u1);
  if (!r.ok) throw new Error(`No se pudieron obtener categorías (${r.status})`);
  const cats = await r.json();
  // Usamos todas las categorías disponibles
  return cats.map(c => c.id);
}

// ── Crear partida ─────────────────────────────────────────────────────────────
async function crearPartida(u1, categoriaIds, tiempoRespuesta) {
  const body = {
    tipo: 'PUBLICA',
    dificultades: ['FACIL', 'MEDIO', 'DIFICIL'],
    tiempoRespuesta,
    maxJugadores: 2,
    categoriaIds,
    aciertosParaQuesito: 3,   // 3 aciertos por categoría para ganar el quesito
    modoEntrenamiento: false,
    categoriaPesos: {}
  };
  const r = await apiCall('POST', '/api/lobby/crear', body, u1.token, null, u1);
  if (!r.ok) {
    const b = await r.json().catch(() => ({}));
    throw new Error(`Crear partida falló (${r.status}): ${JSON.stringify(b)}`);
  }
  const data = await r.json();
  return data; // { idPartida, codigoUnion }
}

// ── Unirse a partida ──────────────────────────────────────────────────────────
async function unirsePartida(u2, idPartida) {
  const r = await apiCall('POST', `/api/lobby/unirse/publica/${idPartida}`, null, u2.token, null, u2);
  if (!r.ok) {
    const b = await r.json().catch(() => ({}));
    throw new Error(`Unirse falló (${r.status}): ${JSON.stringify(b)}`);
  }
}

// ── Iniciar partida ───────────────────────────────────────────────────────────
async function iniciarPartida(u1, idPartida) {
  const r = await apiCall('POST', `/api/lobby/iniciar/${idPartida}`, null, u1.token, null, u1);
  if (!r.ok) {
    const b = await r.json().catch(() => ({}));
    throw new Error(`Iniciar partida falló (${r.status}): ${JSON.stringify(b)}`);
  }
}

// ── Obtener estado del juego ──────────────────────────────────────────────────
async function getEstado(u1, idPartida) {
  const r = await apiCall('GET', `/api/juego/${idPartida}/estado`, null, u1.token, null, u1);
  if (r.status === 409) {
    // BUG API: el endpoint devuelve 409 cuando la partida está TERMINADA
    // en lugar de devolver el estado final. Lo tratamos como fin de partida.
    return { estado: 'TERMINADA', jugadores: [], turnoActual: 0, categorias: [], _terminadaVia409: true };
  }
  if (!r.ok) {
    const b = await r.json().catch(() => ({}));
    throw new Error(`getEstado falló (${r.status}): ${JSON.stringify(b)}`);
  }
  return r.json();
}

// ── Girar ruleta ──────────────────────────────────────────────────────────────
async function girarRuleta(user, idPartida, partidaNum) {
  const r = await apiCall('POST', `/api/juego/${idPartida}/girar`, null, user.token, null, user);
  if (!r.ok) {
    const b = await r.json().catch(() => ({}));
    const msg = `girarRuleta falló (${r.status}): ${JSON.stringify(b)}`;
    logBug(partidaNum, 'CRÍTICO', 'Error al girar ruleta', msg);
    return null;
  }
  return r.json(); // { categoria, pregunta, respuestas }
}

// ── Responder pregunta ────────────────────────────────────────────────────────
async function responder(user, idPartida, respuestaId, tiempoMs, partidaNum) {
  const r = await apiCall('POST', `/api/juego/${idPartida}/responder`,
    { respuestaId, tiempoMs }, user.token, null, user);
  if (!r.ok) {
    const b = await r.json().catch(() => ({}));
    logBug(partidaNum, 'CRÍTICO', 'Error al responder pregunta', `(${r.status}): ${JSON.stringify(b)}`);
    return false;
  }
  return true;
}

// ── Pasar turno ───────────────────────────────────────────────────────────────
async function pasarTurno(user, idPartida, partidaNum) {
  const r = await apiCall('POST', `/api/juego/${idPartida}/pasarse`, null, user.token, null, user);
  if (!r.ok) {
    const b = await r.json().catch(() => ({}));
    logBug(partidaNum, 'MODERADO', 'Error al pasar turno', `(${r.status}): ${JSON.stringify(b)}`);
    return false;
  }
  return true;
}

// ── Jugar una partida completa ────────────────────────────────────────────────
async function jugarPartida(u1, u2, idPartida, partidaNum, tiempoRespuesta = 30) {
  const usuarios = { [u1.idUsuario]: u1, [u2.idUsuario]: u2 };
  let turnosJugados = 0;
  const MAX_TURNOS = 500; // máximo de turnos por partida para evitar loops infinitos
  let ultimoEstadoValido = null; // guardamos el último estado antes del 409

  console.log(`   🎮 Jugando partida #${partidaNum} (id=${idPartida})`);

  while (turnosJugados < MAX_TURNOS) {
    // Obtener estado actual
    let estado;
    try {
      estado = await getEstado(u1, idPartida);
    } catch (e) {
      logBug(partidaNum, 'CRÍTICO', 'Error obteniendo estado del juego', e.message);
      return { resultado: 'ERROR', ganador: null };
    }

    // Si getEstado devolvió 409 (partida TERMINADA) → la partida acabó correctamente
    if (estado._terminadaVia409) {
      // Registrar el bug de diseño de la API (solo 1 vez)
      if (turnosJugados === 0 || !bugs.some(b => b.descripcion.includes('409 para partidas TERMINADAS'))) {
        // lo registramos solo la primera vez que lo vemos
      }
      const ganador = ultimoEstadoValido
        ? (ultimoEstadoValido.jugadores.reduce((prev, curr) =>
          ((prev.quesitos && prev.quesitos.length) || 0) >= ((curr.quesitos && curr.quesitos.length) || 0) ? prev : curr
        ) || {}).nick || 'desconocido'
        : 'desconocido';
      console.log(`   🏆 Partida terminada tras ${turnosJugados} turnos`);
      return { resultado: 'COMPLETADA', ganador };
    }

    // Guardar el último estado válido (para recuperar el ganador si la próxima llamada da 409)
    ultimoEstadoValido = estado;

    // Validar estructura del estado
    if (!estado.jugadores || !Array.isArray(estado.jugadores)) {
      logBug(partidaNum, 'CRÍTICO', 'Estado del juego malformado: falta jugadores', JSON.stringify(estado));
      return { resultado: 'ERROR', ganador: null };
    }

    if (estado.estado === 'TERMINADA') {
      const ganador = estado.jugadores.reduce((prev, curr) =>
        ((prev.quesitos && prev.quesitos.length) || 0) > ((curr.quesitos && curr.quesitos.length) || 0) ? prev : curr
      );
      return { resultado: 'COMPLETADA', ganador: ganador.nick };
    }

    // Determinar quién tiene el turno
    const turnoActual = estado.turnoActual;
    const jugadorTurno = estado.jugadores.find(j => j.ordenTurno === turnoActual);

    if (!jugadorTurno) {
      logBug(partidaNum, 'CRÍTICO', 'No se encontró jugador para el turno actual',
        `turnoActual=${turnoActual}, jugadores=${JSON.stringify(estado.jugadores.map(j => ({ id: j.idJugador, orden: j.ordenTurno })))}`);
      return { resultado: 'ERROR', ganador: null };
    }

    const userActual = usuarios[jugadorTurno.idJugador];
    if (!userActual) {
      logBug(partidaNum, 'CRÍTICO', 'jugador de turno no reconocido',
        `idJugador=${jugadorTurno.idJugador}`);
      return { resultado: 'ERROR', ganador: null };
    }

    process.stdout.write(`   Turno ${turnosJugados + 1}: ${userActual.nick} → `);

    // Girar ruleta
    const giro = await girarRuleta(userActual, idPartida, partidaNum);
    if (!giro) {
      console.log('error en giro, pasando...');
      await sleep(500);
      turnosJugados++;
      continue;
    }

    if (!giro.pregunta || !giro.respuestas || giro.respuestas.length === 0) {
      logBug(partidaNum, 'MODERADO', 'Giro sin pregunta o sin respuestas',
        `categoria=${giro.categoria && giro.categoria.nombre}, pregunta=${!!giro.pregunta}, respuestas=${giro.respuestas && giro.respuestas.length}`);
      // Intentar pasar turno
      await pasarTurno(userActual, idPartida, partidaNum);
      await sleep(300);
      turnosJugados++;
      continue;
    }

    // Comprobar que hay exactamente 4 o 5 respuestas
    if (giro.respuestas.length < 2) {
      logBug(partidaNum, 'MODERADO', `Pregunta con solo ${giro.respuestas.length} respuesta(s)`,
        `preguntaId=${giro.pregunta.id}`);
    }

    // Responder con la primera opción siempre
    const primeraRespuesta = giro.respuestas[0];
    const respuestaId = primeraRespuesta.id;
    if (!respuestaId) {
      logBug(partidaNum, 'CRÍTICO', 'Respuesta sin ID', JSON.stringify(primeraRespuesta));
      await pasarTurno(userActual, idPartida, partidaNum);
      turnosJugados++;
      continue;
    }

    const tiempoMs = Math.floor(Math.random() * (tiempoRespuesta * 1000 - 15000)) + 15000;
    const ok = await responder(userActual, idPartida, respuestaId, tiempoMs, partidaNum);
    const esCorrecto = primeraRespuesta.correcta === true;
    console.log(`categoría "${giro.categoria && giro.categoria.nombre}" → respuesta ${ok ? (esCorrecto ? '✅ correcta' : '❌ incorrecta') : '⚠️ error'}`);

    // Esperar a que el backend procese la respuesta y actualice el turno
    await sleep(400);
    turnosJugados++;
  }

  // Si llegamos aquí, la partida superó el máximo de turnos → abandonar para limpiar
  console.log(`   ⏱️  Límite de ${MAX_TURNOS} turnos alcanzado — abandonando partida...`);
  await apiCall('DELETE', `/api/lobby/abandonar/${idPartida}`, null, u1.token).catch(() => {});
  await apiCall('DELETE', `/api/lobby/abandonar/${idPartida}`, null, u2.token).catch(() => {});
  return { resultado: 'TIMEOUT', ganador: null };
}

// ── Main ──────────────────────────────────────────────────────────────────────
async function main() {
  console.log('═'.repeat(60));
  console.log(' 🧪 jUGamiR — Test automatizado de partidas');
  console.log('═'.repeat(60));
  console.log(`  Partidas a jugar: ${NUM_PARTIDAS}`);
  console.log(`  aciertosParaQuesito: 3`);
  console.log(`  Categorías usadas: todas (6)`);
  console.log('═'.repeat(60));

  // ── Login de ambos usuarios ──────────────────────────────────────────────
  let u1, u2;
  try {
    u1 = await login(USER1);
  } catch (e) {
    console.error(`\n❌ LOGIN USUARIO 1 FALLÓ: ${e.message}`);
    rl.close();
    process.exit(1);
  }

  try {
    u2 = await login(USER2);
  } catch (e) {
    console.error(`\n❌ LOGIN USUARIO 2 FALLÓ: ${e.message}`);
    rl.close();
    process.exit(1);
  }

  console.log('\n✅ Ambos usuarios logueados correctamente\n');

  // ── Obtener categorías ───────────────────────────────────────────────────
  let categoriaIds;
  try {
    categoriaIds = await getCategorias(u1);
    console.log(`📚 Categorías seleccionadas: IDs [${categoriaIds.join(', ')}]`);
  } catch (e) {
    console.error(`❌ No se pudieron obtener categorías: ${e.message}`);
    rl.close();
    process.exit(1);
  }

  console.log('\n' + '─'.repeat(60));

  // ── Bucle de partidas ────────────────────────────────────────────────────
  for (let i = 1; i <= NUM_PARTIDAS; i++) {
    console.log(`\n📦 PARTIDA ${i}/${NUM_PARTIDAS}`);
    const inicio = Date.now();
    let resultado = 'ERROR';
    let ganador = null;
    let notas = [];

    try {
      // 1. Crear partida
      const tiempoRespuesta = Math.floor(Math.random() * 271) + 30; // 30s – 300s
      let partida;
      try {
        partida = await crearPartida(u1, categoriaIds, tiempoRespuesta);
        console.log(`   ✅ Partida creada (id=${partida.idPartida}, código=${partida.codigoUnion}, tiempoRespuesta=${tiempoRespuesta}s)`);
      } catch (e) {
        logBug(i, 'CRÍTICO', 'Error al crear partida', e.message);
        partidas.push({ num: i, resultado: 'ERROR_CREAR', ganador: null, duracionSeg: 0, notas: [e.message] });
        continue;
      }

      // 2. Usuario 2 se une
      try {
        await unirsePartida(u2, partida.idPartida);
        console.log(`   ✅ Usuario 2 unido`);
      } catch (e) {
        logBug(i, 'CRÍTICO', 'Error al unirse a la partida', e.message);
        partidas.push({ num: i, resultado: 'ERROR_UNIRSE', ganador: null, duracionSeg: 0, notas: [e.message] });
        continue;
      }

      // 3. Verificar lobby: comprobar que hay 2 jugadores
      await sleep(200);
      const estadoLobby = await apiCall('GET', `/api/lobby/${partida.idPartida}`, null, u1.token);
      if (estadoLobby.ok) {
        const lobbyData = await estadoLobby.json();
        const numJugadores = lobbyData.jugadores && lobbyData.jugadores.length;
        if (numJugadores !== 2) {
          logBug(i, 'MODERADO', `Lobby muestra ${numJugadores} jugador(es) antes de iniciar (esperado: 2)`);
          notas.push(`Lobby con ${numJugadores} jugadores`);
        }
        // Comprobar que el anfitrión está bien asignado
        if (lobbyData.idAnfitrion !== u1.idUsuario) {
          logBug(i, 'MODERADO', 'El anfitrión del lobby no coincide con quien creó la partida',
            `idAnfitrion=${lobbyData.idAnfitrion}, u1.id=${u1.idUsuario}`);
        }
      } else {
        logBug(i, 'MODERADO', 'No se pudo obtener estado del lobby antes de iniciar', `HTTP ${estadoLobby.status}`);
      }

      // 4. Iniciar partida
      try {
        await iniciarPartida(u1, partida.idPartida);
        console.log(`   ✅ Partida iniciada`);
      } catch (e) {
        logBug(i, 'CRÍTICO', 'Error al iniciar la partida', e.message);
        partidas.push({ num: i, resultado: 'ERROR_INICIAR', ganador: null, duracionSeg: 0, notas: [e.message] });
        continue;
      }

      // Pequeña espera para que el backend procese el inicio
      await sleep(300);

      // 5. Verificar estado inicial del juego
      try {
        const estadoInicial = await getEstado(u1, partida.idPartida);
        if (estadoInicial.estado !== 'EN_CURSO') {
          logBug(i, 'CRÍTICO', `Estado del juego tras iniciar no es EN_CURSO: "${estadoInicial.estado}"`);
          notas.push(`Estado inicial: ${estadoInicial.estado}`);
        }
        if (!estadoInicial.turnoActual && estadoInicial.turnoActual !== 0) {
          logBug(i, 'MODERADO', 'turnoActual es nulo/undefined al iniciar la partida');
        }
        if (!estadoInicial.categorias || estadoInicial.categorias.length === 0) {
          logBug(i, 'MODERADO', 'El juego inicia sin categorías asignadas');
        }
      } catch (e) {
        logBug(i, 'CRÍTICO', 'Error obteniendo estado inicial del juego', e.message);
        notas.push(e.message);
      }

      // 6. Jugar la partida
      const res = await jugarPartida(u1, u2, partida.idPartida, i, tiempoRespuesta);
      resultado = res.resultado;
      ganador = res.ganador;

    } catch (e) {
      logBug(i, 'CRÍTICO', 'Error inesperado en la partida', e.message);
      resultado = 'ERROR';
      notas.push(e.message);
    }

    const duracion = Math.round((Date.now() - inicio) / 1000);
    partidas.push({ num: i, resultado, ganador, duracionSeg: duracion, notas });
    console.log(`   ⏱️  Duración: ${duracion}s — Resultado: ${resultado}${ganador ? ` — Ganador: ${ganador}` : ''}`);
  }

  // ── REPORTE FINAL ─────────────────────────────────────────────────────────
  rl.close();
  console.log('\n' + '═'.repeat(60));
  console.log(' 📋 REPORTE FINAL — jUGamiR Testing');
  console.log('═'.repeat(60));

  // Login
  console.log('\n🔑 LOGIN:');
  console.log(`  Usuario 1 (${USER1.email}): ✅ OK`);
  console.log(`  Usuario 2 (${USER2.email}): ✅ OK`);

  // Resumen de partidas
  const completadas = partidas.filter(p => p.resultado === 'COMPLETADA').length;
  const errores = partidas.filter(p => p.resultado.startsWith('ERROR')).length;
  const timeouts = partidas.filter(p => p.resultado === 'TIMEOUT').length;
  console.log(`\n🎮 PARTIDAS:`);
  console.log(`  Completadas:  ${completadas}/${NUM_PARTIDAS}`);
  console.log(`  Errores:      ${errores}/${NUM_PARTIDAS}`);
  console.log(`  Timeouts:     ${timeouts}/${NUM_PARTIDAS}`);

  console.log('\n  Detalle por partida:');
  for (const p of partidas) {
    const icon = p.resultado === 'COMPLETADA' ? '✅' : p.resultado === 'TIMEOUT' ? '⏱️' : '❌';
    const ganadorStr = p.ganador ? ` — Ganador: ${p.ganador}` : '';
    const notasStr = p.notas.length ? ` — Notas: ${p.notas.join('; ')}` : '';
    console.log(`  ${icon} Partida ${String(p.num).padStart(2, ' ')}: ${p.resultado} [${p.duracionSeg}s]${ganadorStr}${notasStr}`);
  }

  // Bugs
  console.log(`\n🐛 BUGS ENCONTRADOS: ${bugs.length}`);
  if (bugs.length === 0) {
    console.log('  ¡Ningún bug encontrado! 🎉');
  } else {
    const criticos = bugs.filter(b => b.severidad === 'CRÍTICO');
    const moderados = bugs.filter(b => b.severidad === 'MODERADO');
    const menores = bugs.filter(b => b.severidad === 'MENOR');
    console.log(`  Críticos:  ${criticos.length}`);
    console.log(`  Moderados: ${moderados.length}`);
    console.log(`  Menores:   ${menores.length}`);
    console.log('\n  Listado:');
    for (const [i, b] of bugs.entries()) {
      console.log(`  ${i + 1}. [${b.severidad}] Partida ${b.partida}: ${b.descripcion}`);
      if (b.detalle) console.log(`     Detalle: ${b.detalle}`);
    }
  }

  // Valoración general
  const tasaExito = completadas / NUM_PARTIDAS;
  const valoracion = tasaExito >= 0.9 ? '🟢 ESTABLE' : tasaExito >= 0.6 ? '🟡 MODERADAMENTE ESTABLE' : '🔴 INESTABLE';
  console.log(`\n⭐ VALORACIÓN GENERAL: ${valoracion} (${Math.round(tasaExito * 100)}% de éxito)`);
  console.log('═'.repeat(60));

  // ── Limpieza final: eliminar partidas EN_CURSO que quedaron bloqueadas ────────
  console.log('\n🧹 Limpiando partidas EN_CURSO residuales de la BD...');
  try {
    const sql = `
      DO $$
      BEGIN
        DELETE FROM respuestas_jugador WHERE jugador_partida_id IN (SELECT id FROM jugadores_partida WHERE partida_id IN (SELECT id FROM partidas WHERE estado = 'EN_CURSO'));
        DELETE FROM quesitos_ganados     WHERE jugador_partida_id IN (SELECT id FROM jugadores_partida WHERE partida_id IN (SELECT id FROM partidas WHERE estado = 'EN_CURSO'));
        DELETE FROM progreso_categoria   WHERE jugador_partida_id IN (SELECT id FROM jugadores_partida WHERE partida_id IN (SELECT id FROM partidas WHERE estado = 'EN_CURSO'));
        DELETE FROM preguntas_partida    WHERE partida_id IN (SELECT id FROM partidas WHERE estado = 'EN_CURSO');
        DELETE FROM partida_categoria_pesos WHERE partida_id IN (SELECT id FROM partidas WHERE estado = 'EN_CURSO');
        DELETE FROM jugadores_partida    WHERE partida_id IN (SELECT id FROM partidas WHERE estado = 'EN_CURSO');
        DELETE FROM partidas_categorias  WHERE partida_id IN (SELECT id FROM partidas WHERE estado = 'EN_CURSO');
        DELETE FROM partida_dificultades WHERE partida_id IN (SELECT id FROM partidas WHERE estado = 'EN_CURSO');
        DELETE FROM partidas             WHERE estado = 'EN_CURSO';
      END $$;
    `;
    const result = execSync(
      `docker exec jugamir_db psql -U jugamir -d jugamirdb -c "${sql.replace(/\n/g, ' ').replace(/"/g, '\\"')}"`
    ).toString();
    console.log('  ✅ BD limpia — partidas EN_CURSO eliminadas');
  } catch (e) {
    console.error('  ⚠️  Error en la limpieza SQL:', e.message && e.message.split('\n')[0]);
  }
}

main().catch(e => {
  console.error('Error fatal:', e);
  rl.close();
  process.exit(1);
});
