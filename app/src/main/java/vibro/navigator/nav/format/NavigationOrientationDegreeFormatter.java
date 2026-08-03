package vibro.navigator.nav.format;

import androidx.annotation.NonNull;

public final class NavigationOrientationDegreeFormatter {
    private static final double TURN_DEGREES_STEP = 10.0;

    private NavigationOrientationDegreeFormatter() {
    }

    public static int roundedTurnDegrees(double degrees) {
        if (!Double.isFinite(degrees)) {
            return 0;
        }
        return (int) (Math.round(Math.abs(degrees) / TURN_DEGREES_STEP) * TURN_DEGREES_STEP);
    }

    @NonNull
    static String formatRoundedTurnDegrees(@NonNull NavigationTextResources resources, double degrees) {
        return NavigationTextFormatterRules.formatBearingDegrees(resources, (double) roundedTurnDegrees(degrees));
    }
}
