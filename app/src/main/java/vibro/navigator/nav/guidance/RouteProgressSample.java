package vibro.navigator.nav.guidance;

final class RouteProgressSample {
    final double alongTrackMeters;
    final long timeMs;

    RouteProgressSample(double alongTrackMeters, long timeMs) {
        this.alongTrackMeters = alongTrackMeters;
        this.timeMs = timeMs;
    }
}
