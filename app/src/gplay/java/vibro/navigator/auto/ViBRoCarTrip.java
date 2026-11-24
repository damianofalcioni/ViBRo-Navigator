package vibro.navigator.auto;

import androidx.annotation.Nullable;
import androidx.car.app.model.DateTimeWithZone;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.car.app.navigation.model.Trip;

import java.util.TimeZone;

import vibro.navigator.nav.model.NavManeuver;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavState;

final class ViBRoCarTrip {
    private ViBRoCarTrip() { }

    static Trip from(NavState state, boolean imperial, @Nullable ViBRoCarManeuvers.Icons icons) {
        Trip.Builder trip = new Trip.Builder();
        NavGuidanceStatus guidance = state.routeStatus.guidance;
        if (guidance.maneuvers.isEmpty()) {
            return trip.setLoading(true).build();
        }
        long nowMs = state.routeStatus.progress.travelEstimate == null ? System.currentTimeMillis()
                : state.routeStatus.progress.travelEstimate.observationWallTimeMs;
        for (int index = 0; index < guidance.maneuvers.size(); index++) {
            NavManeuver maneuver = guidance.maneuvers.get(index);
            boolean uncertain = index == 0 ? guidance.nextUncertain : index == 1 && guidance.afterNextUncertain;
            trip.addStep(ViBRoCarManeuvers.step(maneuver, ViBRoCarManeuvers.cue(maneuver.instruction, uncertain),
                    icons), stepEstimate(maneuver, nowMs, imperial));
        }
        return trip.build();
    }

    static boolean sameProgress(Trip current, @Nullable Trip previous) {
        if (previous == null || current.isLoading() != previous.isLoading()
                || !current.getSteps().equals(previous.getSteps())) {
            return false;
        }
        var currentEstimates = current.getStepTravelEstimates();
        var previousEstimates = previous.getStepTravelEstimates();
        for (int index = 0; index < currentEstimates.size(); index++) {
            if (!sameEstimate(currentEstimates.get(index), previousEstimates.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameEstimate(TravelEstimate current, TravelEstimate previous) {
        return current.getRemainingDistance().equals(previous.getRemainingDistance())
                && current.getRemainingTimeSeconds() == previous.getRemainingTimeSeconds();
    }

    private static TravelEstimate stepEstimate(NavManeuver maneuver, long nowMs, boolean imperial) {
        long seconds = Double.isFinite(maneuver.timeSeconds) && maneuver.timeSeconds >= 0
                ? (long) Math.ceil(maneuver.timeSeconds) : TravelEstimate.REMAINING_TIME_UNKNOWN;
        return new TravelEstimate.Builder(ViBRoCarManeuvers.distance(maneuver.distanceMeters, imperial),
                DateTimeWithZone.create(nowMs + Math.max(0, seconds) * 1_000L, TimeZone.getDefault()))
                .setRemainingTimeSeconds(seconds).build();
    }
}
