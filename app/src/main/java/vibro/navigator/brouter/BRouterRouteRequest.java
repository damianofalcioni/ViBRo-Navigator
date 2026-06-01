package vibro.navigator.brouter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;

public final class BRouterRouteRequest {
    @NonNull
    public final LatLon start;
    @NonNull
    public final List<LatLon> intermediates;
    @NonNull
    public final LatLon destination;
    @NonNull
    public final String profile;
    @NonNull
    public final List<NogoPoint> blockedWaypoints;

    public BRouterRouteRequest(
            @NonNull LatLon start,
            @Nullable List<LatLon> intermediates,
            @NonNull LatLon destination,
            @NonNull String profile,
            @Nullable List<NogoPoint> blockedWaypoints
    ) {
        this.start = start;
        this.intermediates = immutableCopy(intermediates);
        this.destination = destination;
        this.profile = profile;
        this.blockedWaypoints = immutableCopy(blockedWaypoints);
    }

    @NonNull
    private static <T> List<T> immutableCopy(@Nullable List<T> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(items));
    }
}
