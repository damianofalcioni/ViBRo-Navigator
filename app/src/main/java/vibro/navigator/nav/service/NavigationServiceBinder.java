package vibro.navigator.nav.service;


import vibro.navigator.nav.location.NavigationLocationFormatter;
import vibro.navigator.nav.location.NavigationLocation;
import android.os.Binder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;

public final class NavigationServiceBinder extends Binder {

    private static final String TAG = "NavigationService";

    public interface Host {
        void registerListener(@NonNull NavigationService.Listener listener);

        void unregisterListener(@NonNull NavigationService.Listener listener);

        int listenerCount();

        void emitState();

        void ensureForegroundNotification();

        void setNavigationUiVisible(boolean visible);

        boolean isNavigationPaused();

        @Nullable
        NavigationLocation getLastFilteredLocation();

        @NonNull
        List<?> addBlockedPointsAhead();

        @NonNull
        String getString(int resId);

        void requestBlockedRoadRouteRecalculation(@NonNull String inProgressNotice);

        void stopNavigationAndService();

        void pauseNavigation();

        void resumeNavigation();

        @Nullable
        String buildCurrentRouteGpx();
    }

    private final Host host;

    public NavigationServiceBinder(@NonNull Host host) {
        this.host = host;
    }

    public void registerListener(@NonNull NavigationService.Listener listener) {
        host.registerListener(listener);
        AppLogger.d(TAG, "Listener registered totalListeners=" + host.listenerCount());
        host.emitState();
    }

    public void ensureForegroundNotification() {
        host.ensureForegroundNotification();
    }

    public void setNavigationUiVisible(boolean visible) {
        host.setNavigationUiVisible(visible);
    }

    public void unregisterListener(@NonNull NavigationService.Listener listener) {
        host.unregisterListener(listener);
        AppLogger.d(TAG, "Listener unregistered totalListeners=" + host.listenerCount());
    }

    public void addBlockedWaypoint() {
        if (host.isNavigationPaused()) {
            AppLogger.w(TAG, "Blocked waypoint requested while navigation is paused");
            return;
        }
        NavigationLocation NavigationLocation = host.getLastFilteredLocation();
        if (NavigationLocation == null) {
            AppLogger.w(TAG, "Blocked waypoint requested without a current filtered NavigationLocation");
            return;
        }
        List<?> added = host.addBlockedPointsAhead();
        if (added.isEmpty()) {
            AppLogger.w(TAG, "Blocked-road reroute ignored because no route point ahead could be matched");
            return;
        }
        AppLogger.i(TAG, "Blocked-road points added added=" + formatNogoPoints(added)
                + " NavigationLocation=" + NavigationLocationFormatter.format(NavigationLocation));
        host.requestBlockedRoadRouteRecalculation(host.getString(R.string.nav_route_notice_blocked_road_recalculating));
    }

    public void stop() {
        AppLogger.i(TAG, "Stop requested through binder");
        host.stopNavigationAndService();
    }

    public void pause() {
        host.pauseNavigation();
    }

    public void resume() {
        host.resumeNavigation();
    }

    public boolean isPaused() {
        return host.isNavigationPaused();
    }

    @Nullable
    public String buildCurrentRouteGpx() {
        return host.buildCurrentRouteGpx();
    }

    @NonNull
    private static String formatNogoPoints(@NonNull List<?> values) {
        if (values.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(values.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
}
