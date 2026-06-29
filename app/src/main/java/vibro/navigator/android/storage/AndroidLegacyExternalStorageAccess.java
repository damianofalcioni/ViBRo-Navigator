package vibro.navigator.android.storage;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public final class AndroidLegacyExternalStorageAccess {
    private AndroidLegacyExternalStorageAccess() {
    }

    public static boolean isRuntimeReadPermissionRelevant() {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.P;
    }

    public static boolean hasReadPermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean shouldRequestReadPermission(@NonNull Context context) {
        return isRuntimeReadPermissionRelevant() && !hasReadPermission(context);
    }

    public static boolean canReadSharedExternalFiles(@NonNull Context context) {
        return isRuntimeReadPermissionRelevant() && hasReadPermission(context);
    }

    public static void requestReadPermission(@NonNull Activity activity, int requestCode) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                requestCode
        );
    }

    public static boolean isReadPermissionGranted(@NonNull int[] grantResults) {
        return grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }
}
