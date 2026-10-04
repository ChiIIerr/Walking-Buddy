package com.walkingbuddy.app;
import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Original vector UI icons, authored as small native Canvas paths. */
public final class IconView extends View {
    private final String icon; private final int color; private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    public IconView(Context c, String icon, int color) { super(c); this.icon=icon; this.color=color; setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    private void line(Canvas c, float... points) {
        Path a=new Path();a.moveTo(points[0],points[1]);for(int i=2;i<points.length;i+=2)a.lineTo(points[i],points[i+1]);c.drawPath(a,p);
    }
    @Override protected void onDraw(Canvas c) {
        c.save(); float size=Math.min(getWidth(),getHeight());c.translate((getWidth()-size)/2,(getHeight()-size)/2);c.scale(size/24,size/24);
        p.setColor(color);p.setStrokeWidth(1.8f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        switch(icon) {
            case "leaf":
                Path leaf=new Path();leaf.moveTo(5,17);leaf.cubicTo(2,6,13,4,21,3);leaf.cubicTo(21,14,16,22,7,18);c.drawPath(leaf,p);line(c,4,21,15,10);break;
            case "home":line(c,3,10,12,3,21,10);line(c,5,9,5,21,10,21,10,15,14,15,14,21,19,21,19,9);break;
            case "paw":
                c.drawOval(6,12,18,21,p);c.drawOval(3,7,7,12,p);c.drawOval(8,3,12,9,p);c.drawOval(14,3,18,9,p);c.drawOval(19,7,23,12,p);break;
            case "chart":line(c,4,3,4,21,22,21);line(c,8,17,8,13);line(c,13,17,13,9);line(c,18,17,18,5);break;
            case "settings":
                c.drawCircle(12,12,7,p);c.drawCircle(12,12,2.5f,p);
                for(int i=0;i<8;i++){c.save();c.rotate(i*45,12,12);line(c,12,2,12,5);c.restore();}break;
            case "heart":
                Path heart=new Path();heart.moveTo(12,21);heart.cubicTo(8,17,2,13,2,8);heart.cubicTo(2,2,9,1,12,6);heart.cubicTo(15,1,22,2,22,8);heart.cubicTo(22,13,16,17,12,21);c.drawPath(heart,p);break;
            case "food":line(c,3,12,5,20,19,20,21,12,3,12);line(c,7,8,7,6);line(c,12,8,12,4);line(c,17,8,17,6);break;
            case "water":
                Path drop=new Path();drop.moveTo(12,2);drop.cubicTo(11,6,5,11,5,15);drop.cubicTo(5,24,19,24,19,15);drop.cubicTo(19,11,13,6,12,2);c.drawPath(drop,p);line(c,9,15,9,17,11,18);break;
            case "spark":line(c,12,3,14,9,20,12,14,14,12,21,10,14,4,12,10,9,12,3);line(c,21,3,21,7);line(c,19,5,23,5);break;
            case "foot":
                c.drawOval(4,9,10,19,p);c.drawOval(13,6,19,16,p);
                c.drawCircle(6,5,1.5f,p);c.drawCircle(17,2,1.5f,p);break;
            case "fire":
                Path fire=new Path();fire.moveTo(12,2);fire.cubicTo(11,10,6,7,5,14);fire.cubicTo(3,23,21,24,20,14);fire.cubicTo(20,10,17,7,16,5);fire.lineTo(14,12);fire.close();c.drawPath(fire,p);break;
            case "trophy":line(c,7,3,17,3,17,12,15,15,9,15,7,12,7,3);line(c,7,5,3,5,3,10,7,12);line(c,17,5,21,5,21,10,17,12);line(c,12,15,12,21);line(c,8,21,16,21);break;
            case "arrow":line(c,4,12,20,12);line(c,14,6,20,12,14,18);break;
            case "back":line(c,20,12,4,12);line(c,10,6,4,12,10,18);break;
            case "plus":line(c,12,5,12,19);line(c,5,12,19,12);break;
            case "check":line(c,4,12,9,17,20,6);break;
            case "lock":c.drawRoundRect(5,10,19,21,2,2,p);c.drawArc(8,2,16,14,180,180,false,p);c.drawCircle(12,15,1,p);break;
            case "map":line(c,3,5,9,3,15,5,21,3,21,19,15,21,9,19,3,21,3,5);line(c,9,3,9,19);line(c,15,5,15,21);break;
            case "moon":c.drawCircle(12,12,8,p);line(c,9,9,9,11);line(c,15,9,15,11);c.drawArc(8,11,16,17,0,180,false,p);break;
            default:c.drawCircle(12,12,8,p);
        }
        c.restore();
    }
}
