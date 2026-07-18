package vibro.navigator.nav.guidance;

public final class RouteMotionEstimate {
    static final float UNKNOWN_ACCELERATION_MPS2 = Float.NaN;

    public final float speedMps;
    public final float accelerationMps2;

    private RouteMotionEstimate(float speedMps, float accelerationMps2) {
        this.speedMps = sanitizeSpeed(speedMps);
        this.accelerationMps2 = sanitizeAcceleration(accelerationMps2);
    }

    public static RouteMotionEstimate stationary() {
        return new RouteMotionEstimate(0f, UNKNOWN_ACCELERATION_MPS2);
    }

    public static RouteMotionEstimate speedOnly(float speedMps) {
        return new RouteMotionEstimate(speedMps, UNKNOWN_ACCELERATION_MPS2);
    }

    public static RouteMotionEstimate withAcceleration(float speedMps, float accelerationMps2) {
        return new RouteMotionEstimate(speedMps, accelerationMps2);
    }

    boolean hasUsableSpeed(double minSpeedMps) {
        return Float.isFinite(speedMps) && speedMps >= minSpeedMps;
    }

    boolean hasAcceleration() {
        return Float.isFinite(accelerationMps2);
    }

    private static float sanitizeSpeed(float speedMps) {
        return Float.isFinite(speedMps) && speedMps > 0f ? speedMps : 0f;
    }

    private static float sanitizeAcceleration(float accelerationMps2) {
        return Float.isFinite(accelerationMps2) ? accelerationMps2 : UNKNOWN_ACCELERATION_MPS2;
    }
}
