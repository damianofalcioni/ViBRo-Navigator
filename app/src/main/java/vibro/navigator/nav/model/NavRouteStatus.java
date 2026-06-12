package vibro.navigator.nav.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.route.RouteSpeedLimit;

public final class NavRouteStatus {
    @NonNull
    public final NavGuidanceStatus guidance;
    @NonNull
    public final NavProgressStatus progress;
    @Nullable
    public final NavCompassState compassState;
    @Nullable
    public final RouteSpeedLimit speedLimit;
    public final boolean blockedRoadActionAvailable;

    public NavRouteStatus(
            @NonNull NavGuidanceStatus guidance,
            @NonNull NavProgressStatus progress,
            @Nullable NavCompassState compassState
    ) {
        this(guidance, progress, compassState, null);
    }

    public NavRouteStatus(
            @NonNull NavGuidanceStatus guidance,
            @NonNull NavProgressStatus progress,
            @Nullable NavCompassState compassState,
            @Nullable RouteSpeedLimit speedLimit
    ) {
        this(
                guidance,
                progress,
                compassState,
                speedLimit,
                compassState != null && !compassState.displayMode.straightLineMode
        );
    }

    public NavRouteStatus(
            @NonNull NavGuidanceStatus guidance,
            @NonNull NavProgressStatus progress,
            @Nullable NavCompassState compassState,
            @Nullable RouteSpeedLimit speedLimit,
            boolean blockedRoadActionAvailable
    ) {
        this.guidance = guidance;
        this.progress = progress;
        this.compassState = compassState;
        this.speedLimit = speedLimit;
        this.blockedRoadActionAvailable = blockedRoadActionAvailable;
    }

    @NonNull
    public String displayStatusBlock() {
        return progress.displayStatusBlock();
    }

    @NonNull
    public NavRouteStatus withProgress(@NonNull NavProgressStatus progress) {
        return new NavRouteStatus(
                guidance,
                progress,
                compassState,
                speedLimit,
                blockedRoadActionAvailable
        );
    }
}
