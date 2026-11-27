package vibro.navigator.auto;

import android.text.SpannableStringBuilder;
import android.text.Spanned;

import androidx.annotation.Nullable;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.navigation.model.Step;

import vibro.navigator.nav.format.NavigationMeasurementFormatter;
import vibro.navigator.nav.format.NavigationTextFormatter;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavManeuver;

final class ViBRoCarFollowingStep {
    private ViBRoCarFollowingStep() { }

    @Nullable
    static Step from(NavGuidanceStatus guidance, NavigationTextResources resources, ViBRoCarManeuvers.Icons icons) {
        if (guidance.followingPreview != null) {
            NavManeuver preview = guidance.followingPreview;
            return step(preview, preview.distanceMeters, preview.timeSeconds, guidance.afterNextUncertain,
                    resources, icons);
        }
        if (guidance.maneuvers.size() < 2) {
            return null;
        }
        NavManeuver current = guidance.maneuvers.get(0);
        NavManeuver following = guidance.maneuvers.get(1);
        return step(following, Math.max(0, following.distanceMeters - current.distanceMeters),
                following.timeSeconds - current.timeSeconds, guidance.afterNextUncertain, resources, icons);
    }

    private static Step step(NavManeuver maneuver, double meters, double seconds, boolean uncertain,
            NavigationTextResources resources, ViBRoCarManeuvers.Icons icons) {
        String distance = NavigationMeasurementFormatter.formatDistance(resources, meters);
        String cue = ViBRoCarManeuvers.cue(distance, uncertain);
        var distanceCue = new SpannableStringBuilder(cue);
        distanceCue.setSpan(DistanceSpan.create(ViBRoCarManeuvers.distance(meters, resources.isImperialUnitsEnabled())),
                cue.length() - distance.length(), cue.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        var fullCue = new SpannableStringBuilder(distanceCue);
        if (Double.isFinite(seconds)) {
            // Keep the measured seconds in the cue. Some hosts round a DurationSpan down to zero minutes.
            String duration = NavigationTextFormatter.formatTimeSeconds(resources, Math.max(0, seconds));
            fullCue.append(" · ").append(duration.replace(' ', '\u202f'));
        }
        return ViBRoCarManeuvers.step(maneuver, ViBRoCarCueText.withVariants(fullCue, distanceCue), icons);
    }
}
