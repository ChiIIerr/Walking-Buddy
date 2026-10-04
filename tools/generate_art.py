"""Original Walking Buddy pixel art. Requires Pillow. All coordinates authored here.
No third-party images, fonts, or downloaded sprites are used.
"""
from PIL import Image, ImageDraw
from pathlib import Path
import random
ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'
ASSETS.mkdir(parents=True, exist_ok=True)
OUTLINE = '#304444'
PALETTES = [('#E8A656','#FBE6C1','#C78246'), ('#ECAA74','#FFE6CE','#D28450'),
            ('#E48752','#FFF0D3','#B95939'), ('#DED8EF','#FFF1ED','#B8A9D4'),
            ('#91C69A','#E7F2C3','#5D9B7C'), ('#F3ECE0','#FFFFFF','#425A59'),
            ('#547C91','#F2F1D9','#355567'), ('#97B7DF','#E3EAF7','#658CBD')]
NAMES = ['milo','maple','rusty','mochi','fern','bamboo','pip','nova']
def sprite(kind, frame):
    im = Image.new('RGBA',(64,64)); d = ImageDraw.Draw(im)
    main, cream, shade = PALETTES[kind]
    y = -1 if frame == 1 else 0
    def poly(points, fill, outline=OUTLINE):
        pts=[(x,yy+y) for x,yy in points]; d.polygon(pts,fill=fill)
        if outline: d.line(pts+[pts[0]],fill=outline,width=2)
    def rect(box, fill): d.rectangle((box[0],box[1]+y,box[2],box[3]+y),fill)
    if kind in [0,1,2]:
        poly([(44,43),(51,40),(51,34),(55,34),(58,38),(56,46),(49,49)],shade)
        if kind==2: rect((52,35,55,40),cream)
    if kind==7:
        poly([(42,33),(52,26),(54,34),(58,36),(49,42)],shade)
        poly([(15,33),(8,26),(6,34),(3,36),(14,42)],shade)
    poly([(21,33),(42,33),(46,41),(46,52),(42,56),(20,56),(17,52),(17,42)],main)
    poly([(25,42),(38,42),(40,51),(23,51)],cream,None)
    rect((20,52,27,56),shade); rect((36,52,43,56),shade);rect((27,54,35,56),OUTLINE)
    if kind==3:
        poly([(17,21),(14,8),(17,3),(22,4),(26,21)],main)
        poly([(36,21),(38,4),(43,3),(47,8),(45,24)],main)
        rect((18,7,21,17),'#E8AFB6'); rect((40,7,43,17),'#E8AFB6')
    elif kind==4:
        poly([(16,24),(16,15),(21,12),(26,15),(27,23)],main)
        poly([(35,23),(36,15),(42,12),(47,15),(47,25)],main)
    elif kind==5:
        poly([(15,23),(12,17),(15,12),(21,12),(26,21)],shade)
        poly([(38,21),(42,12),(48,12),(51,17),(48,24)],shade)
    elif kind==6: poly([(19,22),(22,13),(29,10),(38,12),(45,21)],main)
    elif kind==1:
        poly([(17,25),(14,9),(22,12),(29,24)],main)
        poly([(35,24),(42,12),(50,9),(47,26)],main)
        poly([(18,13),(22,16),(24,24)],cream,None);poly([(44,15),(47,13),(45,25)],cream,None)
    elif kind==7:
        poly([(19,21),(17,12),(23,15),(27,22)],shade);poly([(36,22),(42,13),(47,12),(45,23)],shade)
    else:
        poly([(15,25),(16,11),(21,13),(28,24)],main);poly([(35,24),(43,11),(47,13),(49,27)],main)
        rect((18,16,21,22),'#D57C73'); rect((43,16,46,22),'#D57C73')
    poly([(19,20),(43,20),(48,25),(51,31),(50,40),(44,46),(19,46),(13,40),(12,31),(15,25)],main)
    if kind in [1,2,6]: poly([(14,34),(23,31),(31,34),(40,31),(49,34),(46,42),(41,45),(20,45),(15,40)],cream,None)
    if kind==0:
        rect((27,21,29,26),shade);rect((34,21,36,26),shade)
        rect((14,31,18,33),shade);rect((46,31,49,33),shade)
    if kind==5:
        poly([(17,29),(24,26),(28,30),(27,37),(20,39),(16,35)],shade,None)
        poly([(37,29),(43,26),(48,30),(47,37),(40,39),(36,35)],shade,None)
    if kind==7: rect((30,16,33,20),'#F7D277');rect((28,20,34,24),'#F7D277')
    for x in [22,40]:
        if frame==3: rect((x-1,32,x+3,33),OUTLINE)
        else: rect((x,29,x+3,34),OUTLINE);rect((x,29,x+1,30),'#FFFFFF')
    rect((17,35,21,37),'#EBA09D');rect((42,35,46,37),'#EBA09D')
    if kind==6: poly([(29,34),(35,34),(33,38),(31,38)],'#EAA758',None)
    else:
        rect((30,34,33,35),OUTLINE);rect((31,36,32,38),OUTLINE)
        rect((28,38,30,39),OUTLINE);rect((33,38,35,39),OUTLINE)
    rect((23,45,40,47),'#3B927F');rect((27,47,34,50),'#3B927F');rect((30,47,32,49),'#F3C977')
    return im
