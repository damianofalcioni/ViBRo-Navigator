package vibro.navigator.android.storage;

import android.content.Context;
import android.content.UriPermission;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;
import java.util.HashSet;

public final class AndroidPersistedUriPermissions {
    private AndroidPersistedUriPermissions() {
    }

    public static boolean hasReadPermission(@NonNull Context context, @Nullable Uri uri) {
        if (uri == null) {
            return false;
        }
        for (UriPermission permission : context.getContentResolver().getPersistedUriPermissions()) {
            if (permission.isReadPermission() && uri.equals(permission.getUri())) {
                return true;
            }
        }
        return false;
    }

    public static void retainReadable(@NonNull Context context, @NonNull Set<Uri> candidates) {
        if (candidates.isEmpty()) {
            return;
        }
        var readable = new HashSet<Uri>();
        for (UriPermission permission : context.getContentResolver().getPersistedUriPermissions()) {
            if (permission.isReadPermission()) {
                readable.add(permission.getUri());
            }
        }
        candidates.retainAll(readable);
    }
}
