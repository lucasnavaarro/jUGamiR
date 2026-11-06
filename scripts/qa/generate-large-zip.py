"""Create a valid near-limit PNG ZIP using only the Python standard library."""
import json
import os
from pathlib import Path
import struct
import sys
import zipfile
import zlib

target = Path(sys.argv[1])
target.parent.mkdir(parents=True, exist_ok=True)
width, height = 2048, 1024
pixels = b"".join(b"\0" + os.urandom(width * 3) for _ in range(height))

def chunk(kind, data):
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
png += chunk(b"IDAT", zlib.compress(pixels, 1)) + chunk(b"IEND", b"")
with zipfile.ZipFile(target, "w", compression=zipfile.ZIP_STORED) as archive:
    for index in range(32):
        archive.writestr(f"imagenes/large-budget/image-{index:02}.png", png)
assert len(png) < 10 * 1024 * 1024 and target.stat().st_size < 200 * 1024 * 1024
print(json.dumps({"imageBytes": len(png), "archiveBytes": target.stat().st_size, "entries": 32}))
