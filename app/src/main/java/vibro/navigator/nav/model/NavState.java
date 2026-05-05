package vibro.navigator.nav.model;

import vibro.navigator.nav.compass.NavCompassState;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NavState {
    public static final long NO_DEADLINE = -1L;

    @NonNull
    public final NavRouteStatus routeStatus;
    @NonNull
    public final NavGpsStatus gpsStatus;
    @NonNull
    public final NavPauseStatus pauseStatus;

    public NavState(
            @NonNull NavRouteStatus routeStatus,
            @NonNull NavGpsStatus gpsStatus,
            @NonNull NavPauseStatus pauseStatus
    ) {
        this.routeStatus = routeStatus;
        this.gpsStatus = gpsStatus;
        this.pauseStatus = pauseStatus;
    }

    @NonNull
    public String displayStatusBlock() {
        return routeStatus.displayStatusBlock();
    }

}
