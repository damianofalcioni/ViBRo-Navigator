package vibro.navigator.intent;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.poi.Poi;

/** A navigation form route derived from GPX route-form points or track geometry. */
public final class GpxWaypointRoute {
    @NonNull
    public final Poi destination;
    @NonNull
    public final List<Poi> stops;

    private GpxWaypointRoute(@NonNull Poi destination, @NonNull List<Poi> stops) {
        this.destination = destination;
        this.stops = Collections.unmodifiableList(new ArrayList<>(stops));
    }

    @NonNull
    static GpxWaypointRoute fromWaypoints(@NonNull List<Poi> waypoints) {
        if (waypoints.isEmpty()) {
            throw new IllegalArgumentException("GPX document has no valid route points");
        }
        int destinationIndex = waypoints.size() - 1;
        return new GpxWaypointRoute(
                waypoints.get(destinationIndex),
                waypoints.subList(0, destinationIndex)
        );
    }

    @NonNull
    static GpxWaypointRoute fromTrackPoints(@NonNull List<Poi> trackPoints) {
        if (trackPoints.isEmpty()) {
            throw new IllegalArgumentException("GPX document has no valid track points");
        }
        return new GpxWaypointRoute(
                trackPoints.get(trackPoints.size() - 1),
                Collections.emptyList()
        );
    }
}
