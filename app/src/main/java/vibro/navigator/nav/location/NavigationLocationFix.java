package vibro.navigator.nav.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NavigationLocationFix {
    @Nullable
    public final String provider;
    public final long timeMs;
    public final float accuracyMeters;
    public final double lat;
    public final double lon;

    public NavigationLocationFix(
            @Nullable String provider,
            long timeMs,
            float accuracyMeters,
            double lat,
            double lon
    ) {
        this.provider = provider;
        this.timeMs = timeMs;
        this.accuracyMeters = accuracyMeters;
        this.lat = lat;
        this.lon = lon;
    }

    @NonNull
    public static NavigationLocationFix qualityOnly(
            @Nullable String provider,
            long timeMs,
            float accuracyMeters
    ) {
        return new NavigationLocationFix(provider, timeMs, accuracyMeters, 0.0, 0.0);
    }

    @Nullable
    public static NavigationLocationFix from(@Nullable NavigationLocation location) {
        if (location == null) {
            return null;
        }
        return new NavigationLocationFix(
                location.getProvider(),
                location.getTime(),
                location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE,
                location.getLatitude(),
                location.getLongitude()
        );
    }
}
