package vibro.navigator.poi.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiCoordinateLabel;
import vibro.navigator.poi.search.PoiReverseGeocodingClient;
import vibro.navigator.poi.search.PoiReverseGeocodingClients;

public final class PoiReverseGeocodeController {
    private static final String TAG = "PoiReverseGeocode";

    @NonNull
    private final PoiReverseGeocodingClient client;
    @NonNull
    private final TaskScheduler mainThreadScheduler;
    @NonNull
    private final ReverseGeocodeDispatcher dispatcher;

    private int generation;

    public PoiReverseGeocodeController(
            @NonNull PoiReverseGeocodingClient client,
            @NonNull TaskScheduler mainThreadScheduler
    ) {
        this(client, mainThreadScheduler, runnable -> PoiSearchDispatcher.submit(runnable));
    }

    PoiReverseGeocodeController(
            @NonNull PoiReverseGeocodingClient client,
            @NonNull TaskScheduler mainThreadScheduler,
            @NonNull ReverseGeocodeDispatcher dispatcher
    ) {
        this.client = client;
        this.mainThreadScheduler = mainThreadScheduler;
        this.dispatcher = dispatcher;
    }

    @NonNull
    public static PoiReverseGeocodeController createDefault(@NonNull Context context) {
        return new PoiReverseGeocodeController(
                PoiReverseGeocodingClients.createDefault(context),
                AndroidTaskScheduler.main()
        );
    }

    @NonNull
    public static PoiReverseGeocodeController disabled() {
        return new PoiReverseGeocodeController(
                (lat, lon) -> null,
                runnable -> {
                },
                runnable -> {
                }
        );
    }

    public void setPoiAndResolveAddress(
            @NonNull PoiInputController inputController,
            @NonNull Poi poi
    ) {
        inputController.setPoi(poi);
        if (!shouldResolveAddress(poi)) {
            AppLogger.d(TAG, "Skipping reverse geocoding for named POI=" + poi.displayLabel());
            return;
        }
        int requestGeneration = generation;
        dispatcher.submit(() -> resolveAddress(inputController, poi, requestGeneration));
    }

    public void dispose() {
        generation++;
    }

    private void resolveAddress(
            @NonNull PoiInputController inputController,
            @NonNull Poi poi,
            int requestGeneration
    ) {
        try {
            String address = client.reverseGeocode(poi.lat, poi.lon);
            mainThreadScheduler.post(() -> applyAddress(inputController, poi, requestGeneration, address));
        } catch (IOException e) {
            AppLogger.w(TAG, "Reverse geocoding failed for " + poi.stableKey(), e);
        }
    }

    private void applyAddress(
            @NonNull PoiInputController inputController,
            @NonNull Poi poi,
            int requestGeneration,
            @Nullable String address
    ) {
        if (requestGeneration != generation) {
            AppLogger.d(TAG, "Discarding reverse geocode result after dispose");
            return;
        }
        if (address == null || address.trim().isEmpty()) {
            AppLogger.d(TAG, "Reverse geocoding returned no address for " + poi.stableKey());
            return;
        }
        boolean applied = inputController.replaceSelectedPoiNameIfSameCoordinates(poi, address.trim());
        AppLogger.d(TAG, "Reverse geocode address applied=" + applied + " key=" + poi.stableKey());
    }

    private static boolean shouldResolveAddress(@NonNull Poi poi) {
        return poi.hasValidCoordinates() && PoiCoordinateLabel.isCoordinateLabel(poi);
    }

    interface ReverseGeocodeDispatcher {
        void submit(@NonNull Runnable runnable);
    }
}
