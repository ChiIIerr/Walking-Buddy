package com.walkingbuddy.app;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class WeekChart extends View {
    private final GameState s; private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    public WeekChart(Context c, GameState s) {
        super(c);this.s=s;
        StringBuilder description=new StringBuilder("Steps over the last seven days. ");
        for(int i=6;i>=0;i--){LocalDate day=s.day.minusDays(i);GameState.Day d=s.history.get(day);description.append(day).append(": ").append(i==0?s.steps():d==null?0:d.steps()).append(" steps. ");}
        setContentDescription(description);setFocusable(true);
    }
    @Override protected void onDraw(Canvas c) {
        float density=getResources().getDisplayMetrics().density;
        float w=getWidth(),h=getHeight(),base=h-28*density,top=24*density, usable=base-top;
        int max=s.goal;for(GameState.Day d:s.history.values())max=Math.max(max,d.steps());max=Math.max(max,s.steps());
        // Only scale against the visible week, so historic records don't flatten this chart.
        max=s.goal;for(int i=6;i>=0;i--){GameState.Day d=s.history.get(s.day.minusDays(i));if(d!=null)max=Math.max(max,d.steps());}max=Math.max(max,s.steps());
        float goalY=base-usable*s.goal/(float)max;
        p.setColor(Color.parseColor("#D9DFD5"));p.setStrokeWidth(density);p.setPathEffect(new DashPathEffect(new float[]{4*density,4*density},0));
        c.drawLine(0,goalY,w,goalY,p);p.setPathEffect(null);
        p.setTextSize(10*getResources().getDisplayMetrics().scaledDensity);p.setColor(Color.parseColor("#74857A"));
        c.drawText("GOAL",0,Math.max(12*density,goalY-5*density),p);
        float step=w/7;
        for(int i=0;i<7;i++){
            LocalDate day=s.day.minusDays(6-i);GameState.Day d=s.history.get(day);int count=i==6?s.steps():d==null?0:d.steps();
            float bh=Math.max(3*density,usable*count/max);float x=i*step+step*.22f;
            p.setColor(Color.parseColor(i==6?"#287D67":count>=s.goal?"#8FBBA2":"#D6E5D5"));
            c.drawRoundRect(x,base-bh,x+step*.56f,base,4*density,4*density,p);
            p.setColor(Color.parseColor(i==6?"#24473D":"#74857A"));p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(11*getResources().getDisplayMetrics().scaledDensity);
            c.drawText(day.format(DateTimeFormatter.ofPattern("EE",Locale.US)).substring(0,1),i*step+step/2,base+19*density,p);
            p.setTextAlign(Paint.Align.LEFT);
        }
    }
}
