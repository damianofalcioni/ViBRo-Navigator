package vibro.navigator.auto;

import android.text.SpannableStringBuilder;
import android.text.Spanned;

import androidx.car.app.model.Distance;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.DurationSpan;
import androidx.car.app.navigation.model.Maneuver;
import androidx.car.app.navigation.model.Step;

import vibro.navigator.nav.model.NavManeuver;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.format.NavigationDistanceUnit;
import vibro.navigator.nav.format.NavigationMeasurementFormatter;
import vibro.navigator.nav.format.NavigationTextFormatter;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.route.VoiceHint;

final class ViBRoCarManeuvers {
    // BRouter mode-9 commands; unknown and direct-to-target guidance must not invent road turns.
    private static final int[] TYPES = {
            Maneuver.TYPE_UNKNOWN, Maneuver.TYPE_STRAIGHT,
            Maneuver.TYPE_TURN_NORMAL_LEFT, Maneuver.TYPE_TURN_SLIGHT_LEFT, Maneuver.TYPE_TURN_SHARP_LEFT,
            Maneuver.TYPE_TURN_NORMAL_RIGHT, Maneuver.TYPE_TURN_SLIGHT_RIGHT, Maneuver.TYPE_TURN_SHARP_RIGHT,
            Maneuver.TYPE_KEEP_LEFT, Maneuver.TYPE_KEEP_RIGHT, Maneuver.TYPE_U_TURN_LEFT, Maneuver.TYPE_U_TURN_RIGHT,
            Maneuver.TYPE_UNKNOWN, Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW,
            Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW, Maneuver.TYPE_UNKNOWN,
            Maneuver.TYPE_UNKNOWN, Maneuver.TYPE_OFF_RAMP_NORMAL_LEFT, Maneuver.TYPE_OFF_RAMP_NORMAL_RIGHT
    };

    private ViBRoCarManeuvers() { }

    static Step step(NavManeuver value, boolean uncertain) {
        return step(value, cue(value.instruction, uncertain));
    }

    static Step step(NavManeuver value, CharSequence cue) {
        return new Step.Builder(cue).setManeuver(maneuver(value.hint)).build();
    }

    static Step followingStep(NavGuidanceStatus guidance, NavigationTextResources resources) {
        NavManeuver current = guidance.maneuvers.get(0);
        NavManeuver following = guidance.maneuvers.get(1);
        double seconds = following.timeSeconds - current.timeSeconds;
        if (!Double.isFinite(seconds) && !guidance.afterNextLine.isEmpty()) {
            // Shared cue includes the route timing fallback when live step times are unavailable.
            return step(following, cue(guidance.afterNextLine, guidance.afterNextUncertain));
        }

        double meters = Math.max(0, following.distanceMeters - current.distanceMeters);
        SpannableStringBuilder text = new SpannableStringBuilder(cue(following.instruction, guidance.afterNextUncertain));
        appendMetric(text, NavigationMeasurementFormatter.formatDistance(resources, meters),
                DistanceSpan.create(distance(meters, resources.isImperialUnitsEnabled())));
        if (Double.isFinite(seconds)) {
            double remaining = Math.max(0, seconds);
            appendMetric(text, NavigationTextFormatter.formatTimeSeconds(resources, remaining),
                    DurationSpan.create(Math.round(remaining)));
        }
        return step(following, text);
    }

    private static void appendMetric(SpannableStringBuilder cue, String fallback, Object span) {
        cue.append(" · ");
        int start = cue.length();
        cue.append(fallback);
        cue.setSpan(span, start, cue.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    static String cue(String instruction, boolean uncertain) {
        return uncertain ? "≈ " + instruction : instruction;
    }

    static Maneuver maneuver(VoiceHint hint) {
        int type = type(hint.command);
        if (isRoundabout(hint) && hint.exitNumber == 0) {
            type = hint.command == 13 ? Maneuver.TYPE_ROUNDABOUT_ENTER_CCW : Maneuver.TYPE_ROUNDABOUT_ENTER_CW;
        }
        Maneuver.Builder builder = new Maneuver.Builder(type);
        if ((hint.command == 13 || hint.command == 14) && hint.exitNumber != 0) {
            builder.setRoundaboutExitNumber(Math.abs(hint.exitNumber));
        }
        return builder.build();
    }

    private static boolean isRoundabout(VoiceHint hint) {
        return hint.command == 13 || hint.command == 14;
    }

    static int type(int command) {
        if (command == 100 || command == 101) {
            return Maneuver.TYPE_DESTINATION;
        }
        return command >= 0 && command < TYPES.length ? TYPES[command] : Maneuver.TYPE_UNKNOWN;
    }

    static Distance distance(double meters, boolean imperial) {
        double nonnegativeMeters = Double.isFinite(meters) ? Math.max(0, meters) : 0;
        NavigationDistanceUnit unit = NavigationDistanceUnit.select(nonnegativeMeters, imperial);
        double value = unit.fromMeters(nonnegativeMeters);
        return switch (unit) {
            case KILOMETERS -> Distance.create(roundTenths(value), Distance.UNIT_KILOMETERS_P1);
            case MILES -> Distance.create(roundTenths(value), Distance.UNIT_MILES_P1);
            case FEET -> Distance.create(Math.round(value), Distance.UNIT_FEET);
            case METERS -> Distance.create(Math.round(value), Distance.UNIT_METERS);
        };
    }

    private static double roundTenths(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
