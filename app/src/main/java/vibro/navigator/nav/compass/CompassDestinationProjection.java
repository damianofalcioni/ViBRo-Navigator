package vibro.navigator.nav.compass;

public final class CompassDestinationProjection {
    public final float eastMeters;
    public final float northMeters;
    public final float reachedRadiusMeters;
    public final boolean withinRadius;

    public CompassDestinationProjection(
            float eastMeters,
            float northMeters,
            float reachedRadiusMeters,
            boolean withinRadius
    ) {
        this.eastMeters = eastMeters;
        this.northMeters = northMeters;
        this.reachedRadiusMeters = reachedRadiusMeters;
        this.withinRadius = withinRadius;
    }
}
