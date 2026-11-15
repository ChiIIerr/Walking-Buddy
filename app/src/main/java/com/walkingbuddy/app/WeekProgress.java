package com.walkingbuddy.app;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Snapshot of the seven calendar days shown in the journal, oldest first. */
final class WeekProgress {
    static final int DAYS = 7;

    static final class Entry {
        final LocalDate date;
        // A skipped date has no recorded progress or goal.
        final GameState.Day progress;

        Entry(LocalDate date, GameState.Day progress) {
            this.date = date;
            this.progress = progress;
        }
    }

    final List<Entry> days;
    final int totalSteps;
    final int scaleMax;

    WeekProgress(GameState state) {
        List<Entry> entries = new ArrayList<>(DAYS);
        int total = 0;
        int max = state.goal;
        for (int offset = DAYS - 1; offset >= 0; offset--) {
            LocalDate date = state.day.minusDays(offset);
            GameState.Day progress = offset == 0
                    ? new GameState.Day(state.sensorSteps, state.manualSteps, state.goal)
                    : state.history.get(date);
            entries.add(new Entry(date, progress));
            if (progress != null) {
                total += progress.steps();
                max = Math.max(max, Math.max(progress.steps(), progress.goal));
            }
        }
        days = Collections.unmodifiableList(entries);
        totalSteps = total;
        scaleMax = max;
    }
}
