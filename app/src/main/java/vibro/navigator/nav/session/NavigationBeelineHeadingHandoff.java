package vibro.navigator.nav.session;

import vibro.navigator.nav.guidance.NavigationRouteProgressTracker;
import vibro.navigator.nav.guidance.NavigationRouteProgressTracker.DirectionStatus;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.PolylineIndex;

/** Display-source handoff based only on accepted road progress, independent of UI refreshes. */
final class NavigationBeelineHeadingHandoff {
    private final NavigationRouteDirectGuidance directGuidance;
    private final NavigationRouteProgressTracker progressTracker;
    private boolean awaitingForward;
    private long roadStartedMs = -1L;

    NavigationBeelineHeadingHandoff(NavigationRouteDirectGuidance directGuidance,
            NavigationRouteProgressTracker progressTracker) {
        this.directGuidance = directGuidance;
        this.progressTracker = progressTracker;
    }

    boolean usesLocationHeading() {
        return directGuidance.activeTarget() != null || awaitingForward;
    }

    void reset() {
        awaitingForward = false;
        roadStartedMs = -1L;
    }

    void onRouteApplied(boolean wasBeeline) {
        awaitingForward = wasBeeline || directGuidance.activeTarget() != null;
        roadStartedMs = -1L;
    }

    void onLocationEvaluated(boolean wasBeeline, NavigationLocation location,
            boolean likelyStationary, NavigationRouteEvaluation evaluation, long nowMs) {
        if (wasBeeline || directGuidance.activeTarget() != null) {
            // Finishing a beeline must not count its incoming displacement as road progress.
            awaitingForward = true;
            roadStartedMs = nowMs;
            return;
        }
        if (!awaitingForward || likelyStationary || !evaluation.isStableOnRouteSample()) {
            return;
        }
        if (isForward(location, nowMs)) {
            awaitingForward = false;
        }
    }

    private boolean isForward(NavigationLocation location, long nowMs) {
        if (roadStartedMs < 0L) {
            roadStartedMs = nowMs;
            return false;
        }
        PolylineIndex.Match match = directGuidance.resolveRouteMatch(location,
                location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE);
        return match != null && progressTracker.assessDirection(match.alongTrackMeters, nowMs, roadStartedMs).status
                == DirectionStatus.FORWARD;
    }
}
