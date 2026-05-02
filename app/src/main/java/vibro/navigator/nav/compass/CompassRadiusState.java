package vibro.navigator.nav.compass;

public final class CompassRadiusState {
    public final float visibleRadiusMeters;
    public final float fullRouteVisibleRadiusMeters;
    public final float sixtySecondVisibleRadiusMeters;
    public final float accuracyRadiusMeters;
    public final float routeThresholdMeters;

    public CompassRadiusState(
            float visibleRadiusMeters,
            float fullRouteVisibleRadiusMeters,
            float sixtySecondVisibleRadiusMeters,
            float accuracyRadiusMeters,
            float routeThresholdMeters
    ) {
        this.visibleRadiusMeters = visibleRadiusMeters;
        this.fullRouteVisibleRadiusMeters = fullRouteVisibleRadiusMeters;
        this.sixtySecondVisibleRadiusMeters = sixtySecondVisibleRadiusMeters;
        this.accuracyRadiusMeters = accuracyRadiusMeters;
        this.routeThresholdMeters = routeThresholdMeters;
    }

    public float targetVisibleRadiusMeters(boolean sixtySecondView) {
        return sixtySecondView ? sixtySecondVisibleRadiusMeters : fullRouteVisibleRadiusMeters;
    }
}
