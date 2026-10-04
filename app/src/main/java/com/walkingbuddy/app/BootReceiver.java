package com.walkingbuddy.app;
import android.content.*;

public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
        StateStore store = StateStore.get(c);
        if (store.read().tracking && StepService.allowed(c) && StepService.available(c)) {
            try { c.startForegroundService(new Intent(c, StepService.class)); }
            catch (IllegalStateException | SecurityException e) { store.update(s -> { s.tracking = false; s.lastSensor = -1; }); }
        }
    }
}
