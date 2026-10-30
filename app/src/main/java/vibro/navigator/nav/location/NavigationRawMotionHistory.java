package vibro.navigator.nav.location;

import java.util.ArrayDeque;
import java.util.Iterator;

/** Raw stop evidence must not be manufactured by a lagging displayed position. */
final class NavigationRawMotionHistory {
    private static final long MIN_STOP_SPAN_MS = 1_000L;
    private static final long MAX_STOP_GAP_MS = 10_000L;
    private final ArrayDeque<NavigationLocation> samples = new ArrayDeque<>();

    void reset() {
        samples.clear();
    }

    boolean isNew(NavigationLocation location) {
        return samples.isEmpty() || location.getElapsedRealtimeOrTimeMs()
                > samples.peekLast().getElapsedRealtimeOrTimeMs();
    }

    void record(NavigationLocation location) {
        samples.addLast(new NavigationLocation(location));
        while (samples.size() > 1 && location.getElapsedRealtimeOrTimeMs()
                - samples.peekFirst().getElapsedRealtimeOrTimeMs() > MAX_STOP_GAP_MS) {
            samples.removeFirst();
        }
    }

    boolean isStationaryCandidate() {
        return hasStopEvidence(true);
    }

    boolean hasStationaryPositions() {
        return hasStopEvidence(false);
    }

    private boolean hasStopEvidence(boolean requireStoppedSpeed) {
        NavigationLocation current = samples.peekLast();
        long spanMs = 0;
        Iterator<NavigationLocation> candidates = samples.descendingIterator();
        while (candidates.hasNext()) {
            NavigationLocation sample = candidates.next();
            if (requireStoppedSpeed && !allowsStop(sample)) {
                // A measured movement ends the preceding stop window. A subsequent
                // native stop may establish its own independently timed evidence.
                return spanMs >= MIN_STOP_SPAN_MS;
            }
            if (!isNearbyUsablePosition(sample, current)) {
                return false;
            }
            spanMs = current.getElapsedRealtimeOrTimeMs() - sample.getElapsedRealtimeOrTimeMs();
            if (!requireStoppedSpeed && spanMs >= MIN_STOP_SPAN_MS) {
                return true;
            }
        }
        // Do not renew a stop from only the latest nearby pair while the retained
        // raw positions show sustained displacement across the observation window.
        return spanMs >= MIN_STOP_SPAN_MS;
    }

    private static boolean isNearbyUsablePosition(NavigationLocation sample, NavigationLocation current) {
        return usablePosition(sample) && sample.distanceTo(current) <= 0.8;
    }

    private static boolean allowsStop(NavigationLocation location) {
        return !location.hasSpeed() || (Float.isFinite(location.getSpeed())
                && location.getSpeed() >= 0 && location.getSpeed() <= 0.35f
                && !NavigationNativeSpeedEvidence.hasConfidentPositiveSpeed(location));
    }

    private static boolean usablePosition(NavigationLocation location) {
        return location.hasAccuracy() && Float.isFinite(location.getProviderAccuracy())
                && location.getProviderAccuracy() >= 0 && location.getProviderAccuracy() <= 25;
    }
}
