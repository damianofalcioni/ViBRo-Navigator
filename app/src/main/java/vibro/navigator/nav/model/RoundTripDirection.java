package vibro.navigator.nav.model;

public final class RoundTripDirection {
    public static final int DEFAULT_DIRECTION_DEGREES = 0;
    public static final int MIN_DIRECTION_DEGREES = 0;
    public static final int MAX_DIRECTION_DEGREES = 359;

    private static final int FULL_CIRCLE_DEGREES = 360;

    private RoundTripDirection() {
    }

    public static boolean isValidDirectionDegrees(int directionDegrees) {
        return directionDegrees >= MIN_DIRECTION_DEGREES
                && directionDegrees <= MAX_DIRECTION_DEGREES;
    }

    public static int sanitizeDirectionDegrees(int directionDegrees) {
        return isValidDirectionDegrees(directionDegrees)
                ? directionDegrees
                : DEFAULT_DIRECTION_DEGREES;
    }

    public static int fromHeadingDegrees(double headingDegrees) {
        if (!Double.isFinite(headingDegrees)) {
            return DEFAULT_DIRECTION_DEGREES;
        }
        int rounded = (int) Math.round(headingDegrees);
        int normalized = rounded % FULL_CIRCLE_DEGREES;
        return normalized < 0 ? normalized + FULL_CIRCLE_DEGREES : normalized;
    }
}
