package vibro.navigator.main;

import androidx.annotation.Nullable;

final class RoundTripDistanceInput {
    private static final double METERS_PER_MILE = 1609.344;

    private RoundTripDistanceInput() {
    }

    @Nullable
    static Integer parseDistanceMeters(@Nullable CharSequence rawValue, boolean imperialUnitsEnabled) {
        Double entered = parseEnteredDistance(rawValue);
        if (entered == null) {
            return null;
        }
        double meters = imperialUnitsEnabled ? entered * METERS_PER_MILE : entered;
        return roundedPositiveMeters(meters);
    }

    @Nullable
    private static Double parseEnteredDistance(@Nullable CharSequence rawValue) {
        String normalized = normalize(rawValue);
        if (normalized == null) {
            return null;
        }
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nullable
    private static String normalize(@Nullable CharSequence rawValue) {
        if (rawValue == null) {
            return null;
        }
        String normalized = rawValue.toString().trim().replace(',', '.');
        return normalized.isEmpty() ? null : normalized;
    }

    @Nullable
    private static Integer roundedPositiveMeters(double meters) {
        if (Double.isNaN(meters) || Double.isInfinite(meters) || meters <= 0.0) {
            return null;
        }
        long roundedMeters = Math.round(meters);
        if (roundedMeters <= 0L || roundedMeters > Integer.MAX_VALUE) {
            return null;
        }
        return (int) roundedMeters;
    }
}
