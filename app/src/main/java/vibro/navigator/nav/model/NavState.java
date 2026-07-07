package vibro.navigator.nav.model;

import androidx.annotation.NonNull;

public final class NavState {
    public static final long NO_DEADLINE = -1L;

    @NonNull
    public final NavRouteStatus routeStatus;
    @NonNull
    public final NavGpsStatus gpsStatus;
    @NonNull
    public final NavPauseStatus pauseStatus;
    @NonNull
    public final NavTripStatus tripStatus;

    public NavState(
            @NonNull NavRouteStatus routeStatus,
            @NonNull NavGpsStatus gpsStatus,
            @NonNull NavPauseStatus pauseStatus
    ) {
        this(routeStatus, gpsStatus, pauseStatus, NavTripStatus.unavailable());
    }

    public NavState(
            @NonNull NavRouteStatus routeStatus,
            @NonNull NavGpsStatus gpsStatus,
            @NonNull NavPauseStatus pauseStatus,
            @NonNull NavTripStatus tripStatus
    ) {
        this.routeStatus = routeStatus;
        this.gpsStatus = gpsStatus;
        this.pauseStatus = pauseStatus;
        this.tripStatus = tripStatus;
    }

    @NonNull
    public String displayStatusBlock() {
        return routeStatus.displayStatusBlock();
    }

}
