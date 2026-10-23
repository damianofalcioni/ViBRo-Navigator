package vibro.navigator.nav.guidance;

import java.util.ArrayDeque;
import java.util.Iterator;
import vibro.navigator.nav.guidance.NavigationRouteProgressTracker.DirectionAssessment;

/** Accumulate slow progress without letting an older travel direction override a reversal. */
final class RouteDirectionPolicy {
    private static final long WINDOW_MS = 3_000L;
    private static final double MIN_PROGRESS_METERS = 4;
    private static final double MAX_JITTER_METERS = 0.8;

    private RouteDirectionPolicy() { }

    static DirectionAssessment assess(ArrayDeque<RouteProgressSample> samples, double along,
            long nowMs, long earliestAnchorMs) {
        RouteProgressSample anchor = selectAnchor(samples, nowMs, along);
        if (anchor == null || anchor.timeMs < earliestAnchorMs) {
            return DirectionAssessment.unknown();
        }
        double delta = along - anchor.alongTrackMeters;
        if (delta >= MIN_PROGRESS_METERS) {
            return DirectionAssessment.forward(delta);
        }
        return delta <= -MIN_PROGRESS_METERS
                ? DirectionAssessment.backward(delta) : DirectionAssessment.stalled(delta);
    }

    private static RouteProgressSample selectAnchor(ArrayDeque<RouteProgressSample> samples,
            long nowMs, double along) {
        RouteProgressSample recent = latestEligible(samples, nowMs);
        if (recent == null) {
            return null;
        }
        double delta = along - recent.alongTrackMeters;
        if (Math.abs(delta) < MAX_JITTER_METERS || Math.abs(delta) >= MIN_PROGRESS_METERS) {
            return recent;
        }
        return accumulatedAnchor(samples, recent, along, delta, nowMs);
    }

    private static RouteProgressSample latestEligible(ArrayDeque<RouteProgressSample> samples, long nowMs) {
        Iterator<RouteProgressSample> candidates = samples.descendingIterator();
        while (candidates.hasNext()) {
            RouteProgressSample sample = candidates.next();
            if (nowMs - sample.timeMs >= WINDOW_MS) {
                return sample;
            }
        }
        return null;
    }

    private static RouteProgressSample accumulatedAnchor(ArrayDeque<RouteProgressSample> samples,
            RouteProgressSample recent, double along, double recentDelta, long nowMs) {
        Iterator<RouteProgressSample> candidates = samples.descendingIterator();
        while (candidates.hasNext()) {
            RouteProgressSample candidate = candidates.next();
            double delta = along - candidate.alongTrackMeters;
            if (nowMs - candidate.timeMs >= WINDOW_MS
                    && Math.abs(delta) >= MIN_PROGRESS_METERS && delta * recentDelta > 0) {
                return candidate;
            }
        }
        return recent;
    }
}
