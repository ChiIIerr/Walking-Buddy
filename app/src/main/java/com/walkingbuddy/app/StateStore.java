package com.walkingbuddy.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDate;
import java.util.function.Consumer;

/** One synchronized source of truth for the activity and foreground service. */
public final class StateStore {
    private static final String PREVIOUS_PROGRESS = "previous_backup";
    private static StateStore instance;
    private final SharedPreferences prefs;
    private GameState state;
    private StateStore(Context context) {
        prefs = context.getSharedPreferences("walking_buddy", Context.MODE_PRIVATE);
        String raw = prefs.getString("state", "");
        try { state = decode(raw); }
        catch (IllegalStateException e) {
            android.util.Log.e("WalkingBuddy", "Saved progress could not be read", e);
            prefs.edit().putString("unreadable_backup", raw).apply();
            state = new GameState(LocalDate.now(), System.currentTimeMillis());
            android.widget.Toast.makeText(context, "Saved progress could not be read. A recovery copy was preserved.", android.widget.Toast.LENGTH_LONG).show();
        }
    }
    public static synchronized StateStore get(Context context) {
        if (instance == null) instance = new StateStore(context.getApplicationContext());
        return instance;
    }
    public synchronized GameState read() { state.advance(LocalDate.now(), System.currentTimeMillis()); return state; }
    public synchronized GameState update(Consumer<GameState> change) {
        state.advance(LocalDate.now(), System.currentTimeMillis()); change.accept(state); save(); return state;
    }
    public synchronized void save() { prefs.edit().putString("state", encode(state)).apply(); }
    public synchronized void reset() {
        state = new GameState(LocalDate.now(), System.currentTimeMillis());
        // Clear recovery copies too: reset must delete all prior walking data.
        prefs.edit().clear().putString("state", encode(state)).apply();
    }
    public synchronized boolean hasPreviousProgress() {
        return prefs.contains(PREVIOUS_PROGRESS);
    }
    public synchronized GameState previousProgress() {
        String raw = prefs.getString(PREVIOUS_PROGRESS, "");
        if (raw.trim().isEmpty()) throw new IllegalStateException("Previous progress is unavailable");
        return decode(raw);
    }
    public synchronized void replace(GameState restored) {
        String previous = encode(state);
        restored.tracking = false;
        restored.lastSensor = -1;
        restored.lastBoot = -1;
        restored.lastSensorDay = null;
        restored.lastSensorTimestamp = -1;
        restored.advance(LocalDate.now(), System.currentTimeMillis());
        String replacement = encode(restored);
        prefs.edit()
                .putString(PREVIOUS_PROGRESS, previous)
                .putString("state", replacement)
                .apply();
        state = restored;
    }
    static String encode(GameState s) { return ProgressCodec.encode(s); }
    static GameState decode(String raw) { return ProgressCodec.decode(raw); }
}
