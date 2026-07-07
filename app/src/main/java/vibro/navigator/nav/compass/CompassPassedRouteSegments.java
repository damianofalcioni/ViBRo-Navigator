package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;

public final class CompassPassedRouteSegments {
    @NonNull
    public static final CompassPassedRouteSegments EMPTY =
            new CompassPassedRouteSegments(Collections.emptyList());

    @NonNull
    private final List<List<LatLon>> segments;

    public CompassPassedRouteSegments(@NonNull List<List<LatLon>> segments) {
        this.segments = immutableNestedCopy(segments);
    }

    public int segmentCount() {
        return segments.size();
    }

    public int samplePointCount(int segmentIndex) {
        if (segmentIndex < 0 || segmentIndex >= segments.size()) {
            return 0;
        }
        return segments.get(segmentIndex).size();
    }

    public int totalSamplePointCount() {
        int count = 0;
        for (List<LatLon> segment : segments) {
            count += segment.size();
        }
        return count;
    }

    @Nullable
    public LatLon samplePointAt(int segmentIndex, int pointIndex) {
        if (segmentIndex < 0 || segmentIndex >= segments.size()) {
            return null;
        }
        List<LatLon> segment = segments.get(segmentIndex);
        if (pointIndex < 0 || pointIndex >= segment.size()) {
            return null;
        }
        return segment.get(pointIndex);
    }

    @NonNull
    private static List<List<LatLon>> immutableNestedCopy(@NonNull List<List<LatLon>> values) {
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<LatLon>> copy = new ArrayList<>(values.size());
        for (List<LatLon> segment : values) {
            if (segment.isEmpty()) {
                continue;
            }
            copy.add(Collections.unmodifiableList(new ArrayList<>(segment)));
        }
        return copy.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(copy);
    }
}
