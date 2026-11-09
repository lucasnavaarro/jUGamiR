"""Consolida evidencia histórica sin borrar los intentos fallidos."""
from pathlib import Path
import json, re, collections, hashlib, xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[2]
out=root/'docs/variantes-pendientes-2026-11-05'
records=[json.loads(s) for s in (out/'observaciones.ndjson').read_text().splitlines() if s.strip()]
def key(r):
    return r['id'],re.sub(r'P\d{12,}', 'P<fixture>',r['variant'])
latest={key(r):r for r in records}
fail=[r for r in latest.values() if r['result']=='FALLO']
(out/'ultimas-variantes.json').write_text(json.dumps(list(latest.values()),ensure_ascii=False,indent=2))
java={k:0 for k in ['tests','failures','errors','skipped']}
for f in (root/'backend/target/surefire-reports').glob('TEST-*.xml'):
    x=ET.parse(f).getroot()
    for k in java:java[k]+=int(x.attrib.get(k,0))
(out/'java-tests.json').write_text(json.dumps(java,indent=2))
original=json.loads((root/'docs/ejecucion-completa-2026-11-04/casos.json').read_text())
corrected={r['id'] for r in map(json.loads,(root/'docs/correcciones-completas-2026-11-05/observaciones.ndjson').read_text().splitlines()) if r['result']=='OK'}
current=collections.defaultdict(list)
for r in latest.values():current[r['id']].append(r)
partial={ 'WS-11':'Proxy público: sesión propia revisada en lectura; repetición instrumentada de la versión nueva pendiente de despliegue. Ambos proxies QA, 120 s y sustitución de backend probados.', 'WS-12':'Sin Android físico. iPhone solo puede acceder a producción: cambio Wi-Fi/datos y suspensión física de esta versión pendientes. Latencia táctil y congelación de pestaña probadas en QA.' }
lines=['# Matriz consolidada de los 222 casos','', 'Estados limitados a los entornos y variantes de la batería; no certifican producción ni ausencia de defectos desconocidos. La evidencia original se conserva. «VERIFICADO QA» integra la primera campaña, correcciones y ampliación; no implica repetir aquí todas las observaciones anteriores.', '', '| Caso | Estado | Evidencia / límite |','|---|---|---|']
counts=collections.Counter()
for c in original:
    cid=c['id']; checks=current[cid]
    state='VERIFICADO QA'; note='Campaña original conservada.'
    if c['status']=='NO APLICA':state='NO APLICA';note='La funcionalidad descrita no existe en esta aplicación.'
    elif cid in partial:state='PARCIAL EXTERNO';note=partial[cid]
    elif any(r['result']=='FALLO' for r in checks):state='REVISAR';note='Consultar últimas variantes y el historial de intentos.'
    elif checks:note=f'{len(checks)} variantes de ampliación registradas, además de la evidencia anterior.'
    elif cid in corrected:note='Verificado en la campaña de correcciones y sus decisiones aprobadas.'
    elif c['status'] not in ('OK','NO APLICA'):state='REVISAR';note='Falta vincular evidencia específica de ampliación.'
    if cid=='UI-05':note+=' Lector de pantalla real excluido por petición del usuario; teclado y movimiento reducido probados.'
    if cid=='UI-07':note+=' Axe no constituye una certificación WCAG; las comprobaciones incompletas del motor se conservan en accessibility.json.'
    if cid=='REC-01':note+=' SMTP capturado en QA; entrega a un buzón externo real no certificada.'
    if cid=='NAV-01':note+=' La corrección de health requiere despliegue para verificarse en el dominio real.'
    counts[state]+=1;lines.append(f"| {cid} | {state} | {note} |")
(out/'MATRIZ-222-CASOS.md').write_text('\n'.join(lines)+'\n')
summary={'observations':len(records),'semanticVariants':len(latest),'latestFailures':len(fail),'matrix':dict(counts),'java':java}
(out/'resumen.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2))
files=[]
for folder in ['backend/src','frontend/src','db','scripts/qa']:
    for p in (root/folder).rglob('*'):
        if p.is_file() and p.suffix in {'.java','.jsx','.js','.mjs','.py','.sql','.css','.conf','.yaml'}:
            files.append({'path':str(p.relative_to(root)),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
(out/'source-manifest.json').write_text(json.dumps(files,indent=2))
print(json.dumps(summary,ensure_ascii=False))
for r in fail:print('REVISAR',r['id'],r['variant'])
