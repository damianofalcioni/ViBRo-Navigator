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
        final int trackIndex;

        public SamplePoint(@NonNull LatLon point, double alongTrackMeters) {
            this(point, alongTrackMeters, -1);
        }

        SamplePoint(@NonNull LatLon point, double alongTrackMeters, int trackIndex) {
            this.point = point;
            this.alongTrackMeters = alongTrackMeters;
            this.trackIndex = trackIndex;
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
    @NonNull
    private final CompassRouteBeelineSegments beelineSegments;
    @NonNull
    private final boolean[] beelineTrackSegments;

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
                recalculationBridgeSegments,
                new boolean[0]
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
        this(
                routeSamplePoints,
                fullRoutePoints,
                hintSamplePoints,
                intermediateSamplePoints,
                archivedPassedRouteSegments,
                recalculationBridgeSegments,
                new boolean[0]
        );
    }

    CompassRouteGeometry(
            @NonNull List<SamplePoint> routeSamplePoints,
            @NonNull List<SamplePoint> fullRoutePoints,
            @NonNull List<LatLon> hintSamplePoints,
            @NonNull List<LatLon> intermediateSamplePoints,
            @NonNull List<List<LatLon>> archivedPassedRouteSegments,
            @NonNull List<List<LatLon>> recalculationBridgeSegments,
            @NonNull boolean[] beelineTrackSegments
    ) {
        this.routeSamplePoints = immutableCopy(routeSamplePoints);
        this.fullRoutePoints = immutableCopy(fullRoutePoints);
        this.fullRouteSpatialIndex = new CompassRouteSpatialIndex(this.fullRoutePoints);
        this.hintSamplePoints = immutableCopy(hintSamplePoints);
        this.intermediateSamplePoints = immutableCopy(intermediateSamplePoints);
        this.archivedPassedRouteSegments = new CompassPassedRouteSegments(archivedPassedRouteSegments);
        this.recalculationBridgeSegments = new CompassPassedRouteSegments(recalculationBridgeSegments);
        this.beelineTrackSegments = beelineTrackSegments.clone();
        this.beelineSegments = new CompassRouteBeelineSegments(
                this.routeSamplePoints,
                this.fullRoutePoints,
                this.beelineTrackSegments
        );
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

    @NonNull
    public CompassRouteGeometry withStoredRouteSegments(
            @NonNull List<List<LatLon>> archivedPassedRouteSegments,
            @NonNull List<List<LatLon>> recalculationBridgeSegments
    ) {
        return new CompassRouteGeometry(
                routeSamplePoints,
                fullRoutePoints,
                hintSamplePoints,
                intermediateSamplePoints,
                archivedPassedRouteSegments,
                recalculationBridgeSegments,
                beelineTrackSegments
        );
    }

    @NonNull
    public CompassRouteBeelineSegments beelineSegments() {
        return beelineSegments;
    }

    public int passedRoutePointCount(double alongTrackMeters) {
        return CompassRouteProgress.passedPointCount(routeSamplePoints, alongTrackMeters);
    }

    public int passedFullRoutePointCount(double alongTrackMeters) {
        return CompassRouteProgress.passedPointCount(fullRoutePoints, alongTrackMeters);
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

}
