package vibro.navigator.auto;

import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavTravelEstimate;

/** Compare the displayed arrival clocks independently of the observation timestamp. */
record ViBRoCarArrivalMinutes(long destination, long stop) {
    private static final long UNAVAILABLE = Long.MIN_VALUE;

    static ViBRoCarArrivalMinutes from(NavState state) {
        var progress = state.routeStatus.progress;
        NavTravelEstimate estimate = progress.travelEstimate;
        if (state.pauseStatus.paused || estimate == null || estimate.destinationReached) {
            return new ViBRoCarArrivalMinutes(UNAVAILABLE, UNAVAILABLE);
        }
        var nextStop = progress.nextStop;
        return new ViBRoCarArrivalMinutes(
                minute(estimate.observationWallTimeMs, Math.ceil(estimate.timeSeconds)),
                nextStop == null || nextStop.timeSeconds() == null ? UNAVAILABLE
                        : minute(estimate.observationWallTimeMs, nextStop.timeSeconds()));
    }

    private static long minute(long observationTimeMs, double seconds) {
        return Double.isFinite(seconds) && seconds >= 0
                ? Math.floorDiv(observationTimeMs + (long) (seconds * 1_000L), 60_000L) : UNAVAILABLE;
    }
}
