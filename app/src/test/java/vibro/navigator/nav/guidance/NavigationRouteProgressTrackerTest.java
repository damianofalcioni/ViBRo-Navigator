package vibro.navigator.nav.guidance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vibro.navigator.nav.location.NavigationLocation;


public class NavigationRouteProgressTrackerTest {
    @Test
    public void walkingProgressAccumulatesWithoutReusingOppositeDirectionBeforeReversal() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();
        remember(tracker, 0, 1_000);
        remember(tracker, 3.6, 4_000);
        assertEquals(NavigationRouteProgressTracker.DirectionStatus.FORWARD,
                tracker.assessDirection(7.2, 7_000).status);
        remember(tracker, 7.2, 7_000);
        assertEquals(NavigationRouteProgressTracker.DirectionStatus.STALLED,
                tracker.assessDirection(6, 10_000).status);
        remember(tracker, 6, 10_000);
        assertEquals(NavigationRouteProgressTracker.DirectionStatus.BACKWARD,
                tracker.assessDirection(2.4, 13_000).status);
    }
    @Test
    public void providerCorrectionDoesNotBecomeEtaSpeedOrAcceleration() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();
        NavigationLocation gps = new NavigationLocation("gps");
        gps.setTime(1_000L, 1_000L);
        motion(tracker, gps, 10.0, 5f, false);
        remember(tracker, 10.0, 1_000L);
        NavigationLocation network = new NavigationLocation("network");
        network.setTime(4_000L, 4_000L);
        RouteMotionEstimate motion = motion(tracker, network, 70.0, 20f, false);
        assertEquals(0f, motion.speedMps, 0f);
        assertTrue(Float.isNaN(motion.accelerationMps2));
    }

    @Test
    public void rememberAndResolveSmoothedAccuracyMeters_returnsMedianOfRecentSamples() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();

        tracker.rememberAndResolveSmoothedAccuracyMeters(5f, 1_000L);
        tracker.rememberAndResolveSmoothedAccuracyMeters(30f, 2_000L);
        double smoothed = tracker.rememberAndResolveSmoothedAccuracyMeters(5f, 3_000L);

        assertEquals(5.0, smoothed, 0.0);
    }

    @Test
    public void rememberAndResolveSmoothedAccuracyMeters_prunesOldSamplesButKeepsLatest() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();

        tracker.rememberAndResolveSmoothedAccuracyMeters(5f, 1_000L);
        double smoothed = tracker.rememberAndResolveSmoothedAccuracyMeters(30f, 7_000L);

        assertEquals(30.0, smoothed, 0.0);
    }

    @Test
    public void resolveEtaSpeedMps_usesRecentAlongTrackProgress() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();
        remember(tracker, 10.0, 1_000L);

        float speedMps = speed(tracker, 4_000L, 19.0, 2f, false);

        assertEquals(3.0f, speedMps, 0.0f);
    }

    @Test
    public void resolveEtaSpeedMps_usesLocationElapsedRealtimeWhenAvailable() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();
        remember(tracker, 10.0, 1_000L);
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(100_000L, 4_000L);

        float speedMps = speed(tracker, location, 19.0, 2f, false);

        assertEquals(3.0f, speedMps, 0.0f);
    }

    @Test
    public void resolveEtaMotion_reportsPositiveAccelerationFromAlongTrackProgress() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();
        remember(tracker, 0.0, 0L);
        remember(tracker, 6.0, 3_000L);

        RouteMotionEstimate motion = motion(tracker, 6_000L, 18.0, 2f, false);

        assertEquals(4.0f, motion.speedMps, 0.0f);
        assertEquals(0.67f, motion.accelerationMps2, 0.01f);
    }

    @Test
    public void resolveEtaMotion_reportsNegativeAccelerationFromAlongTrackProgress() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();
        remember(tracker, 0.0, 0L);
        remember(tracker, 12.0, 3_000L);

        RouteMotionEstimate motion = motion(tracker, 6_000L, 18.0, 2f, false);

        assertEquals(2.0f, motion.speedMps, 0.0f);
        assertEquals(-0.67f, motion.accelerationMps2, 0.01f);
    }

    @Test
    public void resolveEtaSpeedMps_keepsSlowWalkingProgressUsable() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();
        remember(tracker, 10.0, 1_000L);

        float speedMps = speed(tracker, 7_000L, 11.8, 2f, false);

        assertEquals(0.3f, speedMps, 0.01f);
    }

    @Test
    public void resolveEtaSpeedMps_returnsZeroWhenLikelyStationary() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();
        remember(tracker, 10.0, 1_000L);

        float speedMps = speed(tracker, 4_000L, 19.0, 2f, true);

        assertEquals(0.0f, speedMps, 0.0f);
    }

    @Test
    public void resolveEtaSpeedMps_usesMostRecentEligibleProgressSample() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();
        remember(tracker, 0.0, 0L);
        remember(tracker, 100.0, 6_000L);

        float speedMps = speed(tracker, 9_000L, 95.0, 2f, false);

        assertEquals(0.0f, speedMps, 0.0f);
    }

    @Test
    public void resolveEtaSpeedMps_retainsNormalSparseInterval() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();
        remember(tracker, 10.0, 1_000L);

        float speedMps = speed(tracker, 12_000L, 30.0, 2f, false);

        assertEquals(20f / 11, speedMps, 0.01f);
        assertEquals(0f, speed(tracker, 67_000L, 50, 2f, false), 0f);
    }

    @Test
    public void assessDirection_reportsForwardBackwardAndStalledProgress() {
        NavigationRouteProgressTracker forwardTracker = new NavigationRouteProgressTracker();
        remember(forwardTracker, 10.0, 1_000L);
        NavigationRouteProgressTracker.DirectionAssessment forward =
                forwardTracker.assessDirection(15.0, 4_000L);

        NavigationRouteProgressTracker backwardTracker = new NavigationRouteProgressTracker();
        remember(backwardTracker, 10.0, 1_000L);
        NavigationRouteProgressTracker.DirectionAssessment backward =
                backwardTracker.assessDirection(5.0, 4_000L);

        NavigationRouteProgressTracker stalledTracker = new NavigationRouteProgressTracker();
        remember(stalledTracker, 10.0, 1_000L);
        NavigationRouteProgressTracker.DirectionAssessment stalled =
                stalledTracker.assessDirection(12.0, 4_000L);

        assertEquals(NavigationRouteProgressTracker.DirectionStatus.FORWARD, forward.status);
        assertEquals(5.0, forward.alongTrackDeltaMeters, 0.0);
        assertEquals(NavigationRouteProgressTracker.DirectionStatus.BACKWARD, backward.status);
        assertEquals(-5.0, backward.alongTrackDeltaMeters, 0.0);
        assertEquals(NavigationRouteProgressTracker.DirectionStatus.STALLED, stalled.status);
        assertEquals(2.0, stalled.alongTrackDeltaMeters, 0.0);
    }

    @Test
    public void assessDirection_reportsUnknownUntilSampleWindowIsAvailable() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();
        remember(tracker, 10.0, 1_000L);

        NavigationRouteProgressTracker.DirectionAssessment assessment = tracker.assessDirection(15.0, 3_000L);

        assertEquals(NavigationRouteProgressTracker.DirectionStatus.UNKNOWN, assessment.status);
    }

    @Test
    public void assessDirection_usesMostRecentEligibleWindowSample() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();
        remember(tracker, 0.0, 0L);
        remember(tracker, 100.0, 6_000L);

        NavigationRouteProgressTracker.DirectionAssessment assessment =
                tracker.assessDirection(95.0, 9_000L);

        assertEquals(NavigationRouteProgressTracker.DirectionStatus.BACKWARD, assessment.status);
        assertEquals(-5.0, assessment.alongTrackDeltaMeters, 0.0);
    }

    @Test
    public void assessDirection_ignoresSamplesOlderThanMaximumAge() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();
        remember(tracker, 10.0, 1_000L);

        NavigationRouteProgressTracker.DirectionAssessment assessment =
                tracker.assessDirection(20.0, 12_000L);

        assertEquals(NavigationRouteProgressTracker.DirectionStatus.UNKNOWN, assessment.status);
    }

    private static void remember(NavigationRouteProgressTracker tracker, double along, long time) {
        tracker.rememberAlongTrackSample(along, time);
        tracker.etaHistory().record(location(time), along, 2, true, time);
    }

    private static float speed(NavigationRouteProgressTracker tracker, long time, double along, float accuracy, boolean stopped) {
        return motion(tracker, location(time), along, accuracy, stopped).speedMps;
    }

    private static float speed(NavigationRouteProgressTracker tracker, NavigationLocation location,
            double along, float accuracy, boolean stopped) {
        return motion(tracker, location, along, accuracy, stopped).speedMps;
    }

    private static RouteMotionEstimate motion(NavigationRouteProgressTracker tracker, long time,
            double along, float accuracy, boolean stopped) {
        return motion(tracker, location(time), along, accuracy, stopped);
    }

    private static RouteMotionEstimate motion(NavigationRouteProgressTracker tracker, NavigationLocation location,
            double along, float accuracy, boolean stopped) {
        return tracker.etaHistory().resolve(location, along, accuracy,
                !stopped,
                location.getElapsedRealtimeOrTimeMs());
    }

    private static NavigationLocation location(long time) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(time, time);
        return location;
    }
}
