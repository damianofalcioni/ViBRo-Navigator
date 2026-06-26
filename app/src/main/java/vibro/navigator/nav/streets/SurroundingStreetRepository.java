package vibro.navigator.nav.streets;

import android.content.Context;

import androidx.annotation.NonNull;

import vibro.navigator.nav.compass.CompassStreetOverlay;

public interface SurroundingStreetRepository {
    @NonNull
    CompassStreetOverlay loadSurroundingStreets(
            @NonNull Context context,
            double latitude,
            double longitude,
            double radiusMeters,
            int maxSegments
    );
}
