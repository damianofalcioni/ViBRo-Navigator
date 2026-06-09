package vibro.navigator.nav.guidance;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NavigationRouteProgressTrackerTest {

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
        tracker.rememberAlongTrackSample(10.0, 1_000L);

        float speedMps = tracker.resolveEtaSpeedMps(4_000L, 19.0, 2f, false);

        assertEquals(3.0f, speedMps, 0.0f);
    }

    @Test
    public void resolveEtaSpeedMps_keepsSlowWalkingProgressUsable() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();
        tracker.rememberAlongTrackSample(10.0, 1_000L);

        float speedMps = tracker.resolveEtaSpeedMps(7_000L, 11.8, 2f, false);

        assertEquals(0.3f, speedMps, 0.01f);
    }

    @Test
    public void resolveEtaSpeedMps_returnsZeroWhenLikelyStationary() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();
        tracker.rememberAlongTrackSample(10.0, 1_000L);

        float speedMps = tracker.resolveEtaSpeedMps(4_000L, 19.0, 2f, true);

        assertEquals(0.0f, speedMps, 0.0f);
    }

    @Test
    public void resolveEtaSpeedMps_usesMostRecentEligibleProgressSample() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();
        tracker.rememberAlongTrackSample(0.0, 0L);
        tracker.rememberAlongTrackSample(100.0, 6_000L);

        float speedMps = tracker.resolveEtaSpeedMps(9_000L, 95.0, 2f, false);

        assertEquals(0.0f, speedMps, 0.0f);
    }

    @Test
    public void resolveEtaSpeedMps_ignoresSamplesOlderThanMaximumAge() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();
        tracker.rememberAlongTrackSample(10.0, 1_000L);

        float speedMps = tracker.resolveEtaSpeedMps(12_000L, 30.0, 2f, false);

        assertEquals(0.0f, speedMps, 0.0f);
    }

    @Test
    public void assessDirection_reportsForwardBackwardAndStalledProgress() {
        NavigationRouteProgressTracker forwardTracker = new NavigationRouteProgressTracker();
        forwardTracker.rememberAlongTrackSample(10.0, 1_000L);
        NavigationRouteProgressTracker.DirectionAssessment forward =
                forwardTracker.assessDirection(15.0, 4_000L);

        NavigationRouteProgressTracker backwardTracker = new NavigationRouteProgressTracker();
        backwardTracker.rememberAlongTrackSample(10.0, 1_000L);
        NavigationRouteProgressTracker.DirectionAssessment backward =
                backwardTracker.assessDirection(5.0, 4_000L);

        NavigationRouteProgressTracker stalledTracker = new NavigationRouteProgressTracker();
        stalledTracker.rememberAlongTrackSample(10.0, 1_000L);
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
        tracker.rememberAlongTrackSample(10.0, 1_000L);

        NavigationRouteProgressTracker.DirectionAssessment assessment = tracker.assessDirection(15.0, 3_000L);

        assertEquals(NavigationRouteProgressTracker.DirectionStatus.UNKNOWN, assessment.status);
    }

    @Test
    public void assessDirection_usesMostRecentEligibleWindowSample() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();
        tracker.rememberAlongTrackSample(0.0, 0L);
        tracker.rememberAlongTrackSample(100.0, 6_000L);

        NavigationRouteProgressTracker.DirectionAssessment assessment =
                tracker.assessDirection(95.0, 9_000L);

        assertEquals(NavigationRouteProgressTracker.DirectionStatus.BACKWARD, assessment.status);
        assertEquals(-5.0, assessment.alongTrackDeltaMeters, 0.0);
    }

    @Test
    public void assessDirection_ignoresSamplesOlderThanMaximumAge() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();
        tracker.rememberAlongTrackSample(10.0, 1_000L);

        NavigationRouteProgressTracker.DirectionAssessment assessment =
                tracker.assessDirection(20.0, 12_000L);

        assertEquals(NavigationRouteProgressTracker.DirectionStatus.UNKNOWN, assessment.status);
    }

}
