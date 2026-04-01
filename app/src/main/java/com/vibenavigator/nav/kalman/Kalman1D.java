package com.vibenavigator.nav.kalman;

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
        x = position;
        v = velocity;
        p00 = 10;
        p01 = 0;
        p10 = 0;
        p11 = 10;
    }

    public void predict(double dtSeconds) {
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
        double r = Math.max(1e-3, measurementVariance);
        double s = p00 + r;
        double k0 = p00 / s;
        double k1 = p10 / s;

        double y = measuredPosition - x;
        x = x + k0 * y;
        v = v + k1 * y;

        double n00 = (1 - k0) * p00;
        double n01 = (1 - k0) * p01;
        double n10 = p10 - k1 * p00;
        double n11 = p11 - k1 * p01;

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
}
