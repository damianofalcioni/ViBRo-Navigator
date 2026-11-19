package vibro.navigator.nav.streets;

import android.content.Context;

import androidx.annotation.NonNull;

import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetVisibility;

public interface SurroundingStreetRepository {
    final class ReadUnavailable extends RuntimeException { }

    @NonNull
    CompassStreetOverlay loadSurroundingStreets(
            @NonNull Context context,
            double latitude,
            double longitude,
            double radiusMeters,
            int maxSegments,
            @NonNull CompassStreetVisibility visibility
    );
}
