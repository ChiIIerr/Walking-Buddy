package com.walkingbuddy.app;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Nearest-neighbor pixel art with lifecycle-aware idle/blink animation. */
public final class PixelView extends View {
    private static final Map<String, Bitmap> CACHE = new HashMap<>();
    private final Paint p = new Paint();
    private int buddy, theme;
    private boolean scene, animate = true, resting;
    private long celebrateUntil;
    private static final String[] SCENES={"meadow","sunset","alpine"};
    private final Rect source=new Rect();
    private final RectF destination=new RectF();
    public PixelView(Context c) { this(c,0,0,false); }
    public PixelView(Context c, int buddy, int theme, boolean scene) {
        super(c); this.buddy = buddy; this.theme = theme; this.scene = scene;
        p.setFilterBitmap(false); setContentDescription(GameState.SPECIES[buddy] + (scene ? " in a pixel meadow" : " pixel portrait"));
    }
    public void setBuddy(int b, int t, boolean reduced, boolean dead) {
        buddy = b; theme = t; animate = !reduced; resting = dead;
        setContentDescription(GameState.SPECIES[buddy] + (resting ? ", resting" : ", happy")); invalidate();
    }
    public void celebrate() { celebrateUntil = SystemClock.uptimeMillis() + 2200; invalidate(); }
    private Bitmap asset(String name) {
        if (!CACHE.containsKey(name)) {
            try (java.io.InputStream in = getContext().getAssets().open(name + ".png")) { CACHE.put(name, BitmapFactory.decodeStream(in)); }
            catch (IOException e) { throw new IllegalStateException("Missing bundled artwork: " + name, e); }
        }
        return CACHE.get(name);
    }
    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(); long time = SystemClock.uptimeMillis();
        p.setAntiAlias(false); p.setAlpha(255);
        if (scene) {
            Bitmap bg = asset(SCENES[theme]);
            // Crop instead of stretching the original pixel scene.
            float ratio = w / h; int sh = bg.getHeight(), sw = (int)(sh * ratio);
            if (sw > bg.getWidth()) { sw = bg.getWidth(); sh = (int)(sw / ratio); }
            source.set((bg.getWidth()-sw)/2, (bg.getHeight()-sh)/2, (bg.getWidth()+sw)/2, (bg.getHeight()+sh)/2);
            destination.set(0,0,w,h);c.drawBitmap(bg, source, destination, p);
        }
        Bitmap sheet = asset(GameState.IDS[buddy]);
        int f = animate && !resting ? (int)((time / 400) % 12) : 0;
        f = f == 11 ? 3 : f % 3;
        float size = scene ? Math.min(w*.48f, h*.68f) : Math.min(w,h);
        float x = (w-size)/2, y = scene ? h*.30f : (h-size)/2;
        if (scene) {
            p.setColor(Color.parseColor("#6A956E")); p.setAlpha(95);
            destination.set(w/2-size*.26f, y+size*.83f, w/2+size*.27f,y+size*.94f);c.drawOval(destination,p);p.setAlpha(255);
        }
        if (resting) p.setAlpha(100);
        source.set(f*256,0,(f+1)*256,256);destination.set(x,y,x+size,y+size);c.drawBitmap(sheet, source, destination,p);p.setAlpha(255);
        if (scene && celebrateUntil > time && animate) {
            p.setColor(Color.parseColor("#FFF4C1"));
            for (int i=0;i<7;i++) {
                float sx = w*.27f + i*w*.075f;
                float sy = h*.48f - ((time+i*180)%1000)/1000f*h*.30f;
                c.drawRect(sx-3,sy,sx+6,sy+3,p);c.drawRect(sx,sy-3,sx+3,sy+6,p);
            }
        }
        if (animate && isShown() && !resting) postInvalidateDelayed(150);
    }
    @Override protected void onDetachedFromWindow() { super.onDetachedFromWindow(); }
}
