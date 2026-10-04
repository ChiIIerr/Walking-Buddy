package com.walkingbuddy.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.*;
import android.os.*;
import android.provider.Settings;
import java.time.*;
import java.util.Locale;

/** Low-power hardware step counter kept registered by a user-visible health service. */
public final class StepService extends Service implements SensorEventListener {
    public static final String UPDATE = "com.walkingbuddy.app.PROGRESS_CHANGED";
    public static final String PAUSE = "com.walkingbuddy.app.PAUSE";
    private static final String CHANNEL = "walking_steps";
    private SensorManager sensors;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastNotification;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            StateStore.get(StepService.this).update(s -> {});
            notifyProgress(); handler.postDelayed(this, 60000);
        }
    };
    public static boolean available(Context c) {
        return ((SensorManager)c.getSystemService(SENSOR_SERVICE)).getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null;
    }
    public static boolean allowed(Context c) {
        return Build.VERSION.SDK_INT < 29 || c.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
    }
    @Override public void onCreate() {
        super.onCreate();
        sensors = (SensorManager)getSystemService(SENSOR_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL, "Daily step tracking", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Shows your walking progress while step tracking is active.");
        channel.setShowBadge(false); getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        StateStore store = StateStore.get(this);
        if (intent != null && PAUSE.equals(intent.getAction())) {
            store.update(s -> { s.tracking = false; s.lastSensor = -1; });
            sendBroadcast(new Intent(UPDATE).setPackage(getPackageName())); stopSelf(); return START_NOT_STICKY;
        }
        if (!store.read().tracking || !allowed(this) || !available(this)) {
            store.update(s -> s.tracking = false); stopSelf(); return START_NOT_STICKY;
        }
        try {
            if (Build.VERSION.SDK_INT >= 34) startForeground(1, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH);
            else startForeground(1, notification());
            if (!sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER), SensorManager.SENSOR_DELAY_NORMAL)) {
                store.update(s -> s.tracking = false); stopSelf(); return START_NOT_STICKY;
            }
            handler.removeCallbacks(tick); handler.postDelayed(tick, 60000);
            return START_STICKY;
        } catch (SecurityException | IllegalStateException e) {
            store.update(s -> s.tracking = false); stopSelf(); return START_NOT_STICKY;
        }
    }
    private Notification notification() {
        GameState s = StateStore.get(this).read();
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pause = PendingIntent.getService(this, 1, new Intent(this, StepService.class).setAction(PAUSE), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL)
            .setSmallIcon(com.walkingbuddy.app.R.drawable.ic_notification)
            .setContentTitle("Walking Buddy · " + String.format(Locale.US, "%,d", s.steps()) + " steps")
            .setContentText(String.format(Locale.US, "%,d", Math.max(0, s.goal - s.steps())) + " to your goal · " + s.name + " is cheering you on")
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(new Notification.Action.Builder(null, "Pause tracking", pause).build()).build();
    }
    @Override public void onSensorChanged(SensorEvent event) {
        if (!allowed(this)) { StateStore.get(this).update(s -> s.tracking = false); stopSelf(); return; }
        long value = (long)event.values[0];
        long eventMillis = System.currentTimeMillis() - (SystemClock.elapsedRealtimeNanos() - event.timestamp) / 1000000L;
        LocalDate sampleDay = Instant.ofEpochMilli(eventMillis).atZone(ZoneId.systemDefault()).toLocalDate();
        int boot = Settings.Global.getInt(getContentResolver(), Settings.Global.BOOT_COUNT, 0);
        StateStore.get(this).update(s -> s.sensor(value, boot, sampleDay));
        sendBroadcast(new Intent(UPDATE).setPackage(getPackageName()));
        if (SystemClock.elapsedRealtime() - lastNotification > 5000) notifyProgress();
    }
    private void notifyProgress() {
        lastNotification = SystemClock.elapsedRealtime(); getSystemService(NotificationManager.class).notify(1, notification());
        sendBroadcast(new Intent(UPDATE).setPackage(getPackageName()));
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() {
        sensors.unregisterListener(this); handler.removeCallbacksAndMessages(null); stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy();
    }
}
