package vibro.navigator.auto;

import androidx.annotation.Nullable;
import androidx.car.app.model.DateTimeWithZone;
import androidx.car.app.navigation.model.MessageInfo;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.navigation.model.RoutingInfo;
import androidx.car.app.navigation.model.TravelEstimate;

import java.util.TimeZone;

import vibro.navigator.R;
import vibro.navigator.nav.format.NavigationMeasurementFormatter;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavManeuver;
import vibro.navigator.nav.model.NavProgressStatus;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavTravelEstimate;

final class ViBRoCarGuidance {
    private ViBRoCarGuidance() { }

    static void apply(NavigationTemplate.Builder builder, NavState state, NavigationTextResources resources) {
        NavGuidanceStatus guidance = state.routeStatus.guidance;
        boolean paused = state.pauseStatus.paused;
        boolean imperial = resources.isImperialUnitsEnabled();
        NavProgressStatus progress = state.routeStatus.progress;
        TravelEstimate estimate = travelEstimate(progress.travelEstimate, imperial);
        String detail = detail(progress, estimate, paused, resources);
        if (!paused && detail.isEmpty() && !guidance.maneuvers.isEmpty()) {
            builder.setNavigationInfo(routingInfo(guidance, imperial));
        } else {
            builder.setNavigationInfo(messageInfo(guidance, detail, paused));
        }
        if (estimate != null && !paused) {
            builder.setDestinationTravelEstimate(estimate);
        }
    }

    private static MessageInfo messageInfo(NavGuidanceStatus guidance, String detail, boolean paused) {
        String title = ViBRoCarManeuvers.cue(guidance.nextLine, guidance.nextUncertain);
        String text = detail;
        if (!paused && !guidance.afterNextLine.isEmpty()) {
            text += "\n" + ViBRoCarManeuvers.cue(guidance.afterNextLine, guidance.afterNextUncertain);
        }
        return new MessageInfo.Builder(title).setText(text).build();
    }

    private static RoutingInfo routingInfo(NavGuidanceStatus guidance, boolean imperial) {
        NavManeuver next = guidance.maneuvers.get(0);
        RoutingInfo.Builder routing = new RoutingInfo.Builder()
                .setCurrentStep(ViBRoCarManeuvers.step(next, guidance.nextUncertain),
                        ViBRoCarManeuvers.distance(next.distanceMeters, imperial));
        if (guidance.maneuvers.size() > 1) {
            routing.setNextStep(ViBRoCarManeuvers.step(guidance.maneuvers.get(1), guidance.afterNextUncertain));
        }
        return routing.build();
    }

    private static String detail(NavProgressStatus progress, @Nullable TravelEstimate estimate,
            boolean paused, NavigationTextResources resources) {
        NavTravelEstimate destination = progress.travelEstimate;
        if (paused || estimate != null || destination == null || !Double.isFinite(destination.distanceMeters)) {
            return progress.detailBlock;
        }
        // The host's TravelEstimate requires an arrival timestamp. Display distance in the
        // guidance card while there is no trustworthy time to supply.
        String distance = resources.getString(R.string.nav_destination_label) + " "
                + NavigationMeasurementFormatter.formatDistance(resources, destination.distanceMeters);
        return progress.detailBlock.isEmpty() ? distance : progress.detailBlock + "\n" + distance;
    }

    @Nullable
    private static TravelEstimate travelEstimate(@Nullable NavTravelEstimate value, boolean imperial) {
        if (value == null || !Double.isFinite(value.timeSeconds) || value.timeSeconds < 0) {
            return null;
        }
        long seconds = (long) Math.ceil(value.timeSeconds);
        return new TravelEstimate.Builder(ViBRoCarManeuvers.distance(value.distanceMeters, imperial),
                DateTimeWithZone.create(value.observationWallTimeMs + seconds * 1_000L, TimeZone.getDefault()))
                .setRemainingTimeSeconds(seconds).build();
    }
}
