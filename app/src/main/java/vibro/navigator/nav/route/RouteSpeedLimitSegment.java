package vibro.navigator.nav.route;

import androidx.annotation.NonNull;

public final class RouteSpeedLimitSegment {
    public final double startMeters;
    public final double endMeters;
    @NonNull
    public final RouteSpeedLimit speedLimit;

    public RouteSpeedLimitSegment(
            double startMeters,
            double endMeters,
            @NonNull RouteSpeedLimit speedLimit
    ) {
        this.startMeters = startMeters;
        this.endMeters = endMeters;
        this.speedLimit = speedLimit;
    }

    public boolean contains(double alongTrackMeters) {
        return alongTrackMeters >= startMeters && alongTrackMeters <= endMeters;
    }
}
