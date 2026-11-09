"""Build a case-by-case ledger without treating missing variants as passes."""
from pathlib import Path
from collections import Counter,defaultdict
from xml.etree import ElementTree as ET
import json,re,shutil

root=Path(__file__).resolve().parents[2]
out=root/'docs/ejecucion-completa-2026-11-04'
clean=lambda s:re.sub(r'\x1b\[[0-9;]*m','',re.sub(r'[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}','[JWT REDACTADO]',str(s)))
events=[json.loads(line) for line in (out/'observaciones.ndjson').read_text().splitlines() if line.strip()]
for e in events:e['detail']=clean(e.get('detail',''))
# Keep all observations for audit, but use the latest rerun of an identical variant.
(out/'observaciones.ndjson').write_text(''.join(json.dumps(e,ensure_ascii=False)+'\n' for e in events))
latest={}
for e in events:latest[e['id'],e['variant']]=e
group=defaultdict(list)
for e in latest.values():group[e['id']].append(e)
policy={
 'REG-09':'Se probó la normalización; falta decidir si email/nick deben ser insensibles a mayúsculas y espacios.',
 'CRE-07':'Se probaron 4,5,29,30,300,301,600,601; falta aprobar API 5–600 frente a UI 30–300.',
 'CRE-11':'El listado público devuelve email del anfitrión; política de datos públicos sin aprobar.',
 'ENT-10':'El individual rechaza un segundo jugador; la API acepta variantes públicas/multijugador pendientes de política.',
 'EST-05':'Se observó diferencia de población entre resumen y detalle; falta aprobar/rotular esa semántica.',
 'EST-06':'Se midió qué resultados entran en denominadores; falta aprobar tratamiento de expulsión/abandono/cancelación.',
 'PRO-13':'Se verificó gestión compartida entre profesores; falta aprobar restricción por autor.',
 'CSV-05':'El importador admite identificador ausente; decidir si ese campo es obligatorio en el contrato del CSV.',
}
partial={
 'WS-09':'Se cerraron 20 sockets directos. La navegación repetida de UI quedó bloqueada por el limitador del proxy; no se ha probado fuga de memoria, timers o listeners.',
}
# A partial probe can be completed by a later independent UI/API/SMTP probe.
# These closures concern functionality on the QA build, not the physical-device matrix
# or a claim that the unpublished build is already installed in production.
combined=set('NAV-05 NAV-07 NAV-08 REG-01 REG-02 REG-07 REG-08 AUT-01 SES-01 SES-02 SES-03 SES-06 SES-09 REC-07 PER-01 PER-02 PER-05 PER-07 CRE-01 CRE-03 CRE-04 CRE-09 LOB-07 LOB-10 LOB-11 RUL-01 RUL-09 RES-01 RES-02 RES-03 EST-01 EST-04 IMG-08 IMG-09 IMG-11 OPS-03 ENT-02 ENT-03 ENT-04 FIN-02 RES-04 RES-11 RUL-07 API-03 OPS-06 WS-07 API-02'.split())
cases=json.loads((out/'casos.json').read_text())
for c in cases:
 checks=group[c['id']];c['checks']=checks
 if c['id'] in policy:c['status']='BLOQUEADO';c['remaining']=policy[c['id']]
 elif c['id']=='UI-04':c['status']='NO APLICA';c['remaining']='La aplicación representa la imagen sin control de ampliación/modal. La batería suponía una función no implementada; no se adjudica un fallo de ejecución a ese control inexistente.'
 elif c['id'] in partial:c['status']='PARCIAL';c['remaining']=partial[c['id']]
 elif any(x['result']=='FALLO' for x in checks):c['status']='FALLO';c['remaining']='Corregir y repetir las variantes fallidas; revisar las notas de cobertura de las demás.'
 elif not checks:c['status']='BLOQUEADO';c['remaining']='Sin ejecución dinámica disponible; no se considera aprobado.'
 elif c['id'] in combined:c['status']='OK';c['remaining']='Funcionalidad verificada combinando las evidencias API/SQL/SMTP/DOM/Java del caso. OK en QA; las variantes de dispositivos físicos y entrega real de correo quedan fuera de esta aprobación.'
 elif any(not x.get('complete',True) for x in checks):c['status']='PARCIAL';c['remaining']='Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción.'
 else:c['status']='OK';c['remaining']='Aprobado en las variantes y entorno ejecutados; no equivale a validación de otra versión de producción.'
