package vibro.navigator.android.startup;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.android.location.AndroidLocationDiagnostics;
import vibro.navigator.android.location.AndroidLocationPermissions;
import vibro.navigator.nav.startup.NavigationPreflight;

public final class AndroidNavigationPreflight {

    private static final String LEGACY_EXTRA_APP_PACKAGE = "app_package";
    private static final String LEGACY_EXTRA_APP_UID = "app_uid";

    private AndroidNavigationPreflight() {
    }

    @NonNull
    public static NavigationPreflight.Status inspect(@NonNull Activity activity) {
        List<String> missingPermissions = collectMissingPermissions(activity);
        boolean showPermissionRationale = shouldShowPermissionRationale(activity, missingPermissions);
        boolean locationEnabled = AndroidLocationDiagnostics.isAnyProviderEnabled(activity);
        boolean notificationsEnabled = NotificationManagerCompat.from(activity).areNotificationsEnabled();
        boolean needsBatteryOptimizationExemption = needsBatteryOptimizationExemption(activity);
        return NavigationPreflight.Status.create(
                missingPermissions,
                showPermissionRationale,
                locationEnabled,
                notificationsEnabled,
                needsBatteryOptimizationExemption
        );
    }

    @NonNull
    public static Intent newLocationSettingsIntent() {
        return new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    }

    @NonNull
    public static Intent newNotificationSettingsIntent(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
        }

        ApplicationInfo applicationInfo = activity.getApplicationInfo();
        return new Intent("android.settings.APP_NOTIFICATION_SETTINGS")
                .putExtra(LEGACY_EXTRA_APP_PACKAGE, activity.getPackageName())
                .putExtra(LEGACY_EXTRA_APP_UID, applicationInfo.uid);
    }

    @NonNull
    public static Intent newBatteryOptimizationIntent(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        }
        return newAppDetailsSettingsIntent(activity);
    }

    @NonNull
    public static Intent newAppDetailsSettingsIntent(@NonNull Activity activity) {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", activity.getPackageName(), null));
    }

    @NonNull
    private static List<String> collectMissingPermissions(@NonNull Activity activity) {
        List<String> permissions = new ArrayList<>();
        boolean fineGranted = AndroidLocationPermissions.hasFineLocationPermission(activity);
        boolean coarseGranted = AndroidLocationPermissions.hasCoarseLocationPermission(activity);
        if (!NavigationPreflight.hasAnyLocationPermission(fineGranted, coarseGranted)) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !hasPermission(activity, Manifest.permission.POST_NOTIFICATIONS)) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        return permissions;
    }

    private static boolean hasPermission(@NonNull Activity activity, @NonNull String permission) {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean shouldShowPermissionRationale(
            @NonNull Activity activity,
            @NonNull List<String> permissions
    ) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }

    private static boolean needsBatteryOptimizationExemption(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        PowerManager powerManager = (PowerManager) activity.getSystemService(Activity.POWER_SERVICE);
        return powerManager != null && !powerManager.isIgnoringBatteryOptimizations(activity.getPackageName());
    }
}
