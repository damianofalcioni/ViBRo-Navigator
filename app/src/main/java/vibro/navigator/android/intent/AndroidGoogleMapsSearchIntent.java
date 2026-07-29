package vibro.navigator.android.intent;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

public final class AndroidGoogleMapsSearchIntent {
    static final String GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps";

    private AndroidGoogleMapsSearchIntent() {
    }

    @NonNull
    public static Intent createAppIntent(@NonNull String query) {
        Intent intent = createWebIntent(query);
        intent.setPackage(GOOGLE_MAPS_PACKAGE);
        return intent;
    }

    @NonNull
    public static Intent createWebIntent(@NonNull String query) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(uriString(query)));
    }

    @NonNull
    static String uriString(@NonNull String query) {
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Google Maps search query must not be blank");
        }
        return "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(trimmed);
    }
}
