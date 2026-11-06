package vibro.navigator.nav.guidance;

import org.junit.Test;
import vibro.navigator.nav.location.LocationLogReplay;
import vibro.navigator.nav.location.NavigationLocation;
import static org.junit.Assert.*;

public class NavigationAcquisitionQualityTest {
    @Test
    public void currentTrustworthySpeedProtectsAnApproachAfterAcceleration() {
        NavigationLocation location = fix(100_000, 5);
        location.setSpeed(10);
        location.setSpeedAccuracyMetersPerSecond(0.1f);
        assertEquals(3_000, NavigationAcquisitionQuality.limit(60_000, 100, 1, location, 100_000));
        location.setSpeed(1);
        assertEquals(3_000, NavigationAcquisitionQuality.limit(60_000, 100, 10, location, 100_000));
        assertEquals(1_000, NavigationAcquisitionQuality.limit(1_000, 100, 1, location, 100_000));
    }

    @Test
    public void anUncorroboratedNativeSpeedSpikeCannotOverrideSupportedProgress() {
        NavigationLocation location = fix(100_000, 5);
        location.setSpeed(10);
        assertEquals(12_000, NavigationAcquisitionQuality.limit(60_000, 100, 1, location, 100_000));
        for (float accuracy : new float[] {Float.NaN, Float.POSITIVE_INFINITY, -1, 10}) {
            location.setSpeedAccuracyMetersPerSecond(accuracy);
            assertEquals(12_000, NavigationAcquisitionQuality.limit(60_000, 100, 1, location, 100_000));
        }
        location.setSpeedAccuracyMetersPerSecond(0.1f);
        location.setSpeed(Float.NaN);
        assertEquals(12_000, NavigationAcquisitionQuality.limit(60_000, 100, 1, location, 100_000));
    }

    @Test
    public void uncertaintyAndObservationAgeBringApproachVerificationForward() {
        NavigationLocation location = fix(1_000, 75);
        assertEquals(3_000, NavigationAcquisitionQuality.limit(20_000, 170, 1.5f, location, 1_000));
        location.setAccuracy(5);
        assertEquals(20_000, NavigationAcquisitionQuality.limit(20_000, 170, 1.5f, location, 1_000));
        assertEquals(3_000, NavigationAcquisitionQuality.limit(20_000, 170, 1.5f, location, 91_000));
    }

    @Test
    public void longReliableLegsAndExplicitFastRequestsRetainTheirIntervals() {
        NavigationLocation location = fix(1_000, 10);
        assertEquals(60_000, NavigationAcquisitionQuality.limit(60_000, 2_000, 1.5f, location, 1_000));
        assertEquals(1_000, NavigationAcquisitionQuality.limit(1_000, 2, 1.5f, location, 1_000));
    }

    @Test
    public void fasterApproachReceivesShorterIntervalWithoutPredictingAnAlert() {
        NavigationLocation location = fix(1_000, 10);
        assertEquals(3_000, NavigationAcquisitionQuality.limit(20_000, 200, 8, location, 1_000));
        assertEquals(3_000, NavigationAcquisitionQuality.limit(20_000, 200, 2, fix(1_000, Float.NaN), 1_000));
    }

    @Test
    public void recordedPoorApproachRequestsVerificationWithinItsUncertaintyMargin() throws Exception {
        int checked = 0;
        for (LocationLogReplay.Sample sample : LocationLogReplay.load("gnss-poor-approach")) {
            if (sample.location.getAccuracy() > 50) {
                assertEquals(3_000, NavigationAcquisitionQuality.limit(20_000, 100, 1.5f,
                        sample.location, sample.receivedAtMs));
                checked++;
            }
        }
        assertTrue(checked > 3);
    }

    private static NavigationLocation fix(long time, float accuracy) {
        NavigationLocation fix = new NavigationLocation("gps");
        fix.setTime(time, time);
        fix.setAccuracy(accuracy);
        return fix;
    }
}
