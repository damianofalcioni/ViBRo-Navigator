package vibro.navigator.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class BRouterCustomProfileSource {
    private BRouterCustomProfileSource() {
    }

    @Nullable
    static String readText(
            @NonNull Context context,
            @NonNull BRouterProfileDependencies.DocumentAccess documentAccess,
            @Nullable Uri customUri,
            @Nullable String customProfileName,
            @Nullable String requestedProfileName
    ) {
        String cleanRequested = BRouterProfileNames.clean(requestedProfileName);
        String cleanCustom = BRouterProfileNames.clean(customProfileName);
        if (cleanRequested == null || cleanCustom == null || customUri == null) {
            return null;
        }
        if (!cleanRequested.equals(cleanCustom)) {
            return null;
        }
        return documentAccess.readText(context, customUri);
    }
}
