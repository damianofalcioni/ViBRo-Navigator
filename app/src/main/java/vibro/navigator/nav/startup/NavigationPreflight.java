package vibro.navigator.nav.startup;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NavigationPreflight {

    public static final String PERMISSION_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION";
    public static final String PERMISSION_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION";
    public static final String PERMISSION_POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";
    public static final String PERMISSION_READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";

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

        public boolean hasLocationPermission() {
            return !missingPermissions.contains(PERMISSION_FINE_LOCATION)
                    && !missingPermissions.contains(PERMISSION_COARSE_LOCATION);
        }

        public boolean hasNotificationAccess() {
            return notificationsEnabled && !missingPermissions.contains(PERMISSION_POST_NOTIFICATIONS);
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

    public static boolean hasAnyLocationPermission(boolean fineGranted, boolean coarseGranted) {
        return fineGranted || coarseGranted;
    }
}
