package vibro.navigator.android.location;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.location.NavigationLocation;

public final class AndroidLocationConverter {
    private AndroidLocationConverter() {
    }

    @Nullable
    public static NavigationLocation toNavigationLocation(@Nullable Location location) {
        if (location == null) {
            return null;
        }
        NavigationLocation out = new NavigationLocation(location.getProvider());
        out.setTime(location.getTime());
        out.setLatitude(location.getLatitude());
        out.setLongitude(location.getLongitude());
        copyOptionalFields(location, out);
        return out;
    }

    private static void copyOptionalFields(@NonNull Location location, @NonNull NavigationLocation out) {
        if (location.hasAccuracy()) {
            out.setAccuracy(location.getAccuracy());
        }
        if (location.hasAltitude()) {
            out.setAltitude(location.getAltitude());
        }
        if (location.hasSpeed()) {
            out.setSpeed(location.getSpeed());
        }
        if (location.hasBearing()) {
            out.setBearing(location.getBearing());
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                && location.hasBearingAccuracy()) {
            out.setBearingAccuracyDegrees(location.getBearingAccuracyDegrees());
        }
    }
}
