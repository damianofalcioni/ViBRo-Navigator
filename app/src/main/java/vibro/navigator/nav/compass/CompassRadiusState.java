package vibro.navigator.nav.compass;

public final class CompassRadiusState {
    public final float visibleRadiusMeters;
    public final float fullRouteVisibleRadiusMeters;
    public final float movingScaleVisibleRadiusMeters;
    public final float accuracyRadiusMeters;
    public final float routeThresholdMeters;

    public CompassRadiusState(
            float visibleRadiusMeters,
            float fullRouteVisibleRadiusMeters,
            float movingScaleVisibleRadiusMeters,
            float accuracyRadiusMeters,
            float routeThresholdMeters
    ) {
        this.visibleRadiusMeters = visibleRadiusMeters;
        this.fullRouteVisibleRadiusMeters = fullRouteVisibleRadiusMeters;
        this.movingScaleVisibleRadiusMeters = movingScaleVisibleRadiusMeters;
        this.accuracyRadiusMeters = accuracyRadiusMeters;
        this.routeThresholdMeters = routeThresholdMeters;
    }

    public float targetVisibleRadiusMeters(boolean movingScaleView) {
        return movingScaleView ? movingScaleVisibleRadiusMeters : fullRouteVisibleRadiusMeters;
    }
}
