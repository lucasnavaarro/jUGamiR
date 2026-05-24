#!/usr/bin/env python3
"""
Script de corrección para preguntas_mir_clasificadas (1).csv
Corrige Errores 1, 2, 3, 4 y 5 detectados en el análisis.
"""
import re
from collections import defaultdict

INPUT  = '/home/lucas/Escritorio/TFG/jUGamiR/preguntas_mir_clasificadas (1).csv'
OUTPUT = '/home/lucas/Escritorio/TFG/jUGamiR/preguntas_mir_final.csv'

VALID_RESP = {'1', '2', '3', '4', '5', 'ANULADA', 'IMPUGNADA', '--'}
NCOLS = 10

# ── Carga ─────────────────────────────────────────────────────────────────────
with open(INPUT, encoding='utf-8') as f:
    all_lines = f.readlines()

header = all_lines[0].rstrip('\n')
rows = []
for line in all_lines[1:]:
    stripped = line.rstrip('\n')
    if not stripped.strip():
        continue
    parts = stripped.split(';')
    if len(parts) == NCOLS:
        rows.append(parts)

stats = {
    'e2': 0, 'e1a': 0, 'e1b': 0,
    'del_1c': 0, 'del_3': 0, 'del_5': 0,
    'e4_old': 0, 'e4_mod': 0,
}
error2_applied = set()  # Índices donde se aplicó corrección de Error 2


# ═══════════════════════════════════════════════════════════════════════════════
# ERROR 2 – Enunciado='--', pregunta (y posibles opciones) en campo Opciones
# El campo Opciones empieza con "NNN. texto..." donde NNN es un número de pregunta.
# ═══════════════════════════════════════════════════════════════════════════════
for i, parts in enumerate(rows):
    if parts[3].strip() != '--':
        continue
    opciones = parts[4].strip()
    if not opciones or not re.match(r'^\d+\.', opciones):
        continue

    segments = [s.strip() for s in opciones.split(' | ')]
    first_seg = segments[0]
    rest_segs  = segments[1:]

    # Quitar el número de pregunta inicial ("125. " → "")
    enunciado_extraido = re.sub(r'^\d+\.\s*', '', first_seg).strip()

    if not rest_segs:
        # Sin opciones reales → todo el campo era el enunciado
        parts[3] = enunciado_extraido
        parts[4] = '--'
    else:
        # Comprobar si el primer segmento restante parece una opción numerada ("1. ")
        if re.match(r'^\d+\.', rest_segs[0]):
            parts[3] = enunciado_extraido
            parts[4] = ' | '.join(rest_segs)
        else:
            # No parecen opciones; conservar como están
            parts[3] = enunciado_extraido
            parts[4] = ' | '.join(rest_segs)

    error2_applied.add(i)
    stats['e2'] += 1


# ═══════════════════════════════════════════════════════════════════════════════
# ERROR 1A – RespuestaCorrecta termina en ",N"
# El texto antes de ",N" pertenece al Enunciado; N es la respuesta real.
# ═══════════════════════════════════════════════════════════════════════════════
for i, parts in enumerate(rows):
    resp = parts[5].strip()
    if resp in VALID_RESP:
        continue
    m = re.search(r',(\d)$', resp)
    if not m:
        continue

    actual  = m.group(1)
    prefijo = resp[:m.start()].strip()

    enunciado = parts[3].strip()
    if enunciado and enunciado != '--':
        parts[3] = f'{enunciado} {prefijo}'.strip()
    else:
        parts[3] = prefijo
    parts[5] = actual
    stats['e1a'] += 1


# ═══════════════════════════════════════════════════════════════════════════════
# ERROR 1B – Respuesta embebida en Opciones como "Respuesta: N"
# El texto en RespuestaCorrecta pertenece al Enunciado; N se extrae de Opciones.
# ═══════════════════════════════════════════════════════════════════════════════
for i, parts in enumerate(rows):
    resp = parts[5].strip()
    if resp in VALID_RESP:
        continue
    opciones = parts[4].strip()
    m = re.search(r'\.?\s*Respuesta:\s*(\d)', opciones)
    if not m:
        continue

    actual = m.group(1)

    # El valor actual de RespuestaCorrecta es texto desplazado → añadir al Enunciado
    enunciado = parts[3].strip()
    if enunciado and enunciado != '--':
        parts[3] = f'{enunciado} {resp}'.strip()
    else:
        parts[3] = resp

    # Limpiar Opciones: eliminar "Respuesta: N" y separadores sobrantes
    clean_opts = opciones[:m.start()].rstrip(' |.').strip()
    parts[4] = clean_opts
    parts[5] = actual
    stats['e1b'] += 1


# ═══════════════════════════════════════════════════════════════════════════════
# ELIMINACIONES – Errores 1C, 3 y 5
# ═══════════════════════════════════════════════════════════════════════════════
to_delete = set()

