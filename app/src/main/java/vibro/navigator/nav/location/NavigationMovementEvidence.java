package vibro.navigator.nav.location;

import java.util.ArrayDeque;
import java.util.Objects;

/** Corroborate metadata-poor walking speed with sustained, directional position changes. */
final class NavigationMovementEvidence {
    private static final long WINDOW_MS = 20_000L;
    private static final long MAX_GAP_MS = 10_000L;
    private static final long MIN_SPAN_MS = 6_000L;
    // Filtered uncertainty includes lag as well as the native radius; coherent travel can recover
    // before the filter becomes route-grade again, without trusting a lone poor-position jump.
    private static final float MAX_COHERENT_ACCURACY_METERS = 50f;
    private final ArrayDeque<NavigationLocation> samples = new ArrayDeque<>();
    private NavigationLocation reference;

    void reset() {
        samples.clear();
        reference = null;
    }

    boolean record(NavigationLocation location) {
        NavigationLocation last = samples.peekLast();
        if (last != null && location.getElapsedRealtimeOrTimeMs() <= last.getElapsedRealtimeOrTimeMs()) {
            return reference != null;
        }
        if (startsNewWindow(last, location)) {
            reset();
        }
        reference = null;
        if (!isCandidate(location)) {
            samples.clear();
            return false;
        }
        samples.addLast(new NavigationLocation(location));
        long cutoff = location.getElapsedRealtimeOrTimeMs() - WINDOW_MS;
        while (samples.size() > 1 && samples.peekFirst().getElapsedRealtimeOrTimeMs() < cutoff) {
            samples.removeFirst();
        }
        return resolve(location);
    }

    NavigationLocation reference() {
        return reference;
    }

    private static boolean startsNewWindow(NavigationLocation last, NavigationLocation location) {
        return last != null && (location.getElapsedRealtimeOrTimeMs() - last.getElapsedRealtimeOrTimeMs() > MAX_GAP_MS
                || !Objects.equals(last.getProvider(), location.getProvider()));
    }

    private boolean resolve(NavigationLocation location) {
        NavigationLocation first = samples.peekFirst();
        if (samples.size() < 3 || location.getElapsedRealtimeOrTimeMs() - first.getElapsedRealtimeOrTimeMs() < MIN_SPAN_MS) {
            return false;
        }
        double net = first.distanceTo(location);
        double threshold = Math.max(3, Math.min(8, first.getAccuracy() + location.getAccuracy()));
        double path = 0;
        NavigationLocation previous = first;
        for (NavigationLocation sample : samples) {
            path += previous.distanceTo(sample);
            previous = sample;
        }
        double seconds = (location.getElapsedRealtimeOrTimeMs() - first.getElapsedRealtimeOrTimeMs()) / 1000.0;
        boolean coherent = net >= threshold && net >= path * 0.8
                && net / seconds <= Math.max(3, location.getSpeed() * 3);
        reference = coherent ? first : null;
        return coherent;
    }

    private static boolean isCandidate(NavigationLocation location) {
        return location.hasSpeed() && !location.hasSpeedAccuracy()
                && Float.isFinite(location.getSpeed()) && location.getSpeed() > 0.35f
                && location.hasAccuracy() && Float.isFinite(location.getAccuracy())
                && location.getAccuracy() >= 0 && location.getAccuracy() <= MAX_COHERENT_ACCURACY_METERS;
    }
}
