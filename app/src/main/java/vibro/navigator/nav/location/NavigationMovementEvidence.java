package vibro.navigator.nav.location;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;

/** Corroborate raw movement with sustained, directional position changes. */
public final class NavigationMovementEvidence {
    private static final long WINDOW_MS = 150_000L;
    private static final long MAX_GAP_MS = 65_000L;
    private static final long MIN_SPAN_MS = 6_000L;
    private static final long MIN_SAMPLE_SPACING_MS = 2_000L;
    private static final float MIN_PROGRESS_SPEED_MPS = 0.2f;
    // Native observation quality is separate from added display/filter lag. Several independent
    // same-provider samples are required; a correction alone cannot become travel.
    private static final float MAX_COHERENT_ACCURACY_METERS = 100f;
    // With no native speed, require accurate positions and exceed both endpoint radii.
    private static final float MAX_SPEEDLESS_ACCURACY_METERS = 25f;
    private final ArrayDeque<NavigationLocation> samples = new ArrayDeque<>();
    private NavigationLocation reference;

    public void reset() {
        samples.clear();
        reference = null;
    }

    public boolean record(NavigationLocation location) {
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
        pruneSamples(location.getElapsedRealtimeOrTimeMs());
        if (!hasRecentProgress(location)) {
            // Old displacement cannot corroborate a fresh, contradictory nonzero speed.
            samples.clear();
            samples.addLast(new NavigationLocation(location));
            return false;
        }
        return resolve(location);
    }

    private void pruneSamples(long timeMs) {
        long cutoff = timeMs - WINDOW_MS;
        while (samples.size() > 1 && samples.peekFirst().getElapsedRealtimeOrTimeMs() < cutoff) {
            samples.removeFirst();
        }
    }

    NavigationLocation reference() {
        return reference;
    }

    private static double movementThreshold(NavigationLocation first, NavigationLocation last) {
        if (!first.hasSpeed() || !last.hasSpeed()) {
            return Math.max(3, first.getProviderAccuracy() + last.getProviderAccuracy());
        }
        double accuracy = Math.max(first.getProviderAccuracy(), last.getProviderAccuracy());
        return accuracy > 25 ? Math.max(8, accuracy * 0.5) : Math.max(3, Math.min(8, accuracy * 0.5));
    }

    private static boolean startsNewWindow(NavigationLocation last, NavigationLocation location) {
        return last != null && (location.getElapsedRealtimeOrTimeMs() - last.getElapsedRealtimeOrTimeMs() > MAX_GAP_MS
                || !Objects.equals(last.getProvider(), location.getProvider()));
    }

    private boolean resolve(NavigationLocation location) {
        // A preceding leg must not invalidate a corroborated new direction. Retain
        // the longest still-coherent span for low-speed display confirmation too.
        Iterator<NavigationLocation> recent = samples.descendingIterator();
        double path = 0;
        NavigationLocation previous = location;
        long independentAtMs = location.getElapsedRealtimeOrTimeMs() + MIN_SAMPLE_SPACING_MS;
        int independentSamples = 0;
        while (recent.hasNext()) {
            NavigationLocation anchor = recent.next();
            path += previous.distanceTo(anchor);
            previous = anchor;
            if (independentAtMs - anchor.getElapsedRealtimeOrTimeMs() >= MIN_SAMPLE_SPACING_MS) {
                independentAtMs = anchor.getElapsedRealtimeOrTimeMs();
                independentSamples++;
            }
            if (independentSamples >= 3 && isCoherent(anchor, location, path)) {
                reference = anchor;
            }
        }
        return reference != null;
    }

    private static boolean isCoherent(NavigationLocation first, NavigationLocation location, double path) {
        long spanMs = location.getElapsedRealtimeOrTimeMs() - first.getElapsedRealtimeOrTimeMs();
        if (spanMs < MIN_SPAN_MS) {
            return false;
        }
        double net = first.distanceTo(location);
        double seconds = spanMs / 1000.0;
        return net >= movementThreshold(first, location) && net >= path * 0.8 && net / seconds >= MIN_PROGRESS_SPEED_MPS
                && net / seconds <= Math.max(3, location.getSpeed() * 3);
    }

    private boolean hasRecentProgress(NavigationLocation location) {
        Iterator<NavigationLocation> recent = samples.descendingIterator();
        while (recent.hasNext()) {
            NavigationLocation anchor = recent.next();
            long elapsedMs = location.getElapsedRealtimeOrTimeMs() - anchor.getElapsedRealtimeOrTimeMs();
            if (elapsedMs >= MIN_SAMPLE_SPACING_MS) {
                return anchor.distanceTo(location) >= MIN_PROGRESS_SPEED_MPS * elapsedMs / 1_000.0;
            }
        }
        return true;
    }

    private static boolean isCandidate(NavigationLocation location) {
        return allowsSpeed(location) && location.hasAccuracy() && Float.isFinite(location.getAccuracy())
                && location.getProviderAccuracy() >= 0
                && location.getProviderAccuracy() <= (location.hasSpeed()
                ? MAX_COHERENT_ACCURACY_METERS : MAX_SPEEDLESS_ACCURACY_METERS);
    }

    private static boolean allowsSpeed(NavigationLocation location) {
        return !location.hasSpeed() || (allowsSpeedAccuracy(location)
                && Float.isFinite(location.getSpeed()) && location.getSpeed() >= MIN_PROGRESS_SPEED_MPS);
    }

    private static boolean allowsSpeedAccuracy(NavigationLocation location) {
        return !location.hasSpeedAccuracy() || NavigationNativeSpeedEvidence.hasConfidentPositiveSpeed(location);
    }
}