for i, parts in enumerate(rows):
    resp     = parts[5].strip()
    opciones = parts[4].strip()
    anulada  = parts[7].strip()

    # Error 1C: RespuestaCorrecta sigue siendo inválida tras todas las correcciones
    if resp not in VALID_RESP:
        to_delete.add(i)
        stats['del_1c'] += 1
        continue

    # Error 3: Opciones='--' y no es una pregunta explícitamente anulada/impugnada
    # (se excluyen las filas ya corregidas por Error 2, donde '--' en Opciones es intencional)
    if (opciones == '--'
            and resp not in ('ANULADA', 'IMPUGNADA')
            and anulada != 'SI'
            and i not in error2_applied):
        to_delete.add(i)
        stats['del_3'] += 1
        continue

    # Error 5: 6 o más opciones (campo Opciones con ≥5 separadores ' | ')
    if opciones != '--' and opciones.count(' | ') >= 5:
        to_delete.add(i)
        stats['del_5'] += 1
        continue

surviving = [parts for i, parts in enumerate(rows) if i not in to_delete]


# ═══════════════════════════════════════════════════════════════════════════════
# ERROR 4 – Identificadores duplicados
#
# Caso A – Años sin número de pregunta (ej. "MIR 2000-01"):
#   → añadir ", P1001", ", P1002", … para hacerlos únicos.
#
# Caso B – Años modernos con P# repetido (ej. "MIR 2025, P35" × 6):
#   → mantener la primera ocurrencia; añadir sufijo "b", "c", "d", … al resto.
# ═══════════════════════════════════════════════════════════════════════════════
ident_grupos = defaultdict(list)
for i, parts in enumerate(surviving):
    ident_grupos[parts[1].strip()].append(i)

for ident, indices in ident_grupos.items():
    if len(indices) == 1:
        continue  # Único, sin cambios

    tiene_pnum = bool(re.search(r',\s*P\d+', ident))

    if not tiene_pnum:
        # Caso A: formato antiguo sin P#
        for j, idx in enumerate(indices):
            surviving[idx][1] = f'{ident}, P{1001 + j}'
        stats['e4_old'] += len(indices)
    else:
        # Caso B: formato moderno con P# ya existente
        for j, idx in enumerate(indices[1:], start=1):
            sufijo = chr(ord('a') + j)   # 'b', 'c', 'd', …
            surviving[idx][1] = surviving[idx][1] + sufijo
        stats['e4_mod'] += len(indices) - 1


# ═══════════════════════════════════════════════════════════════════════════════
# ESCRITURA DEL ARCHIVO FINAL
# ═══════════════════════════════════════════════════════════════════════════════
with open(OUTPUT, 'w', encoding='utf-8') as f:
    f.write(header + '\n')
    for parts in surviving:
        f.write(';'.join(parts) + '\n')


# ═══════════════════════════════════════════════════════════════════════════════
# VERIFICACIÓN FINAL
# ═══════════════════════════════════════════════════════════════════════════════
with open(OUTPUT, encoding='utf-8') as f:
    out_lines = f.readlines()

out_rows = [l.rstrip('\n').split(';') for l in out_lines[1:] if l.strip()]

col_distribution = {}
for r in out_rows:
    n = len(r)
    col_distribution[n] = col_distribution.get(n, 0) + 1

bad_resp = [r for r in out_rows if r[5].strip() not in VALID_RESP]
bad_opts = [r for r in out_rows
            if r[4].strip() != '--' and r[4].strip().count(' | ') >= 5]

ident_counter = {}
for r in out_rows:
    k = r[1].strip()
    ident_counter[k] = ident_counter.get(k, 0) + 1
dupes_restantes = {k: v for k, v in ident_counter.items() if v > 1}

SEP = '─' * 60
print(SEP)
print('  RESUMEN DE CORRECCIONES')
print(SEP)
print(f'  Error 2  reparados (Enunciado vacío):           {stats["e2"]:>4}')
print(f'  Error 1A reparados (RespCorrecta con ",N"):      {stats["e1a"]:>4}')
print(f'  Error 1B reparados (Respuesta en Opciones):      {stats["e1b"]:>4}')
print()
print(f'  Eliminadas E1C (sin respuesta recuperable):      {stats["del_1c"]:>4}')
print(f'  Eliminadas E3  (sin opciones):                   {stats["del_3"]:>4}')
print(f'  Eliminadas E5  (6 o más opciones):               {stats["del_5"]:>4}')
print()
print(f'  Error 4 – IDs años sin P#   corregidos:          {stats["e4_old"]:>4}')
print(f'  Error 4 – IDs modernos duplicados corregidos:    {stats["e4_mod"]:>4}')
print(SEP)
print(f'  Filas de entrada:  {len(rows):>5}')
print(f'  Filas de salida:   {len(out_rows):>5}')
print(f'  Filas eliminadas:  {len(rows) - len(out_rows):>5}')
print(SEP)
print('  VERIFICACIÓN')
print(SEP)
print(f'  Distribución de columnas: {col_distribution}')
print(f'  Filas con RespCorrecta inválida:  {len(bad_resp)}')
print(f'  Filas con 6+ opciones:            {len(bad_opts)}')
print(f'  Identificadores aún duplicados:   {len(dupes_restantes)}')
if dupes_restantes:
    print('  Duplicados restantes (primeros 10):')
    for k, v in list(dupes_restantes.items())[:10]:
        print(f'    "{k}" → {v} veces')
print(SEP)
