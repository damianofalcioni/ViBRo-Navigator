package vibro.navigator.brouter;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import vibro.navigator.logging.AppLogger;

public final class BRouterInstallLauncher {

    private static final String TAG = "BRouterInstall";
    private static final Uri PLAY_STORE_APP_URI =
            Uri.parse("market://details?id=" + BRouterProfilesRepository.BROUTER_PACKAGE_NAME);
    private static final Uri PLAY_STORE_WEB_URI =
            Uri.parse("https://play.google.com/store/apps/details?id="
                    + BRouterProfilesRepository.BROUTER_PACKAGE_NAME);
    private static final Uri FDROID_WEB_URI =
            Uri.parse("https://f-droid.org/packages/" + BRouterProfilesRepository.BROUTER_PACKAGE_NAME + "/");

    private BRouterInstallLauncher() {
    }

    public static boolean launchPlayStore(@NonNull Activity activity) {
        return launchFirstResolvable(
                activity,
                "Play Store",
                new Intent(Intent.ACTION_VIEW, PLAY_STORE_APP_URI),
                new Intent(Intent.ACTION_VIEW, PLAY_STORE_WEB_URI)
        );
    }

    public static boolean launchFdroid(@NonNull Activity activity) {
        return launchFirstResolvable(
                activity,
                "F-Droid",
                new Intent(Intent.ACTION_VIEW, FDROID_WEB_URI)
        );
    }

    private static boolean launchFirstResolvable(
            @NonNull Activity activity,
            @NonNull String label,
            @NonNull Intent... intents
    ) {
        for (Intent intent : intents) {
            if (intent.resolveActivity(activity.getPackageManager()) == null) {
                AppLogger.w(TAG, "No activity resolves " + label + " install intent data="
                        + String.valueOf(intent.getData()));
                continue;
            }
            try {
                activity.startActivity(intent);
                AppLogger.i(TAG, "Opened " + label + " install intent data="
                        + String.valueOf(intent.getData()));
                return true;
            } catch (ActivityNotFoundException | SecurityException e) {
                AppLogger.w(TAG, "Failed to open " + label + " install intent data="
                        + String.valueOf(intent.getData()), e);
            }
        }
        return false;
    }
}
