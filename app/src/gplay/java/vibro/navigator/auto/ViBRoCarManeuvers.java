package vibro.navigator.auto;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.util.SparseArray;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.Distance;
import androidx.car.app.navigation.model.Maneuver;
import androidx.car.app.navigation.model.Step;
import androidx.core.graphics.drawable.IconCompat;

import vibro.navigator.R;
import vibro.navigator.nav.model.NavManeuver;
import vibro.navigator.nav.format.NavigationDistanceUnit;
import vibro.navigator.nav.format.NavigationTextFormatter;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.route.RouteStartApproach;
import vibro.navigator.nav.route.VoiceHint;

final class ViBRoCarManeuvers {
    private static final int[] CUSTOM_DRAWABLES = new int[101];

    static {
        CUSTOM_DRAWABLES[12] = R.drawable.ic_auto_off_route;
        CUSTOM_DRAWABLES[15] = R.drawable.ic_auto_uturn;
        CUSTOM_DRAWABLES[RouteStartApproach.BEELINE_COMMAND] = R.drawable.ic_auto_beeline;
        CUSTOM_DRAWABLES[100] = R.drawable.ic_auto_destination;
    }

    record Icons(Context context, SparseArray<CarIcon> drawables, ViBRoCarManeuverGlyphs glyphs) {
        static Icons from(Context context) {
            return new Icons(context, new SparseArray<>(), new ViBRoCarManeuverGlyphs());
        }

        @Nullable
        CarIcon forManeuver(VoiceHint hint, int type, int exitNumber) {
            int command = hint.command;
            int drawable = command >= 0 && command < CUSTOM_DRAWABLES.length ? CUSTOM_DRAWABLES[command] : 0;
            if (drawable != 0) {
                return drawableIcon(drawable);
            }
            return type == Maneuver.TYPE_UNKNOWN ? null : glyphs.get(hint, exitNumber);
        }

        private CarIcon drawableIcon(@DrawableRes int drawable) {
            CarIcon icon = drawables.get(drawable);
            if (icon == null) {
                icon = new CarIcon.Builder(IconCompat.createWithResource(context, drawable))
                        .setTint(CarColor.DEFAULT).build();
                drawables.put(drawable, icon);
            }
            return icon;
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
        return step(value, CarText.create(cue), icons);
    }

    static Step step(NavManeuver value, CarText cue, @Nullable Icons icons) {
        return new Step.Builder(cue).setManeuver(maneuver(value.hint, icons)).build();
    }

    static void appendDuration(SpannableStringBuilder cue, double seconds, NavigationTextResources resources) {
        if (Double.isFinite(seconds) && seconds >= 0) {
            // A host may render a DurationSpan in whole minutes, including zero for sub-minute times.
            cue.append(" · ").append(NavigationTextFormatter.formatTimeSeconds(resources, seconds));
        }
    }

    static String cue(String instruction, boolean uncertain) {
        return uncertain ? "≈ " + instruction : instruction;
    }

    static Maneuver maneuver(VoiceHint hint, @Nullable Icons icons) {
        int maneuverType = maneuverType(hint);
        int exitNumber = isRoundabout(hint) ? Math.abs(hint.exitNumber) : 0;
        Maneuver.Builder builder = new Maneuver.Builder(maneuverType);
        if (exitNumber > 0) {
            builder.setRoundaboutExitNumber(exitNumber);
        }
        CarIcon icon = icons == null ? null : icons.forManeuver(hint, maneuverType, exitNumber);
        if (icon != null) {
            builder.setIcon(icon);
        }
        return builder.build();
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
