package vibro.navigator.nav.simulation;

import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;

/** Current domain-owned target and arrival barrier; the simulator never advances them itself. */
public final class NavigationSimulationPath {
    @Nullable public final GeoJsonRoute route;
    @Nullable public final LatLon directTarget;
    @Nullable public final NavigationSimulationBarrier barrier;
    public final boolean reached;
    public final int minimumRouteSegmentIndex;

    public NavigationSimulationPath(@Nullable GeoJsonRoute route, @Nullable LatLon directTarget,
            @Nullable NavigationSimulationBarrier barrier, boolean reached) {
        this(route, directTarget, barrier, reached, -1);
    }

    public NavigationSimulationPath(@Nullable GeoJsonRoute route, @Nullable LatLon directTarget,
            @Nullable NavigationSimulationBarrier barrier, boolean reached, int minimumRouteSegmentIndex) {
        this.route = route;
        this.directTarget = directTarget;
        this.barrier = barrier;
        this.reached = reached;
        this.minimumRouteSegmentIndex = minimumRouteSegmentIndex;
    }
}
