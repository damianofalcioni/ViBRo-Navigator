package vibro.navigator.nav.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.compass.NavCompassState;

public final class NavRouteStatus {
    @NonNull
    public final NavGuidanceStatus guidance;
    @NonNull
    public final NavProgressStatus progress;
    @Nullable
    public final NavCompassState compassState;

    public NavRouteStatus(
            @NonNull NavGuidanceStatus guidance,
            @NonNull NavProgressStatus progress,
            @Nullable NavCompassState compassState
    ) {
        this.guidance = guidance;
        this.progress = progress;
        this.compassState = compassState;
    }

    @NonNull
    public String displayStatusBlock() {
        return progress.displayStatusBlock();
    }

    @NonNull
    public NavRouteStatus withProgress(@NonNull NavProgressStatus progress) {
        return new NavRouteStatus(guidance, progress, compassState);
    }
}
