package com.walkingbuddy.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

final class WeekChart extends View {
    private static final int GOAL_MARKER_COLOR = 0xff536c5e;
    private static final int GOAL_REACHED_COLOR = 0xff287d67;
    private static final int WALK_COLOR = 0xffb6cbb6;

    private final WeekProgress week;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String[] dayLabels = new String[WeekProgress.DAYS];

    WeekChart(Context context, WeekProgress week) {
        super(context);
        this.week = week;
        Locale locale = Locale.getDefault();
        DateTimeFormatter dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale);
        NumberFormat numbers = NumberFormat.getIntegerInstance(locale);
        StringBuilder description = new StringBuilder("Steps over the last seven days. ");
        for (int i = 0; i < week.days.size(); i++) {
            WeekProgress.Entry entry = week.days.get(i);
            dayLabels[i] = numbers.format(entry.date.getDayOfMonth());
            description.append(entry.date.format(dateFormat)).append(". ");
            GameState.Day progress = entry.progress;
            if (progress == null) {
                description.append("No record. ");
            } else {
                description.append(numbers.format(progress.steps())).append(" steps, goal ")
                        .append(numbers.format(progress.goal)).append(". ")
                        .append(progress.hit() ? "Goal reached. " : "Goal not reached. ");
            }
        }
        setContentDescription(description);
        setFocusable(true);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float baseline = getHeight() - 30 * density;
        float chartHeight = baseline - 12 * density;
        if (chartHeight <= 0 || getWidth() == 0) return;

        float columnWidth = getWidth() / (float) week.days.size();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Math.min(12 * getResources().getDisplayMetrics().scaledDensity,
                columnWidth * 0.55f));
        paint.setStrokeWidth(2 * density);

        for (int i = 0; i < week.days.size(); i++) {
            GameState.Day progress = week.days.get(i).progress;
            float center = (i + 0.5f) * columnWidth;
            if (progress != null) {
                if (progress.steps() > 0) {
                    float barTop = baseline - Math.max(2 * density, chartHeight * progress.steps() / week.scaleMax);
                    paint.setColor(progress.hit() ? GOAL_REACHED_COLOR : WALK_COLOR);
                    canvas.drawRoundRect(center - columnWidth * 0.25f, barTop,
                            center + columnWidth * 0.25f, baseline, 4 * density, 4 * density, paint);
                }
                float goalY = baseline - chartHeight * progress.goal / week.scaleMax;
                paint.setColor(GOAL_MARKER_COLOR);
                canvas.drawLine(center - columnWidth * 0.34f, goalY,
                        center + columnWidth * 0.34f, goalY, paint);
            }
            paint.setColor(i == week.days.size() - 1 ? MainActivity.GREEN : MainActivity.INK);
            canvas.drawText(dayLabels[i], center, baseline + 19 * density, paint);
        }
    }
}
