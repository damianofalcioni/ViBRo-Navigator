package vibro.navigator.nav;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NavigationPreflight {

    private static final String LEGACY_EXTRA_APP_PACKAGE = "app_package";
    private static final String LEGACY_EXTRA_APP_UID = "app_uid";

    public static final class Status {
        @NonNull
        public final List<String> missingPermissions;
        public final boolean showPermissionRationale;
        public final boolean locationEnabled;
        public final boolean notificationsEnabled;
        public final boolean needsBatteryOptimizationExemption;

        private Status(
                @NonNull List<String> missingPermissions,
                boolean showPermissionRationale,
                boolean locationEnabled,
                boolean notificationsEnabled,
                boolean needsBatteryOptimizationExemption
        ) {
            this.missingPermissions = Collections.unmodifiableList(new ArrayList<>(missingPermissions));
            this.showPermissionRationale = showPermissionRationale;
            this.locationEnabled = locationEnabled;
            this.notificationsEnabled = notificationsEnabled;
            this.needsBatteryOptimizationExemption = needsBatteryOptimizationExemption;
        }

        public boolean hasMissingPermissions() {
            return !missingPermissions.isEmpty();
        }

        @NonNull
        public static Status create(
                @NonNull List<String> missingPermissions,
                boolean showPermissionRationale,
                boolean locationEnabled,
                boolean notificationsEnabled,
                boolean needsBatteryOptimizationExemption
        ) {
            return new Status(
                    missingPermissions,
                    showPermissionRationale,
                    locationEnabled,
                    notificationsEnabled,
                    needsBatteryOptimizationExemption
            );
        }
    }

    private NavigationPreflight() {
    }

    @NonNull
    public static Status inspect(@NonNull Activity activity) {
        List<String> missingPermissions = collectMissingPermissions(activity);
        boolean showPermissionRationale = shouldShowPermissionRationale(activity, missingPermissions);
        boolean locationEnabled = isLocationEnabled(activity);
        boolean notificationsEnabled = NotificationManagerCompat.from(activity).areNotificationsEnabled();
        boolean needsBatteryOptimizationExemption = needsBatteryOptimizationExemption(activity);
        return Status.create(
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
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", activity.getPackageName(), null));
    }

    @NonNull
    private static List<String> collectMissingPermissions(@NonNull Activity activity) {
        List<String> permissions = new ArrayList<>();
        boolean fineGranted = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        if (!hasAnyLocationPermission(fineGranted, coarseGranted)) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        return permissions;
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

    static boolean hasAnyLocationPermission(boolean fineGranted, boolean coarseGranted) {
        return fineGranted || coarseGranted;
    }

    private static boolean isLocationEnabled(@NonNull Activity activity) {
        LocationManager locationManager = (LocationManager) activity.getSystemService(Activity.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }
        boolean gpsEnabled = false;
        boolean networkEnabled = false;
        try {
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
            return false;
        }
        return gpsEnabled || networkEnabled;
    }

    private static boolean needsBatteryOptimizationExemption(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        PowerManager powerManager = (PowerManager) activity.getSystemService(Activity.POWER_SERVICE);
        return powerManager != null && !powerManager.isIgnoringBatteryOptimizations(activity.getPackageName());
    }
}
