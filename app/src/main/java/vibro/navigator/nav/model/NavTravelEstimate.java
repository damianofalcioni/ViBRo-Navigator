package vibro.navigator.nav.model;

/** Destination progress in SI units. NaN duration means that an ETA is unavailable. */
public final class NavTravelEstimate {
    public final double distanceMeters;
    public final double timeSeconds;
    public final long observationWallTimeMs;
    public final boolean destinationReached;

    public NavTravelEstimate(double distanceMeters, double timeSeconds,
            long observationWallTimeMs, boolean destinationReached) {
        this.distanceMeters = Math.max(0, distanceMeters);
        this.timeSeconds = timeSeconds;
        this.observationWallTimeMs = observationWallTimeMs;
        this.destinationReached = destinationReached;
    }
}
