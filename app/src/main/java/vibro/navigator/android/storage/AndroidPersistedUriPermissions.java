package vibro.navigator.android.storage;

import android.content.Context;
import android.content.UriPermission;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class AndroidPersistedUriPermissions {
    private AndroidPersistedUriPermissions() {
    }

    public static boolean hasReadPermission(@NonNull Context context, @Nullable Uri uri) {
        if (uri == null) {
            return false;
        }
        for (UriPermission permission : context.getContentResolver().getPersistedUriPermissions()) {
            if (!permission.isReadPermission()) {
                continue;
            }
            if (uri.equals(permission.getUri())) {
                return true;
            }
        }
        return false;
    }
}
