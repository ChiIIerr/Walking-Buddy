package com.walkingbuddy.app;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/** Pure, deterministic game rules. Android persistence and sensors live outside this class. */
public final class GameState {
    public static final String[] IDS = {"milo", "maple", "rusty", "mochi", "fern", "bamboo", "pip", "nova"};
    public static final String[] NAMES = {"Milo", "Maple", "Rusty", "Mochi", "Fern", "Bamboo", "Pip", "Nova"};
    public static final String[] SPECIES = {"Tabby cat", "Corgi", "Fox", "Bunny", "Frog", "Panda", "Penguin", "Dragon"};
    public static final String[] TRAITS = {"A curious little explorer", "A very good walking buddy", "Always takes the scenic route", "Small hops. Big adventures.", "Loves a rainy-day stroll", "A gentle trail companion", "Waddles with determination", "Made for magical adventures"};
    public static final int[] UNLOCKS = {0, 0, 0, 0, 20000, 40000, 60000, 100000};
    public static final int MAX_DAILY_STEPS = 200000;
    public boolean adopted;
    public boolean tracking;
    public boolean reduceMotion;
    public String name = "Milo";
    public int buddy;
    public int theme;
    public int goal = 6000;
    public LocalDate day;
    public int sensorSteps;
    public int manualSteps;
    public int health = 80, food = 75, water = 75, clean = 85;
    public int leaves = 4;
    public int rewardedBlocks;
    public boolean goalRewarded;
    public int careCount;
    public int companionsAdopted;
    public long lastDecay;
    public long lastFeed, lastWater, lastClean;
    public long lastSensor = -1;
    public int lastBoot = -1;
    public LocalDate lastSensorDay;
    public final TreeMap<LocalDate, Day> history = new TreeMap<>();
    public final List<String> memories = new ArrayList<>();

    public static final class Day {
        public final int sensor, manual, goal;
        public Day(int sensor, int manual, int goal) { this.sensor = sensor; this.manual = manual; this.goal = goal; }
        public int steps() { return sensor + manual; }
        public boolean hit() { return steps() >= goal; }
    }

