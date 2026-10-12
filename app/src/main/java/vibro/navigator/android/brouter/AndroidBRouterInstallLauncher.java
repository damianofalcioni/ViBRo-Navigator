package vibro.navigator.android.brouter;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.logging.AppLogger;

public final class AndroidBRouterInstallLauncher {

    private static final String TAG = "BRouterInstall";
    private static final String ACTION_VIEW = Intent.ACTION_VIEW;
    private static final String PLAY_STORE_APP_URI =
            "market://details?id=" + BRouterProfilesRepository.BROUTER_PACKAGE_NAME;
    private static final String PLAY_STORE_WEB_URI =
            "https://play.google.com/store/apps/details?id=" + BRouterProfilesRepository.BROUTER_PACKAGE_NAME;
    private static final String FDROID_WEB_URI =
            "https://f-droid.org/packages/" + BRouterProfilesRepository.BROUTER_PACKAGE_NAME + "/";

    private AndroidBRouterInstallLauncher() {
    }

    public static boolean launchPlayStore(@NonNull Activity activity) {
        return launchPlayStore(activity, null);
    }

    public static boolean launchPlayStore(@NonNull Activity activity, @Nullable Integer requestCode) {
        return launchPlayStore(new ActivityInstallLauncher(activity, requestCode));
    }

    static boolean launchPlayStore(@NonNull InstallLauncher launcher) {
        return launchFirstResolvable(
                launcher,
                "Play Store",
                new InstallTarget(ACTION_VIEW, PLAY_STORE_APP_URI),
                new InstallTarget(ACTION_VIEW, PLAY_STORE_WEB_URI)
        );
    }

    public static boolean launchFdroid(@NonNull Activity activity) {
        return launchFdroid(activity, null);
    }

    public static boolean launchFdroid(@NonNull Activity activity, @Nullable Integer requestCode) {
        return launchFdroid(new ActivityInstallLauncher(activity, requestCode));
    }

    static boolean launchFdroid(@NonNull InstallLauncher launcher) {
        return launchFirstResolvable(
                launcher,
                "F-Droid",
                new InstallTarget(ACTION_VIEW, FDROID_WEB_URI)
        );
    }

    private static boolean launchFirstResolvable(
            @NonNull InstallLauncher launcher,
            @NonNull String label,
            @NonNull InstallTarget... targets
    ) {
        for (InstallTarget target : targets) {
            if (!launcher.canResolve(target)) {
                AppLogger.w(TAG, "No activity resolves " + label + " install intent data="
                        + target.uriString);
                continue;
            }
            if (launcher.launch(target)) {
                AppLogger.i(TAG, "Opened " + label + " install intent data="
                        + target.uriString);
                return true;
            }
        }
        return false;
    }

    interface InstallLauncher {
        boolean canResolve(@NonNull InstallTarget target);

        boolean launch(@NonNull InstallTarget target);
    }

    static final class InstallTarget {
        @NonNull
        final String action;
        @NonNull
        final String uriString;

        InstallTarget(@NonNull String action, @NonNull String uriString) {
            this.action = action;
            this.uriString = uriString;
        }
    }

    private static final class ActivityInstallLauncher implements InstallLauncher {
        @NonNull
        private final Activity activity;
        private final Integer requestCode;

        ActivityInstallLauncher(@NonNull Activity activity, @Nullable Integer requestCode) {
            this.activity = activity;
            this.requestCode = requestCode;
        }

        @Override
        public boolean canResolve(@NonNull InstallTarget target) {
            return toIntent(target).resolveActivity(activity.getPackageManager()) != null;
        }

        @Override
        public boolean launch(@NonNull InstallTarget target) {
            try {
                if (requestCode == null) {
                    activity.startActivity(toIntent(target));
                } else {
                    activity.startActivityForResult(toIntent(target), requestCode);
                }
                return true;
            } catch (ActivityNotFoundException | SecurityException e) {
                AppLogger.w(TAG, "Failed to open install intent data=" + target.uriString, e);
                return false;
            }
        }

        @NonNull
        private static Intent toIntent(@NonNull InstallTarget target) {
            return new Intent(target.action, Uri.parse(target.uriString));
        }
    }
}
