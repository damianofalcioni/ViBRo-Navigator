package vibro.navigator.android.startup;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.android.location.AndroidLocationPermissions;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;

/** Runtime grants used by navigation, speech input, and legacy file output. */
final class AndroidFirstRunRuntimePermissions {
    private AndroidFirstRunRuntimePermissions() { }

    static String[] missing(Activity activity) {
        List<String> permissions = new ArrayList<>();
        if (!AndroidLocationPermissions.hasAnyLocationPermission(activity)) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        addIfMissing(activity, permissions, Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addIfMissing(activity, permissions, Manifest.permission.POST_NOTIFICATIONS);
        }
        if (AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant()) {
            addIfMissing(activity, permissions, Manifest.permission.READ_EXTERNAL_STORAGE);
            addIfMissing(activity, permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        return permissions.toArray(new String[0]);
    }

    private static void addIfMissing(Activity activity, List<String> permissions, String permission) {
        if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(permission);
        }
    }
}
