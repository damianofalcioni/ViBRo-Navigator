package vibro.navigator.nav.location;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NavigationLocationFormatter {

    private NavigationLocationFormatter() {
    }

    @NonNull
    public static String format(@Nullable NavigationLocation NavigationLocation) {
        if (NavigationLocation == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(NavigationLocation.getProvider())
                .append("(")
                .append(NavigationLocation.getLatitude())
                .append(",")
                .append(NavigationLocation.getLongitude())
                .append(")");
        if (NavigationLocation.hasAccuracy()) {
            sb.append(" acc=").append(NavigationLocation.getAccuracy());
        }
        if (NavigationLocation.hasSpeed()) {
            sb.append(" speed=").append(NavigationLocation.getSpeed());
        }
        if (NavigationLocation.hasBearing()) {
            sb.append(" bearing=").append(NavigationLocation.getBearing());
        }
        sb.append(" time=").append(NavigationLocation.getTime());
        return sb.toString();
    }
}
