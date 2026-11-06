package vibro.navigator.nav.location;

/** Unknown movement requires verification; it is neither a confirmed stop nor travel. */
public enum NavigationMotionState {
    UNKNOWN,
    STATIONARY,
    MOVING;

    public static final long MAX_EVIDENCE_AGE_MS = 10_000L;

    public static boolean isFresh(long measurementMs, long nowMs) {
        return measurementMs >= 0 && measurementMs <= nowMs && nowMs - measurementMs <= MAX_EVIDENCE_AGE_MS;
    }

    public boolean isKnown() { return this != UNKNOWN; }

    public boolean isMoving() { return this == MOVING; }
}
