package vibro.navigator.nav.route;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;

import java.util.ArrayList;
import java.util.List;

public final class PolylineIndex {

    public static final class Match {
        public final double distanceToTrackMeters;
        public final double alongTrackMeters;
        public final double segmentBearingDegrees;
        public final int segmentIndex;

        Match(double distanceToTrackMeters, double alongTrackMeters, double segmentBearingDegrees, int segmentIndex) {
            this.distanceToTrackMeters = distanceToTrackMeters;
            this.alongTrackMeters = alongTrackMeters;
            this.segmentBearingDegrees = segmentBearingDegrees;
            this.segmentIndex = segmentIndex;
        }
    }

    private final List<LatLon> pts;
    private final double[] cumulative;

    public PolylineIndex(@NonNull List<LatLon> points) {
        this.pts = new ArrayList<>(points);
        this.cumulative = new double[Math.max(1, points.size())];
        double sum = 0;
        for (int i = 1; i < points.size(); i++) {
            LatLon a = points.get(i - 1);
            LatLon b = points.get(i);
            sum += GeoMath.distanceMeters(a.lat, a.lon, b.lat, b.lon);
            cumulative[i] = sum;
        }
    }

    public double totalLengthMeters() {
        return cumulative.length == 0 ? 0 : cumulative[cumulative.length - 1];
    }

    public double distanceAtPointIndex(int idx) {
        if (idx <= 0) return 0;
        if (idx >= cumulative.length) return totalLengthMeters();
        return cumulative[idx];
    }

    @Nullable
    public LatLon pointAtDistance(double alongTrackMeters) {
        if (pts.isEmpty()) {
            return null;
        }
        if (pts.size() == 1 || alongTrackMeters <= 0.0) {
            return pts.get(0);
        }

        double totalLength = totalLengthMeters();
        if (alongTrackMeters >= totalLength) {
            return pts.get(pts.size() - 1);
        }

        return interpolateInteriorPoint(alongTrackMeters);
    }

    @NonNull
    private LatLon interpolateInteriorPoint(double alongTrackMeters) {
        for (int i = 1; i < pts.size(); i++) {
            double segmentStart = cumulative[i - 1];
            double segmentEnd = cumulative[i];
            if (segmentEnd < alongTrackMeters) {
                continue;
            }
            LatLon start = pts.get(i - 1);
            LatLon end = pts.get(i);
            double segmentLength = segmentEnd - segmentStart;
            if (segmentLength <= 0.0) {
                return end;
            }
            double t = (alongTrackMeters - segmentStart) / segmentLength;
            double lat = start.lat + (end.lat - start.lat) * t;
            double lon = start.lon + (end.lon - start.lon) * t;
            return new LatLon(lat, lon);
        }

        return pts.get(pts.size() - 1);
    }

    @Nullable
    public Match match(@NonNull LatLon p, int lastSegmentIndex) {
        if (pts.size() < 2) {
            return null;
        }

        int start = 0;
        int end = pts.size() - 2;
        if (lastSegmentIndex >= 0) {
            start = Math.max(0, lastSegmentIndex - 200);
            end = Math.min(pts.size() - 2, lastSegmentIndex + 200);
        }

        Match best = findBestMatchInRange(p, start, end);
        if (best == null && lastSegmentIndex >= 0) {
            best = findBestMatchInRange(p, 0, pts.size() - 2);
        }
        return best;
    }

    @Nullable
    private Match findBestMatchInRange(@NonNull LatLon p, int start, int end) {
        Match best = null;
        for (int i = start; i <= end; i++) {
            Match candidate = projectToSegment(p, pts.get(i), pts.get(i + 1), i);
            if (isCloserMatch(candidate, best)) {
                best = candidate;
            }
        }
        return best;
    }

    private static boolean isCloserMatch(@Nullable Match candidate, @Nullable Match best) {
        return candidate != null && (best == null || candidate.distanceToTrackMeters < best.distanceToTrackMeters);
    }

    @Nullable
    private Match projectToSegment(@NonNull LatLon p, @NonNull LatLon a, @NonNull LatLon b, int segIndex) {
        // Equirectangular projection around p.lat for local metric computations.
        double refLatRad = Math.toRadians(p.lat);
        double kx = 111320.0 * Math.cos(refLatRad);
        double ky = 111320.0;

        double ax = (a.lon - p.lon) * kx;
        double ay = (a.lat - p.lat) * ky;
        double bx = (b.lon - p.lon) * kx;
        double by = (b.lat - p.lat) * ky;

        double abx = bx - ax;
        double aby = by - ay;
        double ab2 = abx * abx + aby * aby;
        if (ab2 <= 0) {
            return null;
        }

        // p is at (0,0), so projection t is -dot(a,ab)/|ab|^2
        double t = -(ax * abx + ay * aby) / ab2;
        if (t < 0) t = 0;
        if (t > 1) t = 1;

        double projx = ax + t * abx;
        double projy = ay + t * aby;
        double dist = Math.sqrt(projx * projx + projy * projy);

        double segLen = Math.sqrt(ab2);
        double along = distanceAtPointIndex(segIndex) + t * segLen;
        double bearing = GeoMath.bearingDegrees(a.lat, a.lon, b.lat, b.lon);
        return new Match(dist, along, bearing, segIndex);
    }
}
