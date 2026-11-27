package vibro.navigator.nav.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Progress to the next required stop, with an optional remaining duration. */
public record NavStopEstimate(@NonNull String label, double distanceMeters, @Nullable Double timeSeconds) { }
