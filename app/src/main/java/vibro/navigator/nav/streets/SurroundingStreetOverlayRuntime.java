package vibro.navigator.nav.streets;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.List;

import vibro.navigator.settings.AppCompassSettings;

final class SurroundingStreetOverlayRuntime {
    interface ChunkLoader {
        @NonNull
        SurroundingStreetChunkLoadResult load(@NonNull List<SurroundingStreetChunkKey> keys);
    }

    interface Settings {
        boolean isSurroundingStreetsEnabled();
    }

    @NonNull
    private final ChunkLoader chunkLoader;
    @NonNull
    private final Settings settings;

    SurroundingStreetOverlayRuntime(
            @NonNull ChunkLoader chunkLoader,
            @NonNull Settings settings
    ) {
        this.chunkLoader = chunkLoader;
        this.settings = settings;
    }

    @NonNull
    static SurroundingStreetOverlayRuntime android(
            @NonNull Context context,
            @NonNull SurroundingStreetRepository repository
    ) {
        Context appContext = context.getApplicationContext();
        return new SurroundingStreetOverlayRuntime(
                new SurroundingStreetChunkLoader(appContext, repository),
                () -> AppCompassSettings.isSurroundingStreetsEnabled(appContext)
        );
    }

    boolean isSurroundingStreetsEnabled() {
        return settings.isSurroundingStreetsEnabled();
    }

    @NonNull
    SurroundingStreetChunkLoadResult load(@NonNull List<SurroundingStreetChunkKey> keys) {
        return chunkLoader.load(keys);
    }
}
