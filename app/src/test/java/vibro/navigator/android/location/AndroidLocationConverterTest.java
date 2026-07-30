package vibro.navigator.android.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import vibro.navigator.nav.location.NavigationLocation;

public class AndroidLocationConverterTest {
    private static final String GPS_PROVIDER = "gps";

    @Test
    public void toNavigationLocation_copiesCoreAndOptionalFields() {
        TestLocationSnapshot snapshot = new TestLocationSnapshot(GPS_PROVIDER)
                .time(1234L)
                .elapsedRealtimeMs(5678L)
                .latitude(48.2082)
                .longitude(16.3738)
                .accuracy(4.5f)
                .altitude(188.0)
                .speed(1.25f)
                .bearing(91.0f)
                .bearingAccuracyDegrees(7.5f);

        NavigationLocation location = AndroidLocationConverter.fromSnapshot(snapshot);

        assertEquals(GPS_PROVIDER, location.getProvider());
        assertEquals(1234L, location.getTime());
        assertEquals(5678L, location.getElapsedRealtimeOrTimeMs());
        assertEquals(48.2082, location.getLatitude(), 0.0);
        assertEquals(16.3738, location.getLongitude(), 0.0);
        assertTrue(location.hasAccuracy());
        assertEquals(4.5f, location.getAccuracy(), 0.0f);
        assertTrue(location.hasAltitude());
        assertEquals(188.0, location.getAltitude(), 0.0);
        assertTrue(location.hasSpeed());
        assertEquals(1.25f, location.getSpeed(), 0.0f);
        assertTrue(location.hasBearing());
        assertEquals(91.0f, location.getBearing(), 0.0f);
        assertTrue(location.hasBearingAccuracy());
        assertEquals(7.5f, location.getBearingAccuracyDegrees(), 0.0f);
    }

    @Test
    public void toNavigationLocation_preservesMissingOptionalFields() {
        TestLocationSnapshot snapshot = new TestLocationSnapshot("network")
                .latitude(1.0)
                .longitude(2.0);

        NavigationLocation location = AndroidLocationConverter.fromSnapshot(snapshot);

        assertEquals("network", location.getProvider());
        assertFalse(location.hasAccuracy());
        assertFalse(location.hasAltitude());
        assertFalse(location.hasSpeed());
        assertFalse(location.hasBearing());
        assertFalse(location.hasBearingAccuracy());
    }

    @Test
    public void toNavigationLocation_returnsNullForNullInput() {
        assertNull(AndroidLocationConverter.fromSnapshot(null));
    }

    private static final class TestLocationSnapshot implements AndroidLocationConverter.LocationSnapshot {
        private final String provider;
        private long time;
        private long elapsedRealtimeNanos;
        private double latitude;
        private double longitude;
        private boolean hasAccuracy;
        private float accuracy;
        private boolean hasAltitude;
        private double altitude;
        private boolean hasSpeed;
        private float speed;
        private boolean hasBearing;
        private float bearing;
        private boolean hasBearingAccuracy;
        private float bearingAccuracyDegrees;

        private TestLocationSnapshot(String provider) {
            this.provider = provider;
        }

        private TestLocationSnapshot time(long time) {
            this.time = time;
            return this;
        }

        private TestLocationSnapshot elapsedRealtimeMs(long elapsedRealtimeMs) {
            elapsedRealtimeNanos = TimeUnit.MILLISECONDS.toNanos(elapsedRealtimeMs);
            return this;
        }

        private TestLocationSnapshot latitude(double latitude) {
            this.latitude = latitude;
            return this;
        }

        private TestLocationSnapshot longitude(double longitude) {
            this.longitude = longitude;
            return this;
        }

        private TestLocationSnapshot accuracy(float accuracy) {
            this.accuracy = accuracy;
            hasAccuracy = true;
            return this;
        }

        private TestLocationSnapshot altitude(double altitude) {
            this.altitude = altitude;
            hasAltitude = true;
            return this;
        }

        private TestLocationSnapshot speed(float speed) {
            this.speed = speed;
            hasSpeed = true;
            return this;
        }

        private TestLocationSnapshot bearing(float bearing) {
            this.bearing = bearing;
            hasBearing = true;
            return this;
        }

        private TestLocationSnapshot bearingAccuracyDegrees(float bearingAccuracyDegrees) {
            this.bearingAccuracyDegrees = bearingAccuracyDegrees;
            hasBearingAccuracy = true;
            return this;
        }

        @Override
        public String provider() {
            return provider;
        }

        @Override
        public long time() {
            return time;
        }

        @Override
        public long elapsedRealtimeNanos() {
            return elapsedRealtimeNanos;
        }

        @Override
        public double latitude() {
            return latitude;
        }

        @Override
        public double longitude() {
            return longitude;
        }

        @Override
        public boolean hasAccuracy() {
            return hasAccuracy;
        }

        @Override
        public float accuracy() {
            return accuracy;
        }

        @Override
        public boolean hasAltitude() {
            return hasAltitude;
        }

        @Override
        public double altitude() {
            return altitude;
        }

        @Override
        public boolean hasSpeed() {
            return hasSpeed;
        }

        @Override
        public float speed() {
            return speed;
        }

        @Override
        public boolean hasBearing() {
            return hasBearing;
        }

        @Override
        public float bearing() {
            return bearing;
        }

        @Override
        public boolean hasBearingAccuracy() {
            return hasBearingAccuracy;
        }

        @Override
        public float bearingAccuracyDegrees() {
            return bearingAccuracyDegrees;
        }
    }
}
