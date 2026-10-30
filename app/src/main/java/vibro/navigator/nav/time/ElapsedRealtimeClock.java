package vibro.navigator.nav.time;

public interface ElapsedRealtimeClock {
    long elapsedRealtimeMs();

    default long uptimeMs() { return elapsedRealtimeMs(); }
}
