package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;

public final class CompassStreetSegment {
    @NonNull
    public final List<LatLon> points;

    public CompassStreetSegment(@NonNull List<LatLon> points) {
        this.points = Collections.unmodifiableList(new ArrayList<>(points));
    }
}