portraits=[]
for k,name in enumerate(NAMES):
    sheet=Image.new('RGBA',(256,64))
    for f in range(4): sheet.paste(sprite(k,f),(f*64,0))
    sheet.resize((1024,256),Image.Resampling.NEAREST).save(ASSETS/f'{name}.png')
    portraits.append(sprite(k,0))
def landscape(theme):
    random.seed(17)
    sky,far,near,grass,light=(['#E0EFE5','#AFCDBC','#88B89C','#95C594','#B7D5A5'] if theme==0 else
         ['#F2DECD','#DABDB1','#BBAAA2','#ACBC93','#CED2AA'] if theme==1 else
         ['#D7E0EF','#ACBDD5','#879EB9','#9AB9AA','#BDCEBD'])
    im=Image.new('RGB',(192,128),sky);d=ImageDraw.Draw(im)
    d.rectangle((0,67,192,128),grass);d.ellipse((137,10,156,29),fill='#FAEBC0')
    for x,y in [(12,16),(63,9),(105,29)]:
        d.rectangle((x,y+3,x+24,y+8),'#F9FAEF');d.rectangle((x+4,y,x+18,y+10),'#F9FAEF')
    d.polygon([(0,69),(0,51),(29,28),(61,57),(94,30),(143,64),(169,46),(192,63),(192,80)],far)
    d.polygon([(0,66),(22,51),(52,71),(86,54),(124,73),(155,57),(192,72),(192,89),(0,89)],near)
    d.polygon([(107,73),(114,73),(107,85),(117,94),(147,109),(181,128),(58,128),(86,109),(99,98),(95,88)],'#EADDAC')
    d.polygon([(109,74),(111,74),(103,87),(114,96),(145,112),(159,128),(131,128),(113,109),(102,99),(99,87)],'#F0E6BF')
    d.rectangle((17,52,49,76),'#EDD7A5');d.rectangle((19,71,49,76),'#D3B584')
    d.polygon([(12,54),(33,37),(54,54)],'#8B675C');d.polygon([(18,49),(32,39),(48,51)],'#AF7F63')
    d.rectangle((23,57,30,64),'#688E8E');d.rectangle((38,58,44,76),'#977966')
    d.rectangle((26,57,27,64),'#EFE6C4');d.rectangle((23,60,30,61),'#EFE6C4')
    d.rectangle((40,68,41,69),'#ECDD99');d.rectangle((43,41,46,48),'#856C63')
    for x in range(6,62,6):
        d.rectangle((x,76,x+2,85),'#F2E6C3');d.rectangle((x,79,x+5,80),'#F2E6C3')
    for x,y in [(164,54),(6,56),(148,74)]:
        d.rectangle((x+7,y+6,x+10,y+30),'#9D8364');d.rectangle((x,y-1,x+18,y+15),'#5E9278')
        d.rectangle((x+3,y-7,x+15,y+21),'#5E9278');d.rectangle((x+2,y,x+14,y+9),'#78A484')
        d.rectangle((x+5,y-4,x+12,y+4),'#91B792');d.rectangle((x+4,y+10,x+6,y+12),'#D99473')
        d.rectangle((x+12,y+3,x+14,y+5),'#D99473')
    for j in range(80):
        x=random.randrange(192);y=random.randrange(86,128)
        if 75<x<170: continue
        d.rectangle((x,y,x+1,y+1),random.choice(['#78AA85',light,'#EEDFAD']))
        if j%9==0: d.rectangle((x,y-1,x+2,y),'#F7ECBF');d.point((x+1,y),'#D4AF6A')
    return im
for theme,name in enumerate(['meadow','sunset','alpine']):
    landscape(theme).resize((768,512),Image.Resampling.NEAREST).save(ASSETS/f'{name}.png')
icon=Image.new('RGBA',(432,432));cat=portraits[0].resize((288,288),Image.Resampling.NEAREST);icon.paste(cat,(72,60),cat)
icon.save(ROOT/'app/src/main/res/drawable-nodpi/icon_foreground.png')
preview=Image.new('RGB',(768,448),'#FAF8F3');d=ImageDraw.Draw(preview)
for k,pet in enumerate(portraits):
    x=(k%4)*192;y=(k//4)*224;p=pet.resize((192,192),Image.Resampling.NEAREST);preview.paste(p,(x,y),p)
    d.text((x+76,y+196),NAMES[k].upper(),fill=OUTLINE)
preview.save(ROOT/'art/buddies.png')
print('Created 8 animated sprite sheets, 3 landscapes, launcher art, and contact sheet.')
