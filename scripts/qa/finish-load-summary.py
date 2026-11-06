from pathlib import Path
import json,collections,datetime,os
out=Path(os.environ.get('QA_OUT', Path(__file__).resolve().parents[2]/'docs/variantes-pendientes-2026-11-05'))
x=json.loads((out/'load-results.json').read_text())
def metric(rows):
 a=sorted(r['ms'] for r in rows)
 return {'requests':len(a),'p95Ms':a[min(len(a)-1,int(len(a)*.95))] if a else None,'errors':sum(r['status']!=200 for r in rows)}
summary={'durationMs':x['actualDurationMs'],'httpP95Ms':x['httpP95'],'stompP95Ms':x['stompP95'],'totalRequests':len(x['http']),'errors':len(x['errors']),'statusCounts':dict(collections.Counter(r['status'] for r in x['http'])),'publicEvents':x['publicEvents'],'publicIsolationErrors':x['publicIsolationErrors'],'groups':{}}
for name,rows in [('twoPlayers',[r for r in x['http'] if '/api/juego/' in r['path'] and r.get('players')==2]),('sixPlayers',[r for r in x['http'] if '/api/juego/' in r['path'] and r.get('players')==6]),('teacherSearch',[r for r in x['http'] if '/api/profesor/' in r['path']]),('statistics',[r for r in x['http'] if '/api/stats/' in r['path']])]:
 summary['groups'][name]=metric(rows)
 assert rows and summary['groups'][name]['errors']==0 and summary['groups'][name]['p95Ms']<500,(name,summary['groups'][name])
assert summary['errors']==0 and x['actualDurationMs']>=1800000 and x['httpP95']<500 and x['stompP95']<250
(out/'load-summary.json').write_text(json.dumps(summary,indent=2))
with (out/'observaciones.ndjson').open('a') as f:
 f.write(json.dumps(dict(id='REN-04',variant='búsqueda docente 5000 preguntas y estadísticas 1000 partidas durante carga',result='OK',complete=True,scope='QA audit_load, 30 minutos, hasta 50 clientes',detail=json.dumps(summary['groups']),durationMs=x['actualDurationMs'],at=datetime.datetime.now(datetime.timezone.utc).isoformat()))+'\n')
print(json.dumps(summary))
