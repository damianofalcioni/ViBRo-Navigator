package vibro.navigator.nav.streets;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.compass.CompassStreetOverlay;

final class SurroundingStreetChunkLoader implements SurroundingStreetOverlayRuntime.ChunkLoader {
    private static final String TAG = "SurroundingStreets";
    private static final int MAX_STREET_SEGMENTS_PER_CHUNK = 450;

    @NonNull
    private final Context appContext;
    @NonNull
    private final SurroundingStreetRepository repository;

    SurroundingStreetChunkLoader(
            @NonNull Context appContext,
            @NonNull SurroundingStreetRepository repository
    ) {
        this.appContext = appContext;
        this.repository = repository;
    }

    @NonNull
    @Override
    public SurroundingStreetChunkLoadResult load(@NonNull List<SurroundingStreetChunkKey> keys) {
        SurroundingStreetChunkLoadResult result = new SurroundingStreetChunkLoadResult();
        for (SurroundingStreetChunkKey key : keys) {
            result.put(key, loadChunk(key));
        }
        return result;
    }

    @NonNull
    private CompassStreetOverlay loadChunk(@NonNull SurroundingStreetChunkKey key) {
        LatLon center = key.center();
        try {
            return repository.loadSurroundingStreets(
                    appContext,
                    center.lat,
                    center.lon,
                    SurroundingStreetChunkKey.LOAD_RADIUS_METERS,
                    MAX_STREET_SEGMENTS_PER_CHUNK
            );
        } catch (RuntimeException e) {
            AppLogger.w(TAG, "Failed to load surrounding street chunk", e);
            return CompassStreetOverlay.EMPTY;
        }
    }
}
