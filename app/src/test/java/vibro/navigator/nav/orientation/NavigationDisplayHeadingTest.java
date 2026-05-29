package vibro.navigator.nav.orientation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class NavigationDisplayHeadingTest {

    @Test
    public void remapDegreesForDisplayRotationCompensatesLandscapeAndNormalizes() {
        assertEquals(0.0, NavigationDisplayHeading.remapDegreesForDisplayRotation(0.0, DisplayRotation.ROTATION_0), 0.001);
        assertEquals(90.0, NavigationDisplayHeading.remapDegreesForDisplayRotation(0.0, DisplayRotation.ROTATION_90), 0.001);
        assertEquals(180.0, NavigationDisplayHeading.remapDegreesForDisplayRotation(0.0, DisplayRotation.ROTATION_180), 0.001);
        assertEquals(270.0, NavigationDisplayHeading.remapDegreesForDisplayRotation(0.0, DisplayRotation.ROTATION_270), 0.001);
        assertEquals(135.0, NavigationDisplayHeading.remapDegreesForDisplayRotation(45.0, DisplayRotation.ROTATION_90), 0.001);
    }

    @Test
    public void headingDegreesReturnsNullWhenInactiveMissingOrStale() {
        GeomagneticOrientationMonitor.Sample sample = sample(45.0, null, HeadingAccuracyStatus.HIGH, 1_000L);

        assertNull(NavigationDisplayHeading.headingDegrees(sample, false, 1_100L, DisplayRotation.ROTATION_0));
        assertNull(NavigationDisplayHeading.headingDegrees(null, true, 1_100L, DisplayRotation.ROTATION_0));
        assertNull(NavigationDisplayHeading.headingDegrees(sample, true, 6_001L, DisplayRotation.ROTATION_0));
    }

    @Test
    public void headingDegreesUsesFreshSampleAndDisplayRotation() {
        GeomagneticOrientationMonitor.Sample sample = sample(45.0, null, HeadingAccuracyStatus.HIGH, 1_000L);

        Double headingDegrees = NavigationDisplayHeading.headingDegrees(sample, true, 1_100L, DisplayRotation.ROTATION_90);

        assertEquals(135.0, headingDegrees, 0.001);
    }

    @Test
    public void headingAccuracyDegreesPrefersExplicitSensorAccuracy() {
        GeomagneticOrientationMonitor.Sample sample = sample(45.0, 7.5, HeadingAccuracyStatus.LOW, 1_000L);

        Float headingAccuracyDegrees = NavigationDisplayHeading.headingAccuracyDegrees(sample, true, 1_100L);

        assertEquals(7.5f, headingAccuracyDegrees, 0.001f);
    }

    @Test
    public void headingAccuracyDegreesUsesFreshLegacyOrientationLowAccuracyAsVeto() {
        GeomagneticOrientationMonitor.Sample sample = sample(
                45.0,
                3.0,
                HeadingAccuracyStatus.HIGH,
                HeadingAccuracyStatus.LOW,
                1_050L,
                1_000L
        );

        Float headingAccuracyDegrees = NavigationDisplayHeading.headingAccuracyDegrees(sample, true, 1_100L);

        assertEquals(35f, headingAccuracyDegrees, 0.001f);
    }

    @Test
    public void headingAccuracyDegreesTreatsFreshLegacyOrientationUnreliableAsExplicitPoorAccuracy() {
        GeomagneticOrientationMonitor.Sample sample = sample(
                45.0,
                3.0,
                HeadingAccuracyStatus.HIGH,
                HeadingAccuracyStatus.UNRELIABLE,
                1_050L,
                1_000L
        );

        Float headingAccuracyDegrees = NavigationDisplayHeading.headingAccuracyDegrees(sample, true, 1_100L);

        assertEquals(90f, headingAccuracyDegrees, 0.001f);
    }

    @Test
    public void headingAccuracyDegreesIgnoresStaleLegacyOrientationAccuracy() {
        GeomagneticOrientationMonitor.Sample sample = sample(
                45.0,
                3.0,
                HeadingAccuracyStatus.HIGH,
                HeadingAccuracyStatus.LOW,
                1_000L,
                6_000L
        );

        Float headingAccuracyDegrees = NavigationDisplayHeading.headingAccuracyDegrees(sample, true, 6_100L);

        assertEquals(3f, headingAccuracyDegrees, 0.001f);
    }

    @Test
    public void headingAccuracyDegreesKeepsExplicitAccuracyWhenLegacyOrientationIsHigh() {
        GeomagneticOrientationMonitor.Sample sample = sample(
                45.0,
                3.0,
                HeadingAccuracyStatus.HIGH,
                HeadingAccuracyStatus.HIGH,
                1_050L,
                1_000L
        );

        Float headingAccuracyDegrees = NavigationDisplayHeading.headingAccuracyDegrees(sample, true, 1_100L);

        assertEquals(3f, headingAccuracyDegrees, 0.001f);
    }

    @Test
    public void headingAccuracyDegreesMapsSensorStatusFallbacks() {
        assertEquals(10f, NavigationDisplayHeading.headingAccuracyDegrees(
                sample(45.0, null, HeadingAccuracyStatus.HIGH, 1_000L),
                true,
                1_100L
        ), 0.001f);
        assertEquals(20f, NavigationDisplayHeading.headingAccuracyDegrees(
                sample(45.0, null, HeadingAccuracyStatus.MEDIUM, 1_000L),
                true,
                1_100L
        ), 0.001f);
        assertEquals(35f, NavigationDisplayHeading.headingAccuracyDegrees(
                sample(45.0, null, HeadingAccuracyStatus.LOW, 1_000L),
                true,
                1_100L
        ), 0.001f);
        assertNull(NavigationDisplayHeading.headingAccuracyDegrees(
                sample(45.0, null, HeadingAccuracyStatus.UNRELIABLE, 1_000L),
                true,
                1_100L
        ));
    }

    private static GeomagneticOrientationMonitor.Sample sample(
            double headingDegrees,
            Double headingAccuracyDegrees,
            int accuracy,
            long elapsedRealtimeMs
    ) {
        return new GeomagneticOrientationMonitor.Sample(
                headingDegrees,
                0.0,
                0.0,
                accuracy,
                headingAccuracyDegrees,
                elapsedRealtimeMs
        );
    }

    private static GeomagneticOrientationMonitor.Sample sample(
            double headingDegrees,
            Double headingAccuracyDegrees,
            int accuracy,
            int legacyOrientationAccuracy,
            long legacyOrientationAccuracyElapsedRealtimeMs,
            long elapsedRealtimeMs
    ) {
        return new GeomagneticOrientationMonitor.Sample(
                headingDegrees,
                0.0,
                0.0,
                accuracy,
                legacyOrientationAccuracy,
                legacyOrientationAccuracyElapsedRealtimeMs,
                headingAccuracyDegrees,
                elapsedRealtimeMs
        );
    }
}
