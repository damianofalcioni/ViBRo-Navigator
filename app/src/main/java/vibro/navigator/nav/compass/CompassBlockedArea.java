package vibro.navigator.nav.compass;

public final class CompassBlockedArea {
    public final float eastMeters;
    public final float northMeters;
    public final float radiusMeters;

    public CompassBlockedArea(float eastMeters, float northMeters, float radiusMeters) {
        this.eastMeters = eastMeters;
        this.northMeters = northMeters;
        this.radiusMeters = radiusMeters;
    }
}
