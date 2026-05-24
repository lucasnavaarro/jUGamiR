#!/usr/bin/env python3
"""
Script de corrección para preguntas_mir.csv (VASCULAR, 103 filas).

Problemas detectados y acciones:
  A) 89+ filas: "RESPUESTA: N" dentro de Opciones con RespuestaCorrecta='--'
     → Extrae N, limpia Opciones.
  B) 4 filas: RespuestaCorrecta contiene texto (":  / "sobre la cirugía...")
     → Mueve ese texto al Enunciado; luego aplica (A).
  C) 5 filas: Opciones='--' con pregunta+opciones+RESPUESTA todo en Enunciado
     → Separa Enunciado real, reconstruye Opciones, extrae respuesta.
  D) 2 filas: 6+ opciones por texto extra en Opciones → eliminar.
  E) 1 identificador duplicado ('MIR 2014-2015, P228') → eliminar la segunda.
"""

import re

INPUT  = '/home/lucas/Escritorio/TFG/jUGamiR/preguntas_mir.csv'
OUTPUT = '/home/lucas/Escritorio/TFG/jUGamiR/preguntas_mir.csv'

VALID_RESP = {'1', '2', '3', '4', '5', 'ANULADA', 'IMPUGNADA', '--'}

# Patrón para localizar la respuesta embebida en el texto
RESP_PAT = re.compile(
    r'[.,]?\s*RESPUESTA:\s*(ANULADA|IMPUGNADA|\d+)\.?',
    re.IGNORECASE
)


def extraer_de_opciones(opciones: str) -> tuple[str, str]:
    """
    Si Opciones contiene 'RESPUESTA: N', devuelve (opciones_limpias, respuesta).
    Si no, devuelve (opciones_original, '--').
    """
    m = RESP_PAT.search(opciones)
    if not m:
        return opciones, '--'
    respuesta  = m.group(1).upper()
    clean_opts = opciones[:m.start()].rstrip(' |.').strip()
    return clean_opts, respuesta


def separar_enunciado_opciones(enunciado: str) -> tuple[str, str, str]:
    """
    Para filas donde Opciones='--' y el Enunciado contiene la pregunta,
    las opciones y 'RESPUESTA: N', separa los tres componentes.

    Retorna (enunciado_limpio, opciones_formateadas, respuesta).
    """
    # 1. Extraer RESPUESTA del final
    m_resp = RESP_PAT.search(enunciado)
    if m_resp:
        respuesta = m_resp.group(1).upper()
        texto     = enunciado[:m_resp.start()].strip().rstrip('.')
    else:
        respuesta = '--'
        texto     = enunciado

    # 2. Encontrar dónde empieza la primera opción ("1 ." ó "1.")
    m_start = re.search(r'(?<=[?:.])\s+1\s*\.\s+', texto)
    if not m_start:
        # No se encuentran opciones → devolver tal cual
        return texto, '--', respuesta

    pregunta_real = texto[:m_start.start()].strip()
    opts_raw      = texto[m_start.start():].strip()

    # 3. Parsear las opciones individuales
    # El separador entre opciones es ". N . " (ej. ". 2 . ", ". 3 . " …)
    partes = re.split(r'\.\s+(?=\d+\s*\.\s+)', opts_raw)

    opciones_lista = []
    for i, parte in enumerate(partes, start=1):
        # Quitar el número inicial si lo lleva (ej. "1 . Texto" → "Texto")
        texto_opcion = re.sub(r'^\d+\s*\.\s*', '', parte.strip()).strip()
        if texto_opcion:
            opciones_lista.append(f"{i}. {texto_opcion}.")

    opciones_formateadas = ' | '.join(opciones_lista) if opciones_lista else '--'
    return pregunta_real, opciones_formateadas, respuesta


# ── Carga ─────────────────────────────────────────────────────────────────────
with open(INPUT, encoding='utf-8') as f:
    lines = f.readlines()

header_str = lines[0].rstrip('\n')
header     = [h.strip() for h in header_str.split(';')]
rows       = [l.rstrip('\n').split(';') for l in lines[1:] if l.strip()]
idx        = {h: i for i, h in enumerate(header)}
NCOLS      = len(header)

stats = {'B': 0, 'C': 0, 'A': 0, 'D': 0, 'E': 0}
to_delete = set()

# ── B) RespuestaCorrecta inválida: mover texto al Enunciado ─────────────────
for i, r in enumerate(rows):
    resp = r[idx['RespuestaCorrecta']].strip()
    if resp not in VALID_RESP:
        prefijo = resp.strip()
        enunciado = r[idx['Enunciado']].strip()
        r[idx['Enunciado']] = f"{enunciado} {prefijo}".strip()
        r[idx['RespuestaCorrecta']] = '--'
        stats['B'] += 1

