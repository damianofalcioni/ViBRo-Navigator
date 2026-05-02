package vibro.navigator.nav.compass;

public final class CompassProgressLabels {
    public final float destinationEastMeters;
    public final float destinationNorthMeters;
    public final boolean destinationWithinRadius;

    public CompassProgressLabels(
            float destinationEastMeters,
            float destinationNorthMeters,
            boolean destinationWithinRadius
    ) {
        this.destinationEastMeters = destinationEastMeters;
        this.destinationNorthMeters = destinationNorthMeters;
        this.destinationWithinRadius = destinationWithinRadius;
    }
}
