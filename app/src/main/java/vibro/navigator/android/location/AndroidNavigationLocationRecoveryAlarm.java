package vibro.navigator.android.location;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;

public final class AndroidNavigationLocationRecoveryAlarm {
    private static final String TAG = "NavLocationRecovery";
    private static final int REQUEST_CODE = 7731;

    @Nullable
    private final AlarmManager alarmManager;
    @NonNull
    private final PendingIntent pendingIntent;

    public AndroidNavigationLocationRecoveryAlarm(
            @NonNull Context context,
            @NonNull Class<?> serviceClass,
            @NonNull String action
    ) {
        Context appContext = context.getApplicationContext();
        alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(appContext, serviceClass).setAction(action);
        pendingIntent = PendingIntent.getService(
                appContext,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    public void schedule(long triggerElapsedRealtimeMs) {
        if (alarmManager == null || triggerElapsedRealtimeMs < 0L) {
            return;
        }
        try {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerElapsedRealtimeMs,
                    pendingIntent
            );
        } catch (RuntimeException error) {
            AppLogger.w(TAG, "Failed to schedule stale-location recovery", error);
        }
    }

    public void cancel() {
        if (alarmManager == null) {
            return;
        }
        try {
            alarmManager.cancel(pendingIntent);
        } catch (RuntimeException error) {
            AppLogger.w(TAG, "Failed to cancel stale-location recovery", error);
        }
    }
}
