package vibro.navigator.nav.session;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;

final class NavigationRouteResultInput {
    @NonNull
    public final Context context;
    @NonNull
    public final NavigationRequest request;
    @NonNull
    public final NavigationRouteRequestSnapshot snapshot;
    @NonNull
    public final GeoJsonRoute route;
    @Nullable
    public final Location lastFiltered;
    public final float speedMps;
    public final boolean likelyStationary;
    public final long beganAt;

    NavigationRouteResultInput(
            @NonNull Context context,
            @NonNull NavigationRequest request,
            @NonNull NavigationRouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute route,
            @Nullable Location lastFiltered,
            float speedMps,
            boolean likelyStationary,
            long beganAt
    ) {
        this.context = context;
        this.request = request;
        this.snapshot = snapshot;
        this.route = route;
        this.lastFiltered = lastFiltered;
        this.speedMps = speedMps;
        this.likelyStationary = likelyStationary;
        this.beganAt = beganAt;
    }
}
