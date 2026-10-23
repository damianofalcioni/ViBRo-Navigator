package vibro.navigator.nav.location;

/** Optional low-power, one-shot motion wake-up. Callbacks are serialized with navigation. */
public interface NavigationMotionTrigger {
    boolean arm(Runnable onMotion);

    void cancel();
}
