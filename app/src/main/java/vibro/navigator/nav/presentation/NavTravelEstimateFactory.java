package vibro.navigator.nav.presentation;

import vibro.navigator.nav.guidance.RouteTimeEstimator;
import vibro.navigator.nav.model.NavTravelEstimate;

final class NavTravelEstimateFactory {
    private NavTravelEstimateFactory() { }

    static NavTravelEstimate from(NavStateBuildInput input) {
        if (input.destinationReached) {
            return new NavTravelEstimate(0, 0, input.timing.nowMs, true);
        }
        double distance = Math.max(0,
                input.index.totalLengthMeters() - input.routeProgress.alongTrackMeters);
        Double seconds = RouteTimeEstimator.estimateSecondsToAlongTrack(
                input.route, input.index, input.routeProgress.alongTrackMeters,
                input.routeProgress.currentSegmentIndex, input.index.totalLengthMeters(), input.motion.etaSpeedMps);
        return new NavTravelEstimate(distance, seconds == null ? Double.NaN : seconds,
                input.timing.nowMs, false);
    }
}
