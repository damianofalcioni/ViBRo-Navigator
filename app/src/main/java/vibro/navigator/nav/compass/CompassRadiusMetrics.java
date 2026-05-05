package vibro.navigator.nav.compass;

public final class CompassRadiusMetrics {
    public final float visibleRadiusMeters;
    public final float fullRouteVisibleRadiusMeters;
    public final float sixtySecondVisibleRadiusMeters;
    public final float accuracyRadiusMeters;
    public final float routeThresholdMeters;

    public CompassRadiusMetrics(
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
}
