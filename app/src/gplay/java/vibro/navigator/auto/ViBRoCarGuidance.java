package vibro.navigator.auto;

import androidx.annotation.Nullable;
import androidx.car.app.model.DateTimeWithZone;
import androidx.car.app.navigation.model.MessageInfo;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.navigation.model.RoutingInfo;
import androidx.car.app.navigation.model.TravelEstimate;

import java.util.TimeZone;

import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavManeuver;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavTravelEstimate;

final class ViBRoCarGuidance {
    private ViBRoCarGuidance() { }

    static void apply(NavigationTemplate.Builder builder, NavState state) {
        NavGuidanceStatus guidance = state.routeStatus.guidance;
        if (!state.pauseStatus.paused && !guidance.maneuvers.isEmpty()) {
            NavManeuver next = guidance.maneuvers.get(0);
            RoutingInfo.Builder routing = new RoutingInfo.Builder()
                    .setCurrentStep(ViBRoCarManeuvers.step(next), ViBRoCarManeuvers.distance(next.distanceMeters));
            if (guidance.maneuvers.size() > 1) {
                routing.setNextStep(ViBRoCarManeuvers.step(guidance.maneuvers.get(1)));
            }
            builder.setNavigationInfo(routing.build());
        } else {
            String title = guidance.nextLine;
            String detail = state.routeStatus.progress.detailBlock;
            builder.setNavigationInfo(new MessageInfo.Builder(title).setText(detail).build());
        }
        TravelEstimate estimate = travelEstimate(state.routeStatus.progress.travelEstimate);
        if (estimate != null && !state.pauseStatus.paused) {
            builder.setDestinationTravelEstimate(estimate);
        }
    }

    @Nullable
    static TravelEstimate travelEstimate(@Nullable NavTravelEstimate value) {
        if (value == null || !Double.isFinite(value.timeSeconds) || value.timeSeconds < 0) {
            return null;
        }
        long seconds = (long) Math.ceil(value.timeSeconds);
        return new TravelEstimate.Builder(ViBRoCarManeuvers.distance(value.distanceMeters),
                DateTimeWithZone.create(value.observationWallTimeMs + seconds * 1_000L, TimeZone.getDefault()))
                .setRemainingTimeSeconds(seconds).build();
    }
}
