package vibro.navigator.nav.simulation;

import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;
import vibro.navigator.geo.LatLonTextParser;

/** Persisted speed uses SI units, independently of the selected display units. */
public final class NavigationSimulationConfig {
    public static final float DEFAULT_SPEED_MPS = 50f / 3.6f;
    public static final float MAX_SPEED_MPS = 300f / 3.6f;
    public final float speedMps;
    @Nullable public final LatLon initialPosition;

    public NavigationSimulationConfig(float speedMps, @Nullable LatLon initialPosition) {
        this.speedMps = validSpeed(speedMps) ? speedMps : DEFAULT_SPEED_MPS;
        this.initialPosition = initialPosition;
    }

    public static boolean validSpeed(float speedMps) {
        return Float.isFinite(speedMps) && speedMps >= 0 && speedMps <= MAX_SPEED_MPS;
    }

    @Nullable
    public static LatLon parsePosition(String text) {
        String trimmed = text.trim();
        LatLonTextParser.Match match = LatLonTextParser.find(trimmed);
        if (match == null || !trimmed.matches("[+-]?\\d{1,2}(?:\\.\\d+)?\\s*,\\s*[+-]?\\d{1,3}(?:\\.\\d+)?")) {
            return null;
        }
        return match.latLon;
    }
}
