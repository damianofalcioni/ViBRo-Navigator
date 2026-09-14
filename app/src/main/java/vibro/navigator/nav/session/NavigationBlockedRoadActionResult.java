package vibro.navigator.nav.session;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.brouter.NogoPoint;

public final class NavigationBlockedRoadActionResult {
    private enum Type {
        NONE,
        TARGET_SKIPPED,
        BLOCKED_POINTS_ADDED
    }

    @NonNull
    private static final NavigationBlockedRoadActionResult NONE =
            new NavigationBlockedRoadActionResult(Type.NONE, Collections.emptyList());
    @NonNull
    private static final NavigationBlockedRoadActionResult TARGET_SKIPPED =
            new NavigationBlockedRoadActionResult(Type.TARGET_SKIPPED, Collections.emptyList());

    @NonNull
    private final Type type;
    @NonNull
    public final List<NogoPoint> addedBlockedPoints;

    private NavigationBlockedRoadActionResult(
            @NonNull Type type,
            @NonNull List<NogoPoint> addedBlockedPoints
    ) {
        this.type = type;
        this.addedBlockedPoints = Collections.unmodifiableList(new ArrayList<>(addedBlockedPoints));
    }

    @NonNull
    static NavigationBlockedRoadActionResult none() {
        return NONE;
    }

    @NonNull
    static NavigationBlockedRoadActionResult targetSkipped() {
        return TARGET_SKIPPED;
    }

    @NonNull
    static NavigationBlockedRoadActionResult blockedPointsAdded(@NonNull List<NogoPoint> points) {
        return points.isEmpty()
                ? NONE
                : new NavigationBlockedRoadActionResult(Type.BLOCKED_POINTS_ADDED, points);
    }

    public boolean isTargetSkipped() {
        return type == Type.TARGET_SKIPPED;
    }

    public boolean hasBlockedPoints() {
        return type == Type.BLOCKED_POINTS_ADDED;
    }
}
