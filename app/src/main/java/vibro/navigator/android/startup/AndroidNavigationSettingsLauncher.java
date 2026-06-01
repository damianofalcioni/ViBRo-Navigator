package vibro.navigator.android.startup;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import vibro.navigator.logging.AppLogger;

public final class AndroidNavigationSettingsLauncher {

    private static final String TAG = "NavSettings";

    private AndroidNavigationSettingsLauncher() {
    }

    public static boolean launch(@NonNull Activity activity, @NonNull Intent primaryIntent) {
        if (tryLaunch(activity, primaryIntent, "primary")) {
            return true;
        }

        Intent fallbackIntent = AndroidNavigationPreflight.newAppDetailsSettingsIntent(activity);
        if (sameTarget(primaryIntent, fallbackIntent)) {
            AppLogger.w(TAG, "No resolvable settings activity for intent action=" + primaryIntent.getAction());
            return false;
        }
        return tryLaunch(activity, fallbackIntent, "fallback");
    }

    private static boolean tryLaunch(
            @NonNull Activity activity,
            @NonNull Intent intent,
            @NonNull String label
    ) {
        if (intent.resolveActivity(activity.getPackageManager()) == null) {
            AppLogger.w(TAG, "No activity resolves " + label + " settings intent action=" + intent.getAction()
                    + " data=" + String.valueOf(intent.getData()));
            return false;
        }
        try {
            activity.startActivity(intent);
            AppLogger.i(TAG, "Opened " + label + " settings intent action=" + intent.getAction()
                    + " data=" + String.valueOf(intent.getData()));
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            AppLogger.w(TAG, "Failed to open " + label + " settings intent action=" + intent.getAction()
                    + " data=" + String.valueOf(intent.getData()), e);
            return false;
        }
    }

    private static boolean sameTarget(@NonNull Intent first, @NonNull Intent second) {
        String firstAction = first.getAction();
        String secondAction = second.getAction();
        Uri firstData = first.getData();
        Uri secondData = second.getData();
        return String.valueOf(firstAction).equals(String.valueOf(secondAction))
                && String.valueOf(firstData).equals(String.valueOf(secondData));
    }
}