    public GameState(LocalDate today, long now) { day = today; lastDecay = now; }
    public int steps() { return sensorSteps + manualSteps; }
    public long lifetime() { long n = steps(); for (Day d : history.values()) n += d.steps(); return n; }
    public int goalDays() { int n = steps() >= goal ? 1 : 0; for (Day d : history.values()) if (d.hit()) n++; return n; }
    public boolean unlocked(int index) { return index >= 0 && index < IDS.length && lifetime() >= UNLOCKS[index]; }
    public int unlockedCount() { int n = 0; for (int i = 0; i < IDS.length; i++) if (unlocked(i)) n++; return n; }
    public String mood() {
        if (health == 0) return "Time for a new beginning";
        if (health < 30) return "Needs a little extra love";
        if (food < 25) return "A little hungry";
        if (water < 25) return "Ready for a drink";
        if (clean < 25) return "Could use a little bath";
        if (steps() >= goal) return "So proud of you!";
        return "Happy to be by your side";
    }
    public int streak() {
        int n = 0; LocalDate cursor = day;
        if (steps() >= goal) { n++; cursor = cursor.minusDays(1); }
        else cursor = cursor.minusDays(1);
        for (;;) {
            Day d = history.get(cursor);
            if (d == null || !d.hit()) break;
            n++; cursor = cursor.minusDays(1);
        }
        return n;
    }
    public int bestStreak() {
        int best = 0, run = 0; LocalDate previous = null;
        for (java.util.Map.Entry<LocalDate, Day> e : history.entrySet()) {
            if (previous != null && !e.getKey().equals(previous.plusDays(1))) run = 0;
            run = e.getValue().hit() ? run + 1 : 0; best = Math.max(best, run); previous = e.getKey();
        }
        if (steps() >= goal) {
            if (previous == null || previous.equals(day.minusDays(1))) run++;
            else run = 1;
            best = Math.max(best, run);
        }
        return best;
    }
    public void advance(LocalDate today, long now) {
        // Clock changes backwards never duplicate rewards or rewrite completed days.
        if (today.isAfter(day)) {
            long gap = ChronoUnit.DAYS.between(day, today);
            history.put(day, new Day(sensorSteps, manualSteps, goal));
            if (adopted) {
                if (steps() < goal) health = Math.max(0, health - 20);
                if (gap > 1) health = (int) Math.max(0, health - Math.min(gap - 1, 5) * 20);
            }
            day = today; sensorSteps = 0; manualSteps = 0; rewardedBlocks = 0; goalRewarded = false;
        }
        long hours = Math.max(0, (now - lastDecay) / 3600000L);
        if (hours > 0) {
            int drop = (int) Math.min(hours, 100);
            food = Math.max(0, food - drop * 3); water = Math.max(0, water - drop * 4); clean = Math.max(0, clean - drop * 2);
            lastDecay += hours * 3600000L;
        }
    }
    public void sensor(long value, int boot, LocalDate sampleDay) {
        // Baseline on install, reboot, resume-after-pause, and first sample of a new day.
        if (lastSensor >= 0 && boot == lastBoot && sampleDay.equals(lastSensorDay) && value >= lastSensor) {
            long delta = value - lastSensor;
            if (sampleDay.equals(day)) sensorSteps += (int) Math.min(delta, MAX_DAILY_STEPS - steps());
        }
        lastSensor = value; lastBoot = boot; lastSensorDay = sampleDay; reward();
    }
    public void setManual(int value) {
        if (value < 0 || value > MAX_DAILY_STEPS - sensorSteps) throw new IllegalArgumentException("Enter between 0 and " + (MAX_DAILY_STEPS - sensorSteps) + " steps.");
        manualSteps = value; reward();
    }
    public void setGoal(int value) {
        if (value < 1000 || value > 50000) throw new IllegalArgumentException("Choose a goal from 1,000 to 50,000 steps.");
        goal = value; reward();
    }
    private void reward() {
        if (!adopted) return;
        int blocks = steps() / 500;
        if (blocks > rewardedBlocks) { leaves += blocks - rewardedBlocks; rewardedBlocks = blocks; }
        if (steps() >= goal && !goalRewarded) {
            if (health > 0) health = Math.min(100, health + 15);
            leaves += 3; goalRewarded = true;
        }
    }
    public void adopt(int index, String nickname, long now) {
        if (!unlocked(index)) throw new IllegalArgumentException("Keep walking to unlock this buddy.");
        if (nickname == null || nickname.trim().isEmpty() || nickname.trim().length() > 20) throw new IllegalArgumentException("Give your buddy a name with 1–20 characters.");
        if (companionsAdopted > 0 && health == 0) memories.add(name + " · " + SPECIES[buddy] + " · " + day);
        buddy = index; name = nickname.trim(); adopted = true; health = 80;
        food = 75; water = 75; clean = 85; lastDecay = now; lastFeed = lastWater = lastClean = 0;
        companionsAdopted++; reward();
    }
    public void choose(int index) {
        if (health == 0) throw new IllegalArgumentException("Adopt a new buddy to begin again.");
        if (!unlocked(index)) throw new IllegalArgumentException("Keep walking to unlock this buddy.");
        buddy = index; name = NAMES[index];
    }
    public String care(String action, long now) {
        if (health == 0) return "Your buddy's journey has ended. Adopt a new friend to begin again.";
        long previous = action.equals("feed") ? lastFeed : action.equals("water") ? lastWater : lastClean;
        if (previous != 0 && now - previous < 60000) return "All cared for! Try again in a minute.";
        switch (action) {
            case "feed":
                if (food >= 95) return name + " is already full.";
                if (leaves < 1) return "Walk 500 steps to earn a leaf for a snack.";
                leaves--; food = Math.min(100, food + 30); lastFeed = now; break;
            case "water":
                if (water >= 95) return name + " has plenty of water.";
                water = Math.min(100, water + 35); lastWater = now; break;
            case "clean":
                if (clean >= 95) return name + " is already squeaky clean.";
                clean = Math.min(100, clean + 30); lastClean = now; break;
            default: return "Choose a care action.";
        }
        careCount++;
        return action.equals("feed") ? "A tasty snack for " + name + "!" : action.equals("water") ? "A refreshing drink for " + name + "!" : "Fresh, clean, and ready to explore!";
    }
}
