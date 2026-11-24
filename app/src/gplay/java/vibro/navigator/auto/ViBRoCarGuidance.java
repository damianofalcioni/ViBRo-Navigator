package vibro.navigator.auto;

import android.text.SpannableStringBuilder;

import androidx.annotation.Nullable;
import androidx.car.app.model.DateTimeWithZone;
import androidx.car.app.navigation.model.MessageInfo;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.navigation.model.RoutingInfo;
import androidx.car.app.navigation.model.Step;
import androidx.car.app.navigation.model.TravelEstimate;

import java.util.TimeZone;

import vibro.navigator.R;
import vibro.navigator.nav.format.NavStateTextFactory;
import vibro.navigator.nav.format.NavigationMeasurementFormatter;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavManeuver;
import vibro.navigator.nav.model.NavProgressStatus;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavTravelEstimate;

final class ViBRoCarGuidance {
    private ViBRoCarGuidance() { }

    static void apply(NavigationTemplate.Builder builder, NavState state, NavigationTextResources resources,
            long observationTimeMs, ViBRoCarManeuvers.Icons icons) {
        NavGuidanceStatus guidance = state.routeStatus.guidance;
        NavProgressStatus progress = state.routeStatus.progress;
        if (state.pauseStatus.paused || hasArrived(progress.travelEstimate)) {
            builder.setNavigationInfo(messageInfo(guidance, progress.detailBlock, false));
            return;
        }

        TravelEstimate estimate = travelEstimate(progress.travelEstimate, resources.isImperialUnitsEnabled(),
                observationTimeMs);
        String detail = appendDetail(detail(progress, estimate, resources), stopProgress(progress, resources,
                observationTimeMs));
        if (!guidance.maneuvers.isEmpty()) {
            builder.setNavigationInfo(routingInfo(guidance, detail, resources, icons));
        } else {
            builder.setNavigationInfo(messageInfo(guidance, detail, true));
        }
        if (estimate != null) {
            builder.setDestinationTravelEstimate(estimate);
        }
    }

    private static boolean hasArrived(@Nullable NavTravelEstimate destination) {
        return destination != null && destination.destinationReached;
    }

    private static MessageInfo messageInfo(NavGuidanceStatus guidance, String detail, boolean showFollowing) {
        String title = ViBRoCarManeuvers.cue(guidance.nextLine, guidance.nextUncertain);
        String text = detail;
        if (showFollowing && !guidance.afterNextLine.isEmpty()) {
            text += "\n" + ViBRoCarManeuvers.cue(guidance.afterNextLine, guidance.afterNextUncertain);
        }
        return new MessageInfo.Builder(title).setText(text).build();
    }

    private static RoutingInfo routingInfo(NavGuidanceStatus guidance, String detail,
            NavigationTextResources resources, ViBRoCarManeuvers.Icons icons) {
        NavManeuver next = guidance.maneuvers.get(0);
        var cue = new SpannableStringBuilder(ViBRoCarManeuvers.cue(next.instruction, guidance.nextUncertain));
        ViBRoCarManeuvers.appendDuration(cue, next.timeSeconds, resources);
        if (!detail.isEmpty()) {
            cue.append('\n').append(detail);
        }
        RoutingInfo.Builder routing = new RoutingInfo.Builder()
                .setCurrentStep(ViBRoCarManeuvers.step(next, cue, icons),
                        ViBRoCarManeuvers.distance(next.distanceMeters, resources.isImperialUnitsEnabled()));
        if (guidance.maneuvers.size() > 1) {
            routing.setNextStep(ViBRoCarManeuvers.followingStep(guidance, resources, icons));
        } else if (!guidance.afterNextLine.isEmpty()) {
            // Direct guidance keeps the following road turn as a preview while the beeline owns cluster guidance.
            routing.setNextStep(new Step.Builder(ViBRoCarManeuvers.cue(
                    guidance.afterNextLine, guidance.afterNextUncertain)).build());
        }
        return routing.build();
    }

    private static String detail(NavProgressStatus progress, @Nullable TravelEstimate estimate,
            NavigationTextResources resources) {
        NavTravelEstimate destination = progress.travelEstimate;
        if (estimate != null || destination == null || !Double.isFinite(destination.distanceMeters)) {
            return progress.detailBlock;
        }
        // The host's TravelEstimate requires an arrival timestamp. Display distance in the
        // guidance card while there is no trustworthy time to supply.
        String distance = resources.getString(R.string.nav_destination_label) + " "
                + NavigationMeasurementFormatter.formatDistance(resources, destination.distanceMeters);
        return appendDetail(progress.detailBlock, distance);
    }

    private static String stopProgress(NavProgressStatus progress, NavigationTextResources resources, long nowMs) {
        return progress.nextStop == null ? progress.stopProgressBlock
                : NavStateTextFactory.formatStopProgress(resources, progress.nextStop, nowMs);
    }

    private static String appendDetail(String text, String detail) {
        if (detail.isEmpty()) { return text; }
        return text.isEmpty() ? detail : text + "\n" + detail;
    }

    @Nullable
    private static TravelEstimate travelEstimate(@Nullable NavTravelEstimate value, boolean imperial,
            long observationTimeMs) {
        if (value == null || !Double.isFinite(value.timeSeconds) || value.timeSeconds < 0) {
            return null;
        }
        long seconds = (long) Math.ceil(value.timeSeconds);
        return new TravelEstimate.Builder(ViBRoCarManeuvers.distance(value.distanceMeters, imperial),
                DateTimeWithZone.create(observationTimeMs + seconds * 1_000L, TimeZone.getDefault()))
                .setRemainingTimeSeconds(seconds).build();
    }
}
