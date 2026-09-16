package vibro.navigator.nav.streets;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.List;

import vibro.navigator.settings.AppCompassSettings;
import vibro.navigator.settings.AppCompassStreetTypeSettings;
import vibro.navigator.nav.compass.CompassStreetVisibility;

final class SurroundingStreetOverlayRuntime {
    interface ChunkLoader {
        @NonNull
        SurroundingStreetChunkLoadResult load(
                @NonNull List<SurroundingStreetChunkKey> keys,
                @NonNull CompassStreetVisibility visibility
        );
    }

    interface Settings {
        boolean isSurroundingStreetsEnabled();

        @NonNull
        CompassStreetVisibility streetVisibility();
    }

    @NonNull
    private final ChunkLoader chunkLoader;
    @NonNull
    private final Settings settings;
    @NonNull
    private volatile CompassStreetVisibility activeVisibility = CompassStreetVisibility.all();

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
                new Settings() {
                    @Override
                    public boolean isSurroundingStreetsEnabled() {
                        return AppCompassSettings.isSurroundingStreetsEnabled(appContext);
                    }

                    @Override
                    public CompassStreetVisibility streetVisibility() {
                        return AppCompassStreetTypeSettings.get(appContext);
                    }
                }
        );
    }

    boolean isSurroundingStreetsEnabled() {
        return settings.isSurroundingStreetsEnabled();
    }

    void refreshSelection(@NonNull Runnable onChanged) {
        CompassStreetVisibility selected = settings.streetVisibility();
        if (!activeVisibility.equals(selected)) {
            activeVisibility = selected;
            onChanged.run();
        }
    }

    @NonNull
    SurroundingStreetChunkLoadResult load(@NonNull List<SurroundingStreetChunkKey> keys) {
        return chunkLoader.load(keys, activeVisibility);
    }
}
