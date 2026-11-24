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

        public Match(double distanceToTrackMeters, double alongTrackMeters, double segmentBearingDegrees, int segmentIndex) {
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
        return cumulative[cumulative.length - 1];
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
            double lonSpan = GeoMath.normalizeLongitude(end.lon - start.lon);
            double lon = GeoMath.normalizeLongitude(start.lon + lonSpan * t);
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
    public Match matchFromSegmentIndex(@NonNull LatLon point, int firstSegmentIndex) {
        int start = Math.max(0, Math.min(firstSegmentIndex, pts.size() - 2));
        return findBestMatchInRange(point, start, pts.size() - 2);
    }

    @NonNull
    public List<Double> matchStopDistancesInOrder(@NonNull List<LatLon> stops) {
        List<Double> distances = new ArrayList<>(stops.size());
        double minimumAlongTrackMeters = 0;
        for (LatLon stop : stops) {
            Match match = matchAtOrAfterDistance(stop, minimumAlongTrackMeters);
            if (match == null) {
                break;
            }
            minimumAlongTrackMeters = match.alongTrackMeters;
            distances.add(minimumAlongTrackMeters);
        }
        return distances;
    }

    @Nullable
    private Match matchAtOrAfterDistance(@NonNull LatLon point, double minimumAlongTrackMeters) {
        Match best = null;
        for (int i = 0; i < pts.size() - 1; i++) {
            double end = distanceAtPointIndex(i + 1);
            if (end < minimumAlongTrackMeters) {
                continue;
            }
            double length = end - distanceAtPointIndex(i);
            if (length <= 0.0) {
                continue;
            }
            double minimumFraction = Math.max(0.0, 1.0 - (end - minimumAlongTrackMeters) / length);
            Match candidate = projectToSegment(point, pts.get(i), pts.get(i + 1), i, minimumFraction);
            if (isCloserMatch(candidate, best)) {
                best = candidate;
            }
        }
        return best;
    }

    @Nullable
    Match matchBeforeDistance(@NonNull LatLon p, double maxAlongTrackMeters) {
        if (pts.size() < 2) {
            return null;
        }

        double safeMaxAlongTrackMeters = Math.max(0.0, maxAlongTrackMeters);
        int end = 0;
        for (int i = 0; i <= pts.size() - 2; i++) {
            if (distanceAtPointIndex(i) > safeMaxAlongTrackMeters) {
                break;
            }
            end = i;
        }
        return findBestMatchInRange(p, 0, end);
    }

    @Nullable
    private Match findBestMatchInRange(@NonNull LatLon p, int start, int end) {
        Match best = null;
        for (int i = start; i <= end; i++) {
            Match candidate = projectToSegment(p, pts.get(i), pts.get(i + 1), i, 0.0);
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
    private Match projectToSegment(@NonNull LatLon p, @NonNull LatLon a, @NonNull LatLon b, int segIndex,
            double minimumFraction) {
        // Equirectangular projection around p.lat for local metric computations.
        double refLatRad = Math.toRadians(p.lat);
        double kx = 111320.0 * Math.cos(refLatRad);
        double ky = 111320.0;

        // Unwrap both endpoints around the same midpoint so date-line segments remain short.
        double lonSpan = GeoMath.normalizeLongitude(b.lon - a.lon);
        double lonMidpoint = a.lon + lonSpan / 2;
        double ax = (GeoMath.normalizeLongitude(lonMidpoint - p.lon) - lonSpan / 2) * kx;
        double ay = (a.lat - p.lat) * ky;
        double abx = lonSpan * kx;
        double aby = (b.lat - a.lat) * ky;
        double ab2 = abx * abx + aby * aby;
        if (ab2 <= 0) {
            return null;
        }

        // p is at (0,0), so projection t is -dot(a,ab)/|ab|^2
        double t = Math.max(minimumFraction, Math.min(1.0, -(ax * abx + ay * aby) / ab2));

        double projx = ax + t * abx;
        double projy = ay + t * aby;
        double dist = Math.sqrt(projx * projx + projy * projy);

        double segmentStartMeters = distanceAtPointIndex(segIndex);
        double segmentLengthMeters = distanceAtPointIndex(segIndex + 1) - segmentStartMeters;
        double along = segmentStartMeters + t * segmentLengthMeters;
        double bearing = GeoMath.bearingDegrees(a.lat, a.lon, b.lat, b.lon);
        return new Match(dist, along, bearing, segIndex);
    }
}
