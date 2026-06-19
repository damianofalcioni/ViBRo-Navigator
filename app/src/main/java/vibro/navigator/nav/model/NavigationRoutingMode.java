package vibro.navigator.nav.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public enum NavigationRoutingMode {
    BROUTER("brouter"),
    STRAIGHT_LINE("straight_line"),
    ROUND_TRIP("round_trip");

    @NonNull
    private final String serializedName;

    NavigationRoutingMode(@NonNull String serializedName) {
        this.serializedName = serializedName;
    }

    @NonNull
    public String serializedName() {
        return serializedName;
    }

    @NonNull
    public static NavigationRoutingMode fromSerializedName(@Nullable String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return BROUTER;
        }
        String normalized = rawValue.trim();
        for (NavigationRoutingMode mode : values()) {
            if (mode.serializedName.equals(normalized)) {
                return mode;
            }
        }
        return BROUTER;
    }
}
