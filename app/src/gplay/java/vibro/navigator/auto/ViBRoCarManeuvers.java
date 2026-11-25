package vibro.navigator.auto;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Distance;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.DurationSpan;
import androidx.car.app.navigation.model.Maneuver;
import androidx.car.app.navigation.model.Step;
import androidx.core.graphics.drawable.IconCompat;

import vibro.navigator.R;
import vibro.navigator.nav.model.NavManeuver;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.format.NavigationDistanceUnit;
import vibro.navigator.nav.format.NavigationMeasurementFormatter;
import vibro.navigator.nav.format.NavigationTextFormatter;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.route.RouteStartApproach;
import vibro.navigator.nav.route.VoiceHint;

final class ViBRoCarManeuvers {
    record Icons(CarIcon uTurn, CarIcon beeline, ViBRoCarManeuverGlyphs glyphs) {
        static Icons from(Context context) {
            return new Icons(icon(context, R.drawable.ic_auto_uturn), icon(context, R.drawable.ic_auto_beeline),
                    new ViBRoCarManeuverGlyphs());
        }

        private static CarIcon icon(Context context, @DrawableRes int drawable) {
            return new CarIcon.Builder(IconCompat.createWithResource(context, drawable))
                    .setTint(CarColor.DEFAULT).build();
        }
    }

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

    static Step step(NavManeuver value, CharSequence cue, @Nullable Icons icons) {
        return new Step.Builder(cue).setManeuver(maneuver(value.hint, icons)).build();
    }

    static Step followingStep(NavGuidanceStatus guidance, NavigationTextResources resources, Icons icons) {
        NavManeuver current = guidance.maneuvers.get(0);
        NavManeuver following = guidance.maneuvers.get(1);
        double seconds = following.timeSeconds - current.timeSeconds;
        if (!Double.isFinite(seconds) && !guidance.afterNextLine.isEmpty()) {
            // Shared cue includes the route timing fallback when live step times are unavailable.
            return step(following, cue(guidance.afterNextLine, guidance.afterNextUncertain), icons);
        }

        double meters = Math.max(0, following.distanceMeters - current.distanceMeters);
        SpannableStringBuilder text = new SpannableStringBuilder(cue(following.instruction, guidance.afterNextUncertain));
        appendMetric(text, NavigationMeasurementFormatter.formatDistance(resources, meters),
                DistanceSpan.create(distance(meters, resources.isImperialUnitsEnabled())));
        appendDuration(text, Math.max(0, seconds), resources);
        return step(following, text, icons);
    }

    static void appendDuration(SpannableStringBuilder cue, double seconds, NavigationTextResources resources) {
        if (Double.isFinite(seconds) && seconds >= 0) {
            appendMetric(cue, NavigationTextFormatter.formatTimeSeconds(resources, seconds),
                    DurationSpan.create(Math.round(seconds)));
        }
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

    static Maneuver maneuver(VoiceHint hint, @Nullable Icons icons) {
        int maneuverType = maneuverType(hint);
        Maneuver.Builder builder = new Maneuver.Builder(maneuverType);
        if (isRoundabout(hint) && hint.exitNumber != 0) {
            builder.setRoundaboutExitNumber(Math.abs(hint.exitNumber));
        }
        CarIcon icon = maneuverIcon(hint, maneuverType, icons);
        if (icon != null) {
            builder.setIcon(icon);
        }
        return builder.build();
    }

    @Nullable
    private static CarIcon maneuverIcon(VoiceHint hint, int maneuverType, @Nullable Icons icons) {
        if (icons == null) { return null; }
        return switch (hint.command) {
            case 15 -> icons.uTurn();
            case RouteStartApproach.BEELINE_COMMAND -> icons.beeline();
            default -> maneuverType == Maneuver.TYPE_UNKNOWN ? null : icons.glyphs().get(hint);
        };
    }

    private static int maneuverType(VoiceHint hint) {
        if (isRoundabout(hint) && hint.exitNumber == 0) {
            return hint.command == 13 ? Maneuver.TYPE_ROUNDABOUT_ENTER_CCW : Maneuver.TYPE_ROUNDABOUT_ENTER_CW;
        }
        return type(hint.command);
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
