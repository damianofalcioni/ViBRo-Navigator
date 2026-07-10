package vibro.navigator.android.service;

import android.app.ActivityManager;
import android.content.Context;

import androidx.annotation.NonNull;

public final class AndroidServiceRunningState {

    private AndroidServiceRunningState() {
    }

    @SuppressWarnings("deprecation")
    public static boolean isRunning(@NonNull Context context, @NonNull Class<?> serviceClass) {
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return false;
        }
        String expectedClassName = serviceClass.getName();
        String expectedPackageName = context.getPackageName();
        for (ActivityManager.RunningServiceInfo service
                : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (service.service == null) {
                continue;
            }
            if (expectedClassName.equals(service.service.getClassName())
                    && expectedPackageName.equals(service.service.getPackageName())) {
                return true;
            }
        }
        return false;
    }
}
