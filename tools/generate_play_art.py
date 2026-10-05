"""Render Play artwork from original in-repo sprites. Requires Pillow.
Uses the free DejaVu Sans font if available; set WB_ART_FONT for another font.
No font binary is redistributed. Output art is CC0, as documented in art/LICENSE.md.
"""
from pathlib import Path
import os
from PIL import Image, ImageDraw, ImageFont
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/play'
OUT.mkdir(parents=True, exist_ok=True)
ASSETS = ROOT / 'app/src/main/assets'
FONT = os.environ.get('WB_ART_FONT', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf')
if not Path(FONT).exists():
    FONT = '/System/Library/Fonts/Avenir Next.ttc'
def pet(name, size):
    with Image.open(ASSETS / (name+'.png')) as sheet:
        return sheet.crop((0, 0, 256, 256)).resize((size, size), Image.Resampling.NEAREST)
icon = Image.new('RGB', (512,512), '#E9F1E7')
icon.paste(pet('rusty',448), (32,32), pet('rusty',448))
icon.save(OUT/'icon-512.png')
im = Image.new('RGB',(1024,500),'#FAF8F3')
d = ImageDraw.Draw(im)
d.rectangle((540,0,1024,500),fill='#E9F1E7')
# Original pixel scenery, expanded by a whole-number scale.
with Image.open(ASSETS/'meadow.png') as source:
    scene = source.resize((768,512),Image.Resampling.NEAREST)
    im.paste(scene.crop((128,0,612,500)),(540,0))
d = ImageDraw.Draw(im)
ink='#253A35'; green='#287D67'
d.text((54,72),'EVERY WALK, A LITTLE JOY',font=ImageFont.truetype(FONT,19),fill=green)
d.text((50,126),'Walking',font=ImageFont.truetype(FONT,70),fill=ink)
d.text((50,204),'Buddy',font=ImageFont.truetype(FONT,70),fill=ink)
d.text((54,328),'Small steps. Pixel friends.',font=ImageFont.truetype(FONT,27),fill=green)
im.paste(pet('milo',192),(546,274),pet('milo',192))
im.paste(pet('mochi',192),(824,272),pet('mochi',192))
im.paste(pet('rusty',288),(630,155),pet('rusty',288))
im.save(OUT/'feature-1024x500.png')
print('Created opaque RGB Play icon and feature graphic.')
