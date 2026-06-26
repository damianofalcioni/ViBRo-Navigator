package vibro.navigator.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.model.RoundTripDirection;

final class RoundTripDirectionInput {
    private RoundTripDirectionInput() {
    }

    @Nullable
    static Integer parseDirectionDegrees(@Nullable CharSequence rawValue) {
        if (rawValue == null) {
            return null;
        }
        String value = rawValue.toString().trim();
        if (value.endsWith("\u00b0")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        if (value.isEmpty()) {
            return null;
        }
        try {
            int directionDegrees = Integer.parseInt(value);
            return RoundTripDirection.isValidDirectionDegrees(directionDegrees)
                    ? directionDegrees
                    : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @NonNull
    static String formatHeadingDegrees(double headingDegrees) {
        return Integer.toString(RoundTripDirection.fromHeadingDegrees(headingDegrees));
    }
}