(out/'casos.json').write_text(json.dumps(cases,ensure_ascii=False,indent=2))
(out/'ultimas-variantes.json').write_text(json.dumps(list(latest.values()),ensure_ascii=False,indent=2))
counts=Counter(c['status'] for c in cases)
assert len(cases)==222
java={'tests':0,'failures':0,'errors':0,'skipped':0}
javalines=[]
for p in sorted((root/'backend/target/surefire-reports').glob('TEST-*.xml')):
 s=ET.parse(p).getroot()
 for k in java:java[k]+=int(s.get(k,0))
 javalines.append(f"{s.get('name')}: tests={s.get('tests')}, failures={s.get('failures')}, errors={s.get('errors')}, skipped={s.get('skipped')}")
(out/'java-resumen.txt').write_text('\n'.join(javalines)+'\n'+str(java)+'\n')
loadpath=out/'load-results.json';load=json.loads(loadpath.read_text()) if loadpath.exists() else None
summary={'cases':222,'variants':len(latest),'observationsIncludingReruns':len(events),'statuses':dict(counts),'java':java,'loadFinished':load is not None}
if load:summary['load']={k:load[k] for k in ['actualDurationMs','cycles','httpP95','stompP95','sockets']};summary['load']['httpRequests']=len(load['http']);summary['load']['errors']=len(load['errors'])
(out/'resumen.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2))
lines=['# Matriz de los 222 casos de jUGamiR','',
 'Campaña iniciada el 4 y revisada el 5 de noviembre de 2026. Los estados se refieren a las variantes y entornos documentados. **PARCIAL y BLOQUEADO no son aprobaciones.** Los casos compuestos no se aprueban por superar solo una variante.','',
 '| Estado | Casos |','|---|---:|']
for name in ['OK','FALLO','PARCIAL','BLOQUEADO','NO APLICA']:lines.append(f'| {name} | {counts[name]} |')
lines+=['',f'Variantes diferenciadas: {len(latest)}. Observaciones incluidas las repeticiones: {len(events)}.','',
 '[Informe de conclusiones](../INFORME-BATERIA-COMPLETA-2026-11-05.md). [Detalles completos por caso](casos.json). [Historial de observaciones](observaciones.ndjson).','',
 '| Caso | Prioridad | Estado | Variantes | Evidencia y límite |','|---|---|---|---:|---|']
for c in cases:
 failures=[x for x in c['checks'] if x['result']=='FALLO']
 detail=(failures[0]['detail'] if failures else c['checks'][-1]['detail'] if c['checks'] else c['remaining'])
 if not detail:detail=c['checks'][-1]['variant'] if c['checks'] else c['remaining']
 detail=clean(detail).replace('|','/').replace('\n',' ')[:280]
 if c['status'] in ['BLOQUEADO','NO APLICA','PARCIAL']:detail=c['remaining'][:280]
 lines.append(f"| {c['id']} | {c['priority']} | {c['status']} | {len(c['checks'])} | {detail} |")
lines+=['','## Interpretación y restricciones','',
 'Las pruebas de escritura utilizaron dos bases QA y correo capturado; los códigos se leyeron en PostgreSQL y se comprobó su presencia en SMTP. En producción se hicieron consultas y navegación pública de solo lectura sobre https://jugamir.es/. No se crearon cuentas ni se borraron datos en producción.','',
 'Chrome y Firefox fueron motores de escritorio reales; WebKit fue el motor de Playwright, no Safari nativo. Los viewports/tacto móviles son emulación. No hay certificación de iPhone/Android físicos, lector de pantalla o entrega a un buzón institucional real.','',
 'Se conservan las ejecuciones iniciales del arnés y sus repeticiones corregidas. Se ajustaron payloads, selectores, esperas y fixtures que no respetaban restricciones del esquema; el resultado final utiliza la última observación de la misma variante. No se modificó código de producción para ocultar errores durante esta pasada.','',
 'El fallo de IMG-07 con un ZIP marcado como symlink significa que se aceptó el atributo como imagen regular recodificada; no se ha demostrado creación de enlace simbólico ni escape del directorio. WS-09 no demuestra una fuga de memoria: la variante de navegación quedó bloqueada por el limitador. Las diferencias de política se identifican como BLOQUEADO para decisión de producto.','']
(out/'MATRIZ-222-CASOS.md').write_text('\n'.join(lines))
for source,name in [('/private/tmp/jugamir-fullqa-node-final.log','node-tests.txt'),('/private/tmp/jugamir-fullqa-load.log','load-console.txt')]:
 if Path(source).exists():(out/name).write_text(clean(Path(source).read_text()))
for source,name in [('/private/tmp/jugamir-prod-https-headers.txt','produccion-https-headers.txt'),('/private/tmp/jugamir-prod-health-headers.txt','produccion-health-headers.txt'),('/private/tmp/jugamir-prod-http-headers.txt','produccion-http-headers.txt')]:
 if Path(source).exists():shutil.copyfile(source,out/name)
print(json.dumps(summary,ensure_ascii=False,indent=2))
