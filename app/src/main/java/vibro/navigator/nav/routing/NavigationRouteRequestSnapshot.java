package vibro.navigator.nav.routing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.LatLon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NavigationRouteRequestSnapshot {
    public final int requestNumber;
    public final int requestToken;
    @NonNull
    public final LatLon start;
    @NonNull
    public final List<LatLon> intermediates;
    @Nullable
    public final LatLon destination;
    @Nullable
    public final String profile;
    @NonNull
    public final List<NogoPoint> blocked;

    public NavigationRouteRequestSnapshot(
            int requestNumber,
            int requestToken,
            @NonNull LatLon start,
            @NonNull List<LatLon> intermediates,
            @Nullable LatLon destination,
            @Nullable String profile,
            @NonNull List<NogoPoint> blocked
    ) {
        this.requestNumber = requestNumber;
        this.requestToken = requestToken;
        this.start = start;
        this.intermediates = immutableCopy(intermediates);
        this.destination = destination;
        this.profile = profile;
        this.blocked = immutableCopy(blocked);
    }

    @NonNull
    private static <T> List<T> immutableCopy(@NonNull List<T> values) {
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
