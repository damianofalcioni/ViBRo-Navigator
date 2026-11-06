package vibro.navigator.nav.simulation;

import vibro.navigator.geo.LatLon;

/** Exact required-stop projection and coordinate, including stops between route vertices. */
public final class NavigationSimulationBarrier {
    public final LatLon position;
    public final double alongTrackMeters;

    public NavigationSimulationBarrier(LatLon position, double alongTrackMeters) {
        this.position = position;
        this.alongTrackMeters = alongTrackMeters;
    }
}
