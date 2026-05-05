package vibro.navigator.nav.compass;

public final class CompassDestinationProjection {
    public final float eastMeters;
    public final float northMeters;
    public final boolean withinRadius;

    public CompassDestinationProjection(
            float eastMeters,
            float northMeters,
            boolean withinRadius
    ) {
        this.eastMeters = eastMeters;
        this.northMeters = northMeters;
        this.withinRadius = withinRadius;
    }
}
