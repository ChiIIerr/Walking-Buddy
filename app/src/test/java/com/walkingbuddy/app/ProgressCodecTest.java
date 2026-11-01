package com.walkingbuddy.app;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.junit.Test;
import java.time.LocalDate;
import static org.junit.Assert.*;

public class ProgressCodecTest {
    private final LocalDate day = LocalDate.of(2026, 10, 4);
    private final long now = 1791133200000L;

    private GameState progress() {
        GameState s = new GameState(day, now);
        s.adopt(2, "Scout", now);
        s.tracking = true;
        s.sensor(100, 1, day, 1000);
        s.sensor(600, 1, day, 3000);
        s.setManual(1000);
        s.history.put(day.minusDays(1), new GameState.Day(2000, 4000, 5000));
        s.memories.add("Milo · Tabby cat · 2026-10-02");
        return s;
    }

    private JSONObject backup() throws JSONException { return new JSONObject(ProgressCodec.encode(progress())); }

    @Test public void roundTripPreservesExistingProgressAndSensorOrder() {
        GameState original = progress();
        GameState restored = ProgressCodec.decode(ProgressCodec.encode(original));
        assertEquals("Scout", restored.name);
        assertEquals(2, restored.buddy);
        assertEquals(1500, restored.steps());
        assertEquals(7500, restored.lifetime());
        assertEquals(7, restored.leaves);
        assertEquals(5000, restored.history.get(day.minusDays(1)).goal);
        assertEquals(original.memories, restored.memories);
        assertEquals(3000, restored.lastSensorTimestamp);
        assertEquals(ProgressCodec.encode(original), ProgressCodec.encode(restored));
    }

    @Test public void versionOneBackupWithoutTimestampStillContinuesCounting() throws JSONException {
        JSONObject legacy = backup();
        legacy.remove("lastSensorTimestamp");
        GameState restored = ProgressCodec.decode(legacy.toString());
        assertEquals(-1, restored.lastSensorTimestamp);
        restored.sensor(700, 1, day, 4000);
        assertEquals(1600, restored.steps());
        assertEquals(7600, restored.lifetime());
    }

    @Test public void restartCannotRecountAnOlderSensorEvent() {
        GameState restored = ProgressCodec.decode(ProgressCodec.encode(progress()));
        restored.sensor(400, 1, day, 2000);
        restored.sensor(700, 1, day, 4000);
        assertEquals(1600, restored.steps());
        assertEquals(700, restored.lastSensor);
    }

    @Test public void newBootAfterRestartAcceptsASmallerTimestamp() {
        GameState restored = ProgressCodec.decode(ProgressCodec.encode(progress()));
        restored.sensor(50, 2, day, 10);
        restored.sensor(150, 2, day, 20);
        assertEquals(1600, restored.steps());
    }

    @Test public void unknownOptionalFieldsDoNotEraseProgress() throws JSONException {
        JSONObject backup = backup().put("futureOptionalField", "ignored");
        assertEquals(7500, ProgressCodec.decode(backup.toString()).lifetime());
    }

    @Test(expected = IllegalStateException.class)
    public void duplicateHistoryDatesAreRejected() throws JSONException {
        JSONObject backup = backup();
        JSONArray history = backup.getJSONArray("history");
        history.put(new JSONObject(history.getJSONObject(0).toString()));
        ProgressCodec.decode(backup.toString());
    }

    @Test(expected = IllegalStateException.class)
    public void currentDayInHistoryIsRejectedInsteadOfDoubleCounting() throws JSONException {
        JSONObject backup = backup();
        backup.getJSONArray("history").getJSONObject(0).put("date", day.toString());
        ProgressCodec.decode(backup.toString());
    }

    @Test(expected = IllegalStateException.class)
    public void futureHistoryIsRejected() throws JSONException {
        JSONObject backup = backup();
        backup.getJSONArray("history").getJSONObject(0).put("date", day.plusDays(1).toString());
        ProgressCodec.decode(backup.toString());
    }

    @Test(expected = IllegalStateException.class)
    public void invalidSensorTimestampIsRejected() throws JSONException {
        ProgressCodec.decode(backup().put("lastSensorTimestamp", -2).toString());
    }

    @Test(expected = IllegalStateException.class)
    public void unsupportedBackupVersionIsRejected() throws JSONException {
        ProgressCodec.decode(backup().put("version", 2).toString());
    }

    @Test public void emptyLocalStorageStillStartsANewAdventure() {
        GameState s = ProgressCodec.decode("");
        assertFalse(s.adopted);
        assertEquals(0, s.steps());
        assertEquals(-1, s.lastSensorTimestamp);
    }
}