# ── C) Opciones='--' con pregunta+opciones en Enunciado ─────────────────────
for i, r in enumerate(rows):
    if r[idx['Opciones']].strip() != '--':
        continue
    enunciado = r[idx['Enunciado']].strip()
    m = RESP_PAT.search(enunciado)
    if not m:
        continue

    nueva_pregunta, nuevas_opts, respuesta = separar_enunciado_opciones(enunciado)
    r[idx['Enunciado']]          = nueva_pregunta
    r[idx['Opciones']]           = nuevas_opts
    r[idx['RespuestaCorrecta']]  = respuesta
    stats['C'] += 1

# ── A) Extraer RESPUESTA de Opciones para todos los casos pendientes ─────────
for i, r in enumerate(rows):
    opts = r[idx['Opciones']].strip()
    if opts == '--':
        continue
    resp = r[idx['RespuestaCorrecta']].strip()
    if resp in VALID_RESP and resp != '--':
        continue  # Ya tiene respuesta válida
    m = RESP_PAT.search(opts)
    if not m:
        continue
    clean_opts, respuesta = extraer_de_opciones(opts)
    r[idx['Opciones']]          = clean_opts
    r[idx['RespuestaCorrecta']] = respuesta
    stats['A'] += 1

# ── D) Eliminar filas con 6+ opciones ───────────────────────────────────────
for i, r in enumerate(rows):
    opts = r[idx['Opciones']].strip()
    if opts != '--' and opts.count(' | ') >= 5:
        to_delete.add(i)
        stats['D'] += 1

# ── E) Eliminar identificadores duplicados (conservar el primero) ─────────────
seen_ids = set()
for i, r in enumerate(rows):
    ident = r[idx['Identificador']].strip()
    if ident in seen_ids:
        to_delete.add(i)
        stats['E'] += 1
    else:
        seen_ids.add(ident)

# ── Filtrar filas eliminadas ─────────────────────────────────────────────────
surviving = [r for i, r in enumerate(rows) if i not in to_delete]

# ── Verificación ─────────────────────────────────────────────────────────────
bad_resp = [r for r in surviving if r[idx['RespuestaCorrecta']].strip() not in VALID_RESP]
bad_opts = [r for r in surviving
            if r[idx['Opciones']].strip() != '--'
            and r[idx['Opciones']].strip().count(' | ') >= 5]
dupe_ids = {}
for r in surviving:
    k = r[idx['Identificador']].strip()
    dupe_ids[k] = dupe_ids.get(k, 0) + 1
remaining_dupes = {k: v for k, v in dupe_ids.items() if v > 1}
blank_resp = [r for r in surviving if r[idx['RespuestaCorrecta']].strip() == '--']

# ── Escritura ────────────────────────────────────────────────────────────────
with open(OUTPUT, 'w', encoding='utf-8') as f:
    f.write(header_str + '\n')
    for r in surviving:
        f.write(';'.join(r) + '\n')

# ── Resumen ──────────────────────────────────────────────────────────────────
SEP = '─' * 55
print(SEP)
print('  CORRECCIONES APLICADAS')
print(SEP)
print(f'  B) RespCorrecta inválida → texto movido al Enunciado: {stats["B"]}')
print(f'  C) Opciones="--" → separadas desde Enunciado:          {stats["C"]}')
print(f'  A) RESPUESTA extraída de Opciones:                     {stats["A"]}')
print(f'  D) Filas con 6+ opciones eliminadas:                   {stats["D"]}')
print(f'  E) Identificadores duplicados eliminados:              {stats["E"]}')
print(SEP)
print(f'  Filas de entrada:  {len(rows)}')
print(f'  Filas de salida:   {len(surviving)}')
print(SEP)
print('  VERIFICACIÓN FINAL')
print(SEP)
print(f'  RespuestaCorrecta inválida:    {len(bad_resp)}')
print(f'  Filas con 6+ opciones:         {len(bad_opts)}')
print(f'  Identificadores duplicados:    {len(remaining_dupes)}')
print(f'  RespuestaCorrecta="--":        {len(blank_resp)}')
if blank_resp:
    print('  (Filas sin respuesta):')
    for r in blank_resp:
        print(f'    [{r[idx["Identificador"]]}] {r[idx["Enunciado"]][:60]}')
if bad_resp:
    print('  Respuestas inválidas restantes:')
    for r in bad_resp:
        print(f'    [{r[idx["Identificador"]]}] Resp={r[idx["RespuestaCorrecta"]]}')
print(SEP)
