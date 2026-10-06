package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;

/** Reuses an adjusted viewport across heading snapshots without retaining stale navigation state. */
public final class CompassDisplayStateCache {
    private NavCompassState source;
    private NavCompassState displayed;
    private boolean movingScale;
    private float radiusMeters;

    @NonNull
    public NavCompassState resolve(@NonNull NavCompassState state, boolean moving, float radius) {
        if (displayed == null || movingScale != moving || radiusMeters != radius || !sameGeometry(state)) {
            displayed = state.withDisplayMode(moving, radius);
        } else if (source != state) {
            displayed = NavCompassHeadingRefresh.apply(displayed,
                    (double) state.displayMode.headingDegrees, state.displayMode.headingAccuracyDegrees);
        }
        source = state;
        movingScale = moving;
        radiusMeters = radius;
        return displayed;
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private boolean sameGeometry(NavCompassState state) {
        // Structural assembly creates new radius/projection objects; heading copies share their identities.
        // List equality would traverse and project a potentially long route on each sensor update.
        return source != null && state.radiusState == source.radiusState
                && state.routePoints == source.routePoints && state.streetOverlay == source.streetOverlay;
    }

    public void clear() {
        source = null;
        displayed = null;
    }
}
