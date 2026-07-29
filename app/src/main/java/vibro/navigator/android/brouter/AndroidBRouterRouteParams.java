package vibro.navigator.android.brouter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import vibro.navigator.brouter.BRouterRouteRequest;

final class AndroidBRouterRouteParams {
    private AndroidBRouterRouteParams() {
    }

    @NonNull
    static Bundle build(@NonNull BRouterRouteRequest request) {
        return build(request, null);
    }

    @NonNull
    static Bundle build(@NonNull BRouterRouteRequest request, @Nullable String remoteProfile) {
        AndroidBRouterRouteParameterValues values = AndroidBRouterRouteParameterValues.build(request, remoteProfile);
        Bundle bundle = new Bundle();
        for (Map.Entry<String, Object> entry : values.entries()) {
            putValue(bundle, entry);
        }
        return bundle;
    }

    private static void putValue(
            @NonNull Bundle bundle,
            @NonNull Map.Entry<String, Object> entry
    ) {
        Object value = entry.getValue();
        if (value instanceof String) {
            bundle.putString(entry.getKey(), (String) value);
            return;
        }
        if (value instanceof Integer) {
            bundle.putInt(entry.getKey(), (Integer) value);
            return;
        }
        if (value instanceof double[]) {
            bundle.putDoubleArray(entry.getKey(), (double[]) value);
            return;
        }
        throw new IllegalArgumentException(
                "Unsupported BRouter route parameter type: " + value.getClass().getName()
        );
    }
}
