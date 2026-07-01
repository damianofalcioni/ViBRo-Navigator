package vibro.navigator.nav.compass;

public final class CompassRadiusMetrics {
    public final float visibleRadiusMeters;
    public final float fullRouteVisibleRadiusMeters;
    public final float movingScaleVisibleRadiusMeters;
    public final float accuracyRadiusMeters;
    public final float routeThresholdMeters;

    public CompassRadiusMetrics(
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
}
