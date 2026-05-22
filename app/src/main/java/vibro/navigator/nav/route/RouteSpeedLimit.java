package vibro.navigator.nav.route;

import androidx.annotation.NonNull;

public final class RouteSpeedLimit {
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
}
