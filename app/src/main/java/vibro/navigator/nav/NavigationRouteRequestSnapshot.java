package vibro.navigator.nav;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.LatLon;

import java.util.List;

final class NavigationRouteRequestSnapshot {
    final int requestNumber;
    final int requestToken;
    @NonNull
    final LatLon start;
    @NonNull
    final List<LatLon> intermediates;
    @Nullable
    final LatLon destination;
    @Nullable
    final String profile;
    @NonNull
    final List<NogoPoint> blocked;

    NavigationRouteRequestSnapshot(
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
        this.intermediates = intermediates;
        this.destination = destination;
        this.profile = profile;
        this.blocked = blocked;
    }
}
