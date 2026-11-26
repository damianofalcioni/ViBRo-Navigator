package vibro.navigator.auto;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;

import vibro.navigator.R;
import vibro.navigator.android.language.AndroidAppLanguage;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.nav.service.NavigationServiceBinder;
import vibro.navigator.settings.AppAndroidAutoSettings;

final class ViBRoCarNavigationController {
    interface Host {
        @Nullable
        NavState currentState();

        void updateCurrentState(@Nullable NavState state);
    }

    static final String TAG = "ViBRoCarScreen";

    @NonNull
    private final CarContext carContext;
    @NonNull
    private final Host host;
    @NonNull
    private final ViBRoCarNavigationActions actions;
    private NavigationServiceBinder navBinder;
    private boolean bound;
    private boolean autoDrive;
    private boolean serviceConnectionPending;
    private boolean mapVisible;

    private final NavigationService.Listener navListener = new NavigationService.Listener() {
        @Override
        public void onState(@NonNull NavState state) {
            host.updateCurrentState(state);
            showSimulationNoticeIfPending();
        }

        @Override
        public void onNavigationStopped() {
            AppLogger.i(TAG, "Navigation stopped; clearing Android Auto surface");
            host.updateCurrentState(null);
            unbind();
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (!serviceConnectionPending) {
                return; // A late connection cannot resurrect a destroyed/stopped car client.
            }
            navBinder = (NavigationServiceBinder) service;
            bound = true;
            serviceConnectionPending = false;
            AppLogger.i(TAG, "NavigationService connected component=" + name);
            attachConnectedService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            AppLogger.w(TAG, "NavigationService disconnected component=" + name);
            navBinder = null;
            host.updateCurrentState(null);
            unbind();
        }
    };

    ViBRoCarNavigationController(@NonNull CarContext carContext, @NonNull Host host) {
        this.carContext = carContext;
        this.host = host;
        actions = new ViBRoCarNavigationActions(host, () -> navBinder);
    }

    boolean ensureIntegrationEnabled() {
        if (isIntegrationEnabled()) {
            return true;
        }
        if (bound || serviceConnectionPending || navBinder != null) {
            unbind();
        }
        host.updateCurrentState(null);
        return false;
    }

    void bind() {
        if (!isIntegrationEnabled()) {
            host.updateCurrentState(null);
            return;
        }
        if (bound || serviceConnectionPending) {
            return;
        }
        AppLogger.d(TAG, "Binding NavigationService from Android Auto");
        serviceConnectionPending = true;
        boolean serviceBound = carContext.bindService(new Intent(carContext, NavigationService.class), connection, 0);
        if (!serviceBound) {
            AppLogger.d(TAG, "No running NavigationService found for Android Auto");
            serviceConnectionPending = false;
            host.updateCurrentState(null);
            return;
        }
        host.updateCurrentState(host.currentState());
    }

    void unbind() {
        if (!bound && !serviceConnectionPending) {
            serviceConnectionPending = false;
            return;
        }
        AppLogger.i(TAG, "Unbinding NavigationService from Android Auto");
        try {
            if (navBinder != null) {
                navBinder.unregisterListener(navListener);
                navBinder.setCarNavigationClient(false, false);
            }
            carContext.unbindService(connection);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to unbind navigation service", e);
        } finally {
            bound = false;
            serviceConnectionPending = false;
            navBinder = null;
        }
    }

    void setCompassStreetViewport(@Nullable NavCompassState compassState) {
        actions.setCompassStreetViewport(compassState);
    }

    void setMapVisible(boolean visible) {
        if (mapVisible == visible) { return; }
        mapVisible = visible;
        if (navBinder != null) {
            navBinder.setCarNavigationClient(true, visible);
            showSimulationNoticeIfPending();
        }
    }

    void addBlockedWaypoint() {
        actions.addBlockedWaypoint();
    }

    void togglePaused() {
        actions.togglePaused();
    }

    void refreshLocationUpdateSettings() {
        actions.refreshLocationUpdateSettings();
    }

    void enableAutoDrive() {
        autoDrive = true;
        if (navBinder != null) { navBinder.enableSimulation(); }
    }

    void dispose() {
        if (autoDrive) { stopNavigation(); }
        unbind();
    }

    void stopNavigation() {
        actions.stopNavigation();
        if (navBinder == null) {
            carContext.stopService(new Intent(carContext, NavigationService.class));
        }
        host.updateCurrentState(null);
        unbind();
    }

    private void attachConnectedService() {
        if (!isIntegrationEnabled()) {
            AppLogger.i(TAG, "Android Auto integration disabled; clearing active car navigation");
            host.updateCurrentState(null);
            unbind();
            return;
        }
        if (!navBinder.isNavigationStarted()) {
            AppLogger.i(TAG, "NavigationService has no active navigation for Android Auto");
            host.updateCurrentState(null);
            unbind();
            return;
        }
        NavigationServiceBinder binder = navBinder;
        if (autoDrive) { binder.enableSimulation(); }
        binder.ensureForegroundNotification();
        binder.setCarNavigationClient(true, mapVisible);
        if (binder == navBinder) {
            binder.registerListener(navListener);
        }
    }

    private void showSimulationNoticeIfPending() {
        if (mapVisible && navBinder != null && navBinder.consumeCarSimulationStartNotice()) {
            String message = AndroidAppLanguage.getString(carContext, R.string.nav_simulation_started);
            CarToast.makeText(carContext, message, CarToast.LENGTH_LONG).show();
        }
    }

    private boolean isIntegrationEnabled() {
        return AppAndroidAutoSettings.isIntegrationEnabled(carContext);
    }
}
