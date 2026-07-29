package vibro.navigator.android.intent;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import vibro.navigator.logging.AppLogger;

public final class AndroidGoogleMapsSearchLauncher {
    private static final String TAG = "GoogleMapsSearchLauncher";

    private AndroidGoogleMapsSearchLauncher() {
    }

    public static boolean open(@NonNull Context context, @NonNull String query) {
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        if (tryStart(context, AndroidGoogleMapsSearchIntent.createAppIntent(trimmed))) {
            AppLogger.i(TAG, "Opened Google Maps app search query=" + trimmed);
            return true;
        }
        if (tryStart(context, AndroidGoogleMapsSearchIntent.createWebIntent(trimmed))) {
            AppLogger.i(TAG, "Opened Google Maps web search query=" + trimmed);
            return true;
        }
        return false;
    }

    private static boolean tryStart(@NonNull Context context, @NonNull Intent intent) {
        try {
            Intent launchIntent = new Intent(intent);
            if (!(context instanceof Activity)) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(launchIntent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            AppLogger.w(TAG, "Google Maps search intent could not be handled", e);
            return false;
        } catch (RuntimeException e) {
            AppLogger.w(TAG, "Failed to open Google Maps search intent", e);
            return false;
        }
    }
}
