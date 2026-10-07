package vibro.navigator.nav.kalman;

/**
 * Minimal constant-velocity Kalman filter (position+velocity) for one axis.
 */
public final class Kalman1D {
    private double x;  // position
    private double v;  // velocity

    private double p00 = 1, p01 = 0, p10 = 0, p11 = 1; // covariance

    private final double processNoise; // acceleration noise (m/s^2)

    public Kalman1D(double processNoise) {
        this.processNoise = processNoise;
    }

    public void reset(double position, double velocity) {
        reset(position, velocity, 10, 10);
    }

    public void reset(double position, double velocity, double positionVariance, double velocityVariance) {
        requireFinite(position);
        requireFinite(velocity);
        requireVariance(positionVariance);
        requireVariance(velocityVariance);
        x = position;
        v = velocity;
        p00 = positionVariance;
        p01 = 0;
        p10 = 0;
        p11 = velocityVariance;
    }

    public void predict(double dtSeconds) {
        requireFinite(dtSeconds);
        if (dtSeconds <= 0) {
            return;
        }
        x = x + v * dtSeconds;

        // State transition F = [[1, dt],[0,1]]
        // Process noise Q for constant acceleration model
        double dt = dtSeconds;
        double q = processNoise * processNoise;
        double q00 = 0.25 * dt * dt * dt * dt * q;
        double q01 = 0.5 * dt * dt * dt * q;
        double q11 = dt * dt * q;

        double n00 = p00 + dt * (p10 + p01) + dt * dt * p11 + q00;
        double n01 = p01 + dt * p11 + q01;
        double n10 = p10 + dt * p11 + q01;
        double n11 = p11 + q11;

        p00 = n00;
        p01 = n01;
        p10 = n10;
        p11 = n11;
    }

    public void update(double measuredPosition, double measurementVariance) {
        requireFinite(measuredPosition);
        requireVariance(measurementVariance);
        double r = Math.max(1e-3, measurementVariance);
        double s = p00 + r;
        double k0 = p00 / s;
        double k1 = p10 / s;

        double y = measuredPosition - x;
        x = x + k0 * y;
        v = v + k1 * y;

        // Joseph covariance update remains stable when gains approach one.
        double a = 1 - k0;
        double n00 = a * a * p00 + k0 * k0 * r;
        double n01 = a * (p01 - k1 * p00) + k0 * k1 * r;
        double n10 = n01;
        double n11 = p11 - k1 * (p01 + p10) + k1 * k1 * (p00 + r);

        p00 = n00;
        p01 = n01;
        p10 = n10;
        p11 = n11;
    }

    public double position() {
        return x;
    }

    public double velocity() {
        return v;
    }

    /** Position corrections must not invent a travel velocity. */
    public void predictPosition(double displacement, double addedVariance) {
        requireFinite(displacement);
        requireVariance(addedVariance);
        x += displacement;
        p00 += addedVariance;
        v = 0;
        p01 = p10 = p11 = 0;
    }

    public double positionVariance() {
        return p00;
    }

    private static void requireFinite(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Filter input must be finite");
        }
    }

    private static void requireVariance(double value) {
        requireFinite(value);
        if (value < 0) {
            throw new IllegalArgumentException("Variance must be nonnegative");
        }
    }
}
