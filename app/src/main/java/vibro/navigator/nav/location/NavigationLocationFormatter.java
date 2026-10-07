package vibro.navigator.nav.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NavigationLocationFormatter {

    private NavigationLocationFormatter() {
    }

    @NonNull
    public static String format(@Nullable NavigationLocation location) {
        if (location == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(location.getProvider())
                .append("(")
                .append(location.getLatitude())
                .append(",")
                .append(location.getLongitude())
                .append(")");
        if (location.hasAccuracy()) {
            sb.append(" acc=").append(location.getAccuracy());
        }
        if (location.hasSpeed()) {
            sb.append(" speed=").append(location.getSpeed());
        }
        sb.append(" speedAccuracy=").append(location.hasSpeedAccuracy()
                ? Float.toString(location.getSpeedAccuracyMetersPerSecond()) : "unknown");
        if (location.hasBearing()) {
            sb.append(" bearing=").append(location.getBearing());
        }
        sb.append(" bearingAccuracy=").append(location.hasBearingAccuracy()
                ? Float.toString(location.getBearingAccuracyDegrees()) : "unknown");
        sb.append(" time=").append(location.getTime());
        return sb.toString();
    }
}
