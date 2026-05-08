package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;

import java.util.Collections;
import java.util.List;

public final class CompassRouteGeometry {

    public static final class SamplePoint {
        @NonNull
        final LatLon point;
        final double alongTrackMeters;

        public SamplePoint(@NonNull LatLon point, double alongTrackMeters) {
            this.point = point;
            this.alongTrackMeters = alongTrackMeters;
        }
    }

    @NonNull
    private final List<SamplePoint> routeSamplePoints;
    @NonNull
    private final List<LatLon> hintSamplePoints;
    @NonNull
    private final List<LatLon> intermediateSamplePoints;

    public CompassRouteGeometry(
            @NonNull List<SamplePoint> routeSamplePoints,
            @NonNull List<LatLon> hintSamplePoints
    ) {
        this(routeSamplePoints, hintSamplePoints, Collections.emptyList());
    }

    public CompassRouteGeometry(
            @NonNull List<SamplePoint> routeSamplePoints,
            @NonNull List<LatLon> hintSamplePoints,
            @NonNull List<LatLon> intermediateSamplePoints
    ) {
        this.routeSamplePoints = Collections.unmodifiableList(routeSamplePoints);
        this.hintSamplePoints = Collections.unmodifiableList(hintSamplePoints);
        this.intermediateSamplePoints = Collections.unmodifiableList(intermediateSamplePoints);
    }

    public int routeSamplePointCount() {
        return routeSamplePoints.size();
    }

    public int hintSamplePointCount() {
        return hintSamplePoints.size();
    }

    public int intermediateSamplePointCount() {
        return intermediateSamplePoints.size();
    }

    @Nullable
    public LatLon routeSamplePointAt(int index) {
        if (index < 0 || index >= routeSamplePoints.size()) {
            return null;
        }
        return routeSamplePoints.get(index).point;
    }

    @Nullable
    public LatLon hintSamplePointAt(int index) {
        if (index < 0 || index >= hintSamplePoints.size()) {
            return null;
        }
        return hintSamplePoints.get(index);
    }

    @Nullable
    public LatLon intermediateSamplePointAt(int index) {
        if (index < 0 || index >= intermediateSamplePoints.size()) {
            return null;
        }
        return intermediateSamplePoints.get(index);
    }

    public int passedRoutePointCount(double alongTrackMeters) {
        if (routeSamplePoints.isEmpty()) {
            return 0;
        }
        int passedPointCount = 0;
        for (int i = 0; i < routeSamplePoints.size(); i++) {
            if (routeSamplePoints.get(i).alongTrackMeters <= alongTrackMeters) {
                passedPointCount = i + 1;
            } else {
                break;
            }
        }
        return Math.max(1, passedPointCount);
    }
}
