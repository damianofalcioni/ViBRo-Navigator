package vibro.navigator.nav.kalman;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;

public final class LatLonKalmanFilter {

    private final Kalman1D xFilter = new Kalman1D(1.0);
    private final Kalman1D yFilter = new Kalman1D(1.0);

    private boolean initialized;
    private long lastTimeMs;
    private double refLat;

    public void reset() {
        initialized = false;
        lastTimeMs = 0L;
        refLat = 0.0;
    }

    @Nullable
    public Location update(@NonNull Location in) {
        long t = in.getTime();
        if (!initialized) {
            refLat = in.getLatitude();
            double[] xy = toXY(in.getLatitude(), in.getLongitude());
            xFilter.reset(xy[0], 0);
            yFilter.reset(xy[1], 0);
            initialized = true;
            lastTimeMs = t;
            return copyWithLatLon(in, in.getLatitude(), in.getLongitude());
        }

        double dt = Math.max(0.0, (t - lastTimeMs) / 1000.0);
        lastTimeMs = t;

        xFilter.predict(dt);
        yFilter.predict(dt);

        double[] xy = toXY(in.getLatitude(), in.getLongitude());
        double acc = Math.max(5.0, in.hasAccuracy() ? in.getAccuracy() : 20.0);
        double var = acc * acc;
        xFilter.update(xy[0], var);
        yFilter.update(xy[1], var);

        LatLon ll = fromXY(xFilter.position(), yFilter.position());
        return copyWithLatLon(in, ll.lat, ll.lon);
    }

    private double[] toXY(double lat, double lon) {
        double kx = 111320.0 * Math.cos(Math.toRadians(refLat));
        double ky = 111320.0;
        double x = lon * kx;
        double y = lat * ky;
        return new double[]{x, y};
    }

    private LatLon fromXY(double x, double y) {
        double kx = 111320.0 * Math.cos(Math.toRadians(refLat));
        double ky = 111320.0;
        double lon = x / kx;
        double lat = y / ky;
        return new LatLon(lat, lon);
    }

    private static Location copyWithLatLon(@NonNull Location base, double lat, double lon) {
        Location out = new Location(base);
        out.setLatitude(lat);
        out.setLongitude(lon);
        return out;
    }
}
