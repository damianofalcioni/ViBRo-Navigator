package vibro.navigator.main;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.poi.Poi;

final class SavedRoutePoints {
    @NonNull
    final Poi destination;
    @NonNull
    final List<Poi> stops;

    SavedRoutePoints(@NonNull Poi destination, @NonNull List<Poi> stops) {
        this.destination = destination;
        this.stops = Collections.unmodifiableList(new ArrayList<>(stops));
    }
}
