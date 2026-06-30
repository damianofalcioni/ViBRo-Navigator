package vibro.navigator.nav.ui;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import vibro.navigator.nav.startup.NavigationPreflight;
import vibro.navigator.settings.AppCompassSettings;

final class NavigationActivityPermissionResultHandler {
    private NavigationActivityPermissionResultHandler() {
    }

    static boolean disableSurroundingStreetsWhenStorageDenied(
            @NonNull Context context,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        for (int i = 0; i < permissions.length; i++) {
            if (isDeniedStoragePermission(permissions, grantResults, i)) {
                AppCompassSettings.setSurroundingStreetsEnabled(context, false);
                return true;
            }
        }
        return false;
    }

    private static boolean isDeniedStoragePermission(
            @NonNull String[] permissions,
            @NonNull int[] grantResults,
            int index
    ) {
        return NavigationPreflight.PERMISSION_READ_EXTERNAL_STORAGE.equals(permissions[index])
                && (index >= grantResults.length || grantResults[index] != PackageManager.PERMISSION_GRANTED);
    }
}
