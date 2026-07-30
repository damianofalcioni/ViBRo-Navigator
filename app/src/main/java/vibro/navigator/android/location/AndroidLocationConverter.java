package vibro.navigator.android.location;

import android.location.Location;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.location.NavigationLocation;

import java.util.concurrent.TimeUnit;

public final class AndroidLocationConverter {
    private AndroidLocationConverter() {
    }

    @Nullable
    public static NavigationLocation toNavigationLocation(@Nullable Location location) {
        if (location == null) {
            return null;
        }
        return fromSnapshot(new AndroidLocationSnapshot(location));
    }

    @Nullable
    static NavigationLocation fromSnapshot(@Nullable LocationSnapshot location) {
        if (location == null) {
            return null;
        }
        NavigationLocation out = new NavigationLocation(location.provider());
        out.setTime(
                location.time(),
                TimeUnit.NANOSECONDS.toMillis(location.elapsedRealtimeNanos())
        );
        out.setLatitude(location.latitude());
        out.setLongitude(location.longitude());
        copyOptionalFields(location, out);
        return out;
    }

    private static void copyOptionalFields(@NonNull LocationSnapshot location, @NonNull NavigationLocation out) {
        if (location.hasAccuracy()) {
            out.setAccuracy(location.accuracy());
        }
        if (location.hasAltitude()) {
            out.setAltitude(location.altitude());
        }
        if (location.hasSpeed()) {
            out.setSpeed(location.speed());
        }
        if (location.hasBearing()) {
            out.setBearing(location.bearing());
        }
        if (location.hasBearingAccuracy()) {
            out.setBearingAccuracyDegrees(location.bearingAccuracyDegrees());
        }
    }

    interface LocationSnapshot {
        @Nullable
        String provider();

        long time();

        long elapsedRealtimeNanos();

        double latitude();

        double longitude();

        boolean hasAccuracy();

        float accuracy();

        boolean hasAltitude();

        double altitude();

        boolean hasSpeed();

        float speed();

        boolean hasBearing();

        float bearing();

        boolean hasBearingAccuracy();

        float bearingAccuracyDegrees();
    }

    private static final class AndroidLocationSnapshot implements LocationSnapshot {
        @NonNull
        private final Location location;

        private AndroidLocationSnapshot(@NonNull Location location) {
            this.location = location;
        }

        @Override
        @Nullable
        public String provider() {
            return location.getProvider();
        }

        @Override
        public long time() {
            return location.getTime();
        }

        @Override
        public long elapsedRealtimeNanos() {
            return location.getElapsedRealtimeNanos();
        }

        @Override
        public double latitude() {
            return location.getLatitude();
        }

        @Override
        public double longitude() {
            return location.getLongitude();
        }

        @Override
        public boolean hasAccuracy() {
            return location.hasAccuracy();
        }

        @Override
        public float accuracy() {
            return location.getAccuracy();
        }

        @Override
        public boolean hasAltitude() {
            return location.hasAltitude();
        }

        @Override
        public double altitude() {
            return location.getAltitude();
        }

        @Override
        public boolean hasSpeed() {
            return location.hasSpeed();
        }

        @Override
        public float speed() {
            return location.getSpeed();
        }

        @Override
        public boolean hasBearing() {
            return location.hasBearing();
        }

        @Override
        public float bearing() {
            return location.getBearing();
        }

        @Override
        public boolean hasBearingAccuracy() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasBearingAccuracy();
        }

        @Override
        public float bearingAccuracyDegrees() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return location.getBearingAccuracyDegrees();
            }
            return 0f;
        }
    }
}
