package vibro.navigator.nav.location;

import androidx.annotation.Nullable;

final class LiveLocationFix {
    @Nullable
    final String provider;
    final long timeMs;
    final float accuracyMeters;
    final double lat;
    final double lon;

    LiveLocationFix(@Nullable String provider, long timeMs, float accuracyMeters, double lat, double lon) {
        this.provider = provider;
        this.timeMs = timeMs;
        this.accuracyMeters = accuracyMeters;
        this.lat = lat;
        this.lon = lon;
    }
}
