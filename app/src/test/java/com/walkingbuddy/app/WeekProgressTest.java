package com.walkingbuddy.app;

import org.junit.Test;
import java.time.LocalDate;
import static org.junit.Assert.*;

public class WeekProgressTest {
    private final LocalDate today = LocalDate.of(2026, 11, 15);

    @Test public void raisingTodaysGoalPreservesYesterdaysSuccess() {
        GameState state = new GameState(today.minusDays(1), 0);
        state.setGoal(4000);
        state.setManual(4500);
        state.advance(today, 0);
        state.setGoal(8000);
        state.setManual(4500);

        WeekProgress week = new WeekProgress(state);

        assertEquals(4000, week.days.get(5).progress.goal);
        assertTrue(week.days.get(5).progress.hit());
        assertEquals(8000, week.days.get(6).progress.goal);
        assertFalse(week.days.get(6).progress.hit());
        assertEquals(9000, week.totalSteps);
    }

    @Test public void weekExcludesOlderRecordsAndDistinguishesMissingDaysFromZero() {
        GameState state = new GameState(today, 0);
        state.history.put(today.minusDays(7), new GameState.Day(130000, 0, 40000));
        state.history.put(today.minusDays(6), new GameState.Day(0, 0, 12000));
        state.setManual(1000);

        WeekProgress week = new WeekProgress(state);

        assertEquals(7, week.days.size());
        assertEquals(today.minusDays(6), week.days.get(0).date);
        assertEquals(0, week.days.get(0).progress.steps());
        assertNull(week.days.get(1).progress);
        assertEquals(today, week.days.get(6).date);
        assertEquals(1000, week.totalSteps);
        assertEquals(12000, week.scaleMax);
    }

    @Test public void scaleIncludesTheTallestVisibleWalk() {
        GameState state = new GameState(today, 0);
        state.history.put(today.minusDays(1), new GameState.Day(17000, 0, 5000));
        assertEquals(17000, new WeekProgress(state).scaleMax);
    }
}
