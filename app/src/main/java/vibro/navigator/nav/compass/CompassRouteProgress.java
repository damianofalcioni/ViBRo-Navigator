package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;

import java.util.List;

final class CompassRouteProgress {
    private CompassRouteProgress() {
    }

    static int passedPointCount(
            @NonNull List<CompassRouteGeometry.SamplePoint> points,
            double alongTrackMeters
    ) {
        if (points.isEmpty()) {
            return 0;
        }
        int passedPointCount = 0;
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).alongTrackMeters > alongTrackMeters) {
                break;
            }
            passedPointCount = i + 1;
        }
        return Math.max(1, passedPointCount);
    }
}
