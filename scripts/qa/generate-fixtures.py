from pathlib import Path
from PIL import Image, PngImagePlugin
import io, json, struct, zlib, zipfile, stat

out=Path('/private/tmp/jugamir-fullqa-fixtures');out.mkdir(exist_ok=True)
image=Image.new('RGB',(32,24),(20,120,210));image.save(out/'valid.png')
exif=Image.Exif();exif[274]=6;exif[315]='QA test author';image.save(out/'oriented.jpg',exif=exif)
Image.new('RGBA',(32,24),(255,0,0,100)).save(out/'alpha.png')
(out/'invalid.png').write_text('<script>window.QA_XSS=1</script>')
(out/'truncated.png').write_bytes((out/'valid.png').read_bytes()[:25])
image.save(out/'unsupported.gif')
for label,w,h in [('pixels-below',3999,4000),('pixels-at',4000,4000),('pixels-above',4001,4000)]:
 Image.new('RGB',(w,h),(10,20,30)).save(out/(label+'.png'))
base=(out/'valid.png').read_bytes()
for label,target in [('bytes-below',10*1024*1024-1),('bytes-at',10*1024*1024),('bytes-above',10*1024*1024+1),('multipart-2m',2*1024*1024)]:
 payload=b'QA\0'+b'A'*(target-len(base)-12-3)
 chunk=struct.pack('>I',len(payload))+b'tEXt'+payload+struct.pack('>I',zlib.crc32(b'tEXt'+payload)&0xffffffff)
 (out/(label+'.png')).write_bytes(base[:-12]+chunk+base[-12:])
def archive(name,entries):
 with zipfile.ZipFile(out/name,'w',compression=zipfile.ZIP_DEFLATED) as z:
  for n,data in entries:z.writestr(n,data)
archive('valid.zip',[('imagenes/qa-zip/a.png',base),('imagenes/qa-zip/b.jpg',(out/'oriented.jpg').read_bytes())])
archive('traversal.zip',[('imagenes/../../escape.png',base)])
archive('partial-invalid.zip',[('imagenes/qa-partial/good.png',base),('imagenes/qa-partial/bad.png',b'invalid')])
archive('entries-500.zip',[(f'imagenes/qa-500/{n}.png',base) for n in range(500)])
archive('entries-501.zip',[(f'imagenes/qa-501/{n}.png',base) for n in range(501)])
with zipfile.ZipFile(out/'symlink.zip','w') as z:
 info=zipfile.ZipInfo('imagenes/qa-link.png');info.create_system=3;info.external_attr=(stat.S_IFLNK|0o777)<<16;z.writestr(info,base)
archive('high-expansion.zip',[(f'imagenes/qa-expand/{n}.png',(out/'bytes-at.png').read_bytes()) for n in range(21)])
(out/'fixtures.json').write_text(json.dumps({p.name:p.stat().st_size for p in out.iterdir() if p.is_file()},indent=2))
print(out)
