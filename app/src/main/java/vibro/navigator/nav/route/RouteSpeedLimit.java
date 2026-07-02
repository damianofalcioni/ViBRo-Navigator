package vibro.navigator.nav.route;

import androidx.annotation.NonNull;

public final class RouteSpeedLimit {
    private static final double KILOMETERS_PER_HOUR_TO_METERS_PER_SECOND = 1.0 / 3.6;
    private static final double MILES_PER_HOUR_TO_METERS_PER_SECOND = 0.44704;

    public enum Unit {
        KILOMETERS_PER_HOUR,
        MILES_PER_HOUR
    }

    public final int value;
    @NonNull
    public final Unit unit;

    public RouteSpeedLimit(int value, @NonNull Unit unit) {
        this.value = value;
        this.unit = unit;
    }

    public double metersPerSecond() {
        if (unit == Unit.MILES_PER_HOUR) {
            return value * MILES_PER_HOUR_TO_METERS_PER_SECOND;
        }
        return value * KILOMETERS_PER_HOUR_TO_METERS_PER_SECOND;
    }
}
