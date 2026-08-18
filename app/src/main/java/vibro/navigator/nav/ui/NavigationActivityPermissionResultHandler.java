package vibro.navigator.nav.ui;

import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import vibro.navigator.R;
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
        return disableSurroundingStreetsWhenStorageDenied(
                permissions,
                grantResults,
                () -> AppCompassSettings.setSurroundingStreetsEnabled(context, false)
        );
    }

    static void disableSurroundingStreetsWhenStorageDeniedAndToast(
            @NonNull Context context,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        if (disableSurroundingStreetsWhenStorageDenied(context, permissions, grantResults)) {
            Toast.makeText(
                    context,
                    R.string.msg_compass_surrounding_streets_storage_permission_required,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    static boolean disableSurroundingStreetsWhenStorageDenied(
            @NonNull String[] permissions,
            @NonNull int[] grantResults,
            @NonNull Runnable disableSurroundingStreets
    ) {
        for (int i = 0; i < permissions.length; i++) {
            if (isDeniedStoragePermission(permissions, grantResults, i)) {
                disableSurroundingStreets.run();
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
