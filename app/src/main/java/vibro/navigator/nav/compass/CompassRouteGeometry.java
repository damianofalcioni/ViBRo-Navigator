package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;

import java.util.ArrayList;
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
    private final List<SamplePoint> fullRoutePoints;
    @NonNull
    private final CompassRouteSpatialIndex fullRouteSpatialIndex;
    @NonNull
    private final List<LatLon> hintSamplePoints;
    @NonNull
    private final List<LatLon> intermediateSamplePoints;
    @NonNull
    private final CompassPassedRouteSegments archivedPassedRouteSegments;
    @NonNull
    private final CompassPassedRouteSegments recalculationBridgeSegments;

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
        this(routeSamplePoints, hintSamplePoints, intermediateSamplePoints, Collections.emptyList());
    }

    public CompassRouteGeometry(
            @NonNull List<SamplePoint> routeSamplePoints,
            @NonNull List<LatLon> hintSamplePoints,
            @NonNull List<LatLon> intermediateSamplePoints,
            @NonNull List<List<LatLon>> archivedPassedRouteSegments
    ) {
        this(
                routeSamplePoints,
                hintSamplePoints,
                intermediateSamplePoints,
                archivedPassedRouteSegments,
                Collections.emptyList()
        );
    }

    public CompassRouteGeometry(
            @NonNull List<SamplePoint> routeSamplePoints,
            @NonNull List<LatLon> hintSamplePoints,
            @NonNull List<LatLon> intermediateSamplePoints,
            @NonNull List<List<LatLon>> archivedPassedRouteSegments,
            @NonNull List<List<LatLon>> recalculationBridgeSegments
    ) {
        this(
                routeSamplePoints,
                routeSamplePoints,
                hintSamplePoints,
                intermediateSamplePoints,
                archivedPassedRouteSegments,
                recalculationBridgeSegments
        );
    }

    public CompassRouteGeometry(
            @NonNull List<SamplePoint> routeSamplePoints,
            @NonNull List<SamplePoint> fullRoutePoints,
            @NonNull List<LatLon> hintSamplePoints,
            @NonNull List<LatLon> intermediateSamplePoints,
            @NonNull List<List<LatLon>> archivedPassedRouteSegments,
            @NonNull List<List<LatLon>> recalculationBridgeSegments
    ) {
        this.routeSamplePoints = immutableCopy(routeSamplePoints);
        this.fullRoutePoints = immutableCopy(fullRoutePoints);
        this.fullRouteSpatialIndex = new CompassRouteSpatialIndex(this.fullRoutePoints);
        this.hintSamplePoints = immutableCopy(hintSamplePoints);
        this.intermediateSamplePoints = immutableCopy(intermediateSamplePoints);
        this.archivedPassedRouteSegments = new CompassPassedRouteSegments(archivedPassedRouteSegments);
        this.recalculationBridgeSegments = new CompassPassedRouteSegments(recalculationBridgeSegments);
    }

    public int routeSamplePointCount() {
        return routeSamplePoints.size();
    }

    public int fullRoutePointCount() {
        return fullRoutePoints.size();
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
    public LatLon fullRoutePointAt(int index) {
        if (index < 0 || index >= fullRoutePoints.size()) {
            return null;
        }
        return fullRoutePoints.get(index).point;
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

    @NonNull
    public CompassPassedRouteSegments archivedPassedRouteSegments() {
        return archivedPassedRouteSegments;
    }

    @NonNull
    public CompassPassedRouteSegments recalculationBridgeSegments() {
        return recalculationBridgeSegments;
    }

    public int passedRoutePointCount(double alongTrackMeters) {
        return passedPointCount(routeSamplePoints, alongTrackMeters);
    }

    public int passedFullRoutePointCount(double alongTrackMeters) {
        return passedPointCount(fullRoutePoints, alongTrackMeters);
    }

    public double alongTrackMetersForSampleCount(int samplePointCount) {
        if (routeSamplePoints.isEmpty() || samplePointCount <= 0) {
            return 0.0;
        }
        int index = Math.min(samplePointCount, routeSamplePoints.size()) - 1;
        return routeSamplePoints.get(index).alongTrackMeters;
    }

    @NonNull
    CompassRouteSpatialIndex fullRouteSpatialIndex() {
        return fullRouteSpatialIndex;
    }

    @NonNull
    public List<LatLon> copyRouteSamplePointsUntil(int samplePointCount) {
        int safeCount = Math.max(0, Math.min(samplePointCount, routeSamplePoints.size()));
        if (safeCount == 0) {
            return Collections.emptyList();
        }
        List<LatLon> points = new ArrayList<>(safeCount);
        for (int i = 0; i < safeCount; i++) {
            points.add(routeSamplePoints.get(i).point);
        }
        return points;
    }

    @NonNull
    private static <T> List<T> immutableCopy(@NonNull List<T> values) {
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static int passedPointCount(
            @NonNull List<SamplePoint> points,
            double alongTrackMeters
    ) {
        if (points.isEmpty()) {
            return 0;
        }
        int passedPointCount = 0;
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).alongTrackMeters <= alongTrackMeters) {
                passedPointCount = i + 1;
            } else {
                break;
            }
        }
        return Math.max(1, passedPointCount);
    }
}
