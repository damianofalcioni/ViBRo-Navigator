package vibro.navigator.nav.location;

public interface NavigationLocationRecoveryAlarm {
    boolean schedule(long triggerElapsedRealtimeMs);

    void cancel();
}
