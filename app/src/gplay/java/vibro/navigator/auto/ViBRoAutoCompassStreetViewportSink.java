package vibro.navigator.auto;

import androidx.annotation.Nullable;

import vibro.navigator.nav.compass.NavCompassState;

interface ViBRoAutoCompassStreetViewportSink {
    void onCompassStreetViewport(@Nullable NavCompassState compassState);

    default void clearCompassStreetViewport() {
        onCompassStreetViewport(null);
    }
}
