package vibro.navigator.nav.presentation;

import androidx.annotation.Nullable;

import vibro.navigator.nav.guidance.RouteTimeEstimator;
import vibro.navigator.nav.model.NavStopEstimate;
import vibro.navigator.nav.model.NavTarget;
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

    @Nullable
    static NavStopEstimate nextStop(NavStateBuildInput input) {
        if (input.destinationReached) { return null; }
        for (int index = 0; index < input.targets.size() - 1; index++) {
            NavTarget target = input.targets.get(index);
            double distance = target.alongTrackMeters - input.routeProgress.alongTrackMeters;
            if (distance <= 0.0) { continue; }
            Double seconds = RouteTimeEstimator.estimateSecondsToAlongTrack(
                    input.route, input.index, input.routeProgress.alongTrackMeters,
                    input.routeProgress.currentSegmentIndex, target.alongTrackMeters, input.motion.etaSpeedMps);
            return new NavStopEstimate(target.label, distance, seconds);
        }
        return null;
    }
}
