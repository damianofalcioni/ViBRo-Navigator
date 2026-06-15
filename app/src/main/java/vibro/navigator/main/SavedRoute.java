package vibro.navigator.main;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.poi.Poi;

final class SavedRoute {
    @NonNull
    final String id;
    @NonNull
    final String name;
    @NonNull
    final Poi destination;
    @NonNull
    final List<Poi> stops;
    final long createdAtMillis;

    SavedRoute(
            @NonNull String id,
            @NonNull String name,
            @NonNull Poi destination,
            @NonNull List<Poi> stops,
            long createdAtMillis
    ) {
        this.id = id;
        this.name = name;
        this.destination = destination;
        this.stops = Collections.unmodifiableList(new ArrayList<>(stops));
        this.createdAtMillis = createdAtMillis;
    }

    @NonNull
    SavedRoute renamed(@NonNull String updatedName) {
        return new SavedRoute(id, updatedName, destination, stops, createdAtMillis);
    }
}
