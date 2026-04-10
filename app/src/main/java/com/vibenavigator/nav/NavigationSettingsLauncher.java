package com.vibenavigator.nav;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.vibenavigator.util.AppLogger;

public final class NavigationSettingsLauncher {

    private static final String TAG = "NavSettings";

    private NavigationSettingsLauncher() {
    }

    public static boolean launch(@NonNull Activity activity, @NonNull Intent primaryIntent) {
        if (tryLaunch(activity, primaryIntent, "primary")) {
            return true;
        }

        Intent fallbackIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", activity.getPackageName(), null));
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
