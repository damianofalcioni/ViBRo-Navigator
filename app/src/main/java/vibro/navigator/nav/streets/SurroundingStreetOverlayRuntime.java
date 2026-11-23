package vibro.navigator.nav.streets;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.util.Supplier;

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
    private ChunkLoader chunkLoader;
    @NonNull
    private final Supplier<ChunkLoader> newSource;
    @NonNull
    private final Settings settings;
    @NonNull
    private CompassStreetVisibility activeVisibility = CompassStreetVisibility.all();

    SurroundingStreetOverlayRuntime(
            @NonNull ChunkLoader chunkLoader,
            @NonNull Settings settings
    ) {
        this(() -> chunkLoader, settings);
    }

    private SurroundingStreetOverlayRuntime(@NonNull Supplier<ChunkLoader> newSource, @NonNull Settings settings) {
        this.newSource = newSource;
        chunkLoader = newSource.get();
        this.settings = settings;
    }

    @NonNull
    static SurroundingStreetOverlayRuntime android(
            @NonNull Context context,
            @NonNull Supplier<SurroundingStreetRepository> repository
    ) {
        Context appContext = context.getApplicationContext();
        return new SurroundingStreetOverlayRuntime(
                () -> new SurroundingStreetChunkLoader(appContext, repository.get()),
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
    Supplier<SurroundingStreetChunkLoadResult> request(@NonNull List<SurroundingStreetChunkKey> keys) {
        ChunkLoader loader = chunkLoader;
        CompassStreetVisibility visibility = activeVisibility;
        return () -> loader.load(keys, visibility);
    }

    void changeSource() {
        chunkLoader = newSource.get();
    }
}
