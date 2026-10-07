package vibro.navigator.nav.kalman;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.nav.location.NavigationLocation;

/** Position corrections never invent velocity; prediction uses a trusted measured course. */
public final class LatLonKalmanFilter {
    private static final double METERS_PER_DEGREE = 111320.0;
    private static final double REBASE_DISTANCE_METERS = 10_000;
    private static final double STATIONARY_VARIANCE_PER_SECOND = 0.01;
    private final Kalman1D xFilter = new Kalman1D(1);
    private final Kalman1D yFilter = new Kalman1D(1);
    private final LocationJumpGate jumpGate = new LocationJumpGate();
    @Nullable private NavigationLocation lastRaw;
    @Nullable private String lastRejectionReason;
    private double refLat;
    private double refLon;
    private double longitudeScale;

    public void reset() {
        lastRaw = null;
        jumpGate.reset();
        lastRejectionReason = null;
    }

    @Nullable
    public String getLastRejectionReason() {
        return lastRejectionReason;
    }

    @Nullable
    public NavigationLocation update(@NonNull NavigationLocation in) {
        lastRejectionReason = null;
        if (!validObservation(in)) {
            return null;
        }
        if (lastRaw == null) {
            return initialize(in);
        }
        long t = in.getElapsedRealtimeOrTimeMs();
        if (t == lastRaw.getElapsedRealtimeOrTimeMs()) {
            return replaceObservation(in);
        }
        double dt = (t - lastRaw.getElapsedRealtimeOrTimeMs()) / 1000.0;
        LocationJumpGate.Result jump = jumpGate.evaluate(lastRaw, in, dt);
        if (jump == LocationJumpGate.Result.REJECT) {
            return reject("unconfirmed position jump");
        }
        if (needsInitialization(in, dt, jump)) {
            return initialize(in);
        }
        return smooth(in, dt);
    }

    private boolean validObservation(NavigationLocation in) {
        if (!LocationFilterPolicy.isValid(in)) {
            reject("invalid coordinates or accuracy");
            return false;
        }
        if (lastRaw != null && in.getElapsedRealtimeOrTimeMs() < lastRaw.getElapsedRealtimeOrTimeMs()) {
            reject("out-of-order fix");
            return false;
        }
        return true;
    }

    @Nullable
    private NavigationLocation replaceObservation(NavigationLocation in) {
        // Replace an improved observation; never count the same measurement twice.
        if (jumpGate.evaluate(lastRaw, in, 0) == LocationJumpGate.Result.REJECT) {
            return reject("unconfirmed position jump");
        }
        return LocationFilterPolicy.accuracy(in) < LocationFilterPolicy.accuracy(lastRaw)
                ? initialize(in) : reject("duplicate fix time");
    }

    private boolean needsInitialization(NavigationLocation in, double dt, LocationJumpGate.Result jump) {
        return jump == LocationJumpGate.Result.REINITIALIZE
                || LocationFilterPolicy.shouldReinitialize(lastRaw, in, dt)
                || GeoMath.distanceMeters(refLat, refLon, in.getLatitude(), in.getLongitude()) > REBASE_DISTANCE_METERS;
    }

    private NavigationLocation smooth(NavigationLocation in, double dt) {
        predict(in, dt);
        double variance = LocationFilterPolicy.measurementVariance(in);
        double x = normalizeLongitude(in.getLongitude() - refLon) * longitudeScale;
        double y = (in.getLatitude() - refLat) * METERS_PER_DEGREE;
        xFilter.update(x, variance);
        yFilter.update(y, variance);
        boundLag(x, y, LocationFilterPolicy.accuracy(in), variance);
        NavigationLocation out = new NavigationLocation(in);
        out.setLatitude(refLat + yFilter.position() / METERS_PER_DEGREE);
        out.setLongitude(normalizeLongitude(refLon + xFilter.position() / longitudeScale));
        if (!LocationFilterPolicy.isValid(out)) {
            return initialize(in);
        }
        // Translate the incoming confidence region conservatively to the displayed center.
        if (in.hasAccuracy()) {
            out.setFilteredAccuracy((float) (in.getAccuracy() + in.distanceTo(out)));
        }
        lastRaw = new NavigationLocation(in);
        return out;
    }

    private void predict(NavigationLocation in, double dt) {
        double eastDisplacement = 0;
        double northDisplacement = 0;
        double processVariance = STATIONARY_VARIANCE_PER_SECOND * dt;
        if (!LocationFilterPolicy.stationary(in)) {
            // A random walk prevents coordinate corrections from becoming inferred momentum.
            double travel = Math.max(1, LocationFilterPolicy.speed(in)) * dt;
            processVariance = Math.max(dt, travel * travel);
            if (LocationFilterPolicy.trustedCourse(in) && LocationFilterPolicy.trustedCourse(lastRaw)) {
                double bearing = Math.toRadians(lastRaw.getBearing());
                eastDisplacement = lastRaw.getSpeed() * Math.sin(bearing) * dt;
                northDisplacement = lastRaw.getSpeed() * Math.cos(bearing) * dt;
            }
        }
        xFilter.predictPosition(eastDisplacement, processVariance);
        yFilter.predictPosition(northDisplacement, processVariance);
    }

    private void boundLag(double x, double y, double accuracy, double variance) {
        double dx = xFilter.position() - x;
        double dy = yFilter.position() - y;
        double shift = Math.hypot(dx, dy);
        if (shift > accuracy) {
            double scale = accuracy / shift;
            xFilter.reset(x + dx * scale, 0, variance, 0);
            yFilter.reset(y + dy * scale, 0, variance, 0);
        }
    }

    @NonNull
    private NavigationLocation initialize(NavigationLocation in) {
        refLat = in.getLatitude();
        refLon = in.getLongitude();
        longitudeScale = METERS_PER_DEGREE * Math.max(1e-6, Math.cos(Math.toRadians(refLat)));
        double variance = LocationFilterPolicy.measurementVariance(in);
        xFilter.reset(0, 0, variance, 0);
        yFilter.reset(0, 0, variance, 0);
        lastRaw = new NavigationLocation(in);
        jumpGate.reset();
        return new NavigationLocation(in);
    }

    @Nullable
    private NavigationLocation reject(String reason) {
        lastRejectionReason = reason;
        return null;
    }

    private static double normalizeLongitude(double lon) {
        if (lon >= -180 && lon < 180) {
            return lon;
        }
        return ((lon + 180) % 360 + 360) % 360 - 180;
    }
}
