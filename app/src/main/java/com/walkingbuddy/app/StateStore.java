package com.walkingbuddy.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.time.LocalDate;
import java.util.function.Consumer;

/** One synchronized source of truth for the activity and foreground service. */
public final class StateStore {
    private static StateStore instance;
    private final SharedPreferences prefs;
    private GameState state;
    private StateStore(Context context) {
        prefs = context.getSharedPreferences("walking_buddy", Context.MODE_PRIVATE);
        String raw = prefs.getString("state", "");
        try { state = decode(raw); }
        catch (IllegalStateException e) {
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
    public synchronized void replace(GameState restored) {
        prefs.edit().putString("previous_backup", encode(state)).apply();
        restored.tracking = false; restored.lastSensor = -1; restored.lastBoot = -1; restored.lastSensorDay = null;
        state = restored; state.advance(LocalDate.now(), System.currentTimeMillis()); save();
    }
    static String encode(GameState s) {
        try {
            JSONObject j = new JSONObject();
            j.put("version", 1); j.put("adopted", s.adopted); j.put("tracking", s.tracking); j.put("reduceMotion", s.reduceMotion);
            j.put("name", s.name); j.put("buddy", s.buddy); j.put("theme", s.theme); j.put("goal", s.goal); j.put("day", s.day);
            j.put("sensorSteps", s.sensorSteps); j.put("manualSteps", s.manualSteps); j.put("health", s.health); j.put("food", s.food);
            j.put("water", s.water); j.put("clean", s.clean); j.put("leaves", s.leaves); j.put("rewardedBlocks", s.rewardedBlocks);
            j.put("goalRewarded", s.goalRewarded); j.put("careCount", s.careCount); j.put("companionsAdopted", s.companionsAdopted);
            j.put("lastDecay", s.lastDecay); j.put("lastFeed", s.lastFeed); j.put("lastWater", s.lastWater); j.put("lastClean", s.lastClean);
            j.put("lastSensor", s.lastSensor); j.put("lastBoot", s.lastBoot);
            j.put("lastSensorDay", s.lastSensorDay == null ? "" : s.lastSensorDay.toString());
            JSONArray days = new JSONArray();
            for (java.util.Map.Entry<LocalDate, GameState.Day> e : s.history.entrySet()) {
                days.put(new JSONObject().put("date", e.getKey()).put("sensor", e.getValue().sensor).put("manual", e.getValue().manual).put("goal", e.getValue().goal));
            }
            j.put("history", days); j.put("memories", new JSONArray(s.memories)); return j.toString();
        } catch (JSONException e) { throw new IllegalStateException("Unable to save progress", e); }
    }
    static GameState decode(String raw) {
        GameState s = new GameState(LocalDate.now(), System.currentTimeMillis());
        if (raw.isEmpty()) return s;
        try {
            JSONObject j = new JSONObject(raw);
            if (j.getInt("version") != 1) throw new JSONException("Unsupported backup version");
            s.day = LocalDate.parse(j.getString("day")); s.adopted = j.optBoolean("adopted"); s.tracking = j.optBoolean("tracking");
            s.reduceMotion = j.optBoolean("reduceMotion"); s.name = j.optString("name", "Milo");
            s.buddy = Math.max(0, Math.min(7, j.optInt("buddy"))); s.theme = Math.max(0, Math.min(2, j.optInt("theme")));
            s.goal = Math.max(1000, Math.min(50000, j.optInt("goal", 6000))); s.sensorSteps = j.optInt("sensorSteps"); s.manualSteps = j.optInt("manualSteps");
            s.health = j.optInt("health", 80); s.food = j.optInt("food", 75); s.water = j.optInt("water", 75); s.clean = j.optInt("clean", 85);
            s.leaves = j.optInt("leaves", 4); s.rewardedBlocks = j.optInt("rewardedBlocks"); s.goalRewarded = j.optBoolean("goalRewarded");
            s.careCount = j.optInt("careCount"); s.companionsAdopted = j.optInt("companionsAdopted"); s.lastDecay = j.optLong("lastDecay", System.currentTimeMillis());
            s.lastFeed = j.optLong("lastFeed"); s.lastWater = j.optLong("lastWater"); s.lastClean = j.optLong("lastClean");
            s.lastSensor = j.optLong("lastSensor", -1); s.lastBoot = j.optInt("lastBoot", -1);
            String sensorDay = j.optString("lastSensorDay"); if (!sensorDay.isEmpty()) s.lastSensorDay = LocalDate.parse(sensorDay);
            JSONArray days = j.optJSONArray("history");
            if (days != null) for (int i = 0; i < days.length(); i++) {
                JSONObject d = days.getJSONObject(i); s.history.put(LocalDate.parse(d.getString("date")), new GameState.Day(d.getInt("sensor"), d.getInt("manual"), d.getInt("goal")));
            }
            JSONArray memories = j.optJSONArray("memories");
            if (memories != null) for (int i = 0; i < memories.length(); i++) s.memories.add(memories.getString(i));
        } catch (JSONException | java.time.format.DateTimeParseException e) {
            // The constructor preserves unreadable local data; imports are rejected.
            android.util.Log.e("WalkingBuddy", "Saved progress could not be read", e);
            throw new IllegalStateException("Saved progress could not be read", e);
        }
        if (s.name.trim().isEmpty() || s.name.length() > 20 || s.sensorSteps < 0 || s.manualSteps < 0 || s.sensorSteps > GameState.MAX_DAILY_STEPS || s.manualSteps > GameState.MAX_DAILY_STEPS || s.steps() > GameState.MAX_DAILY_STEPS || s.health < 0 || s.health > 100 || s.food < 0 || s.food > 100 || s.water < 0 || s.water > 100 || s.clean < 0 || s.clean > 100 || s.leaves < 0 || s.rewardedBlocks < 0 || s.rewardedBlocks > 400 || s.careCount < 0 || s.history.size() > 36500) throw new IllegalStateException("Invalid Walking Buddy progress");
        for (GameState.Day d : s.history.values()) if (d.sensor < 0 || d.manual < 0 || d.sensor > GameState.MAX_DAILY_STEPS || d.manual > GameState.MAX_DAILY_STEPS || d.steps() > GameState.MAX_DAILY_STEPS || d.goal < 1000 || d.goal > 50000) throw new IllegalStateException("Invalid walking history");
        return s;
    }
}
