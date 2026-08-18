package vibro.navigator.auto;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;

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
    private boolean serviceConnectionPending;
    private boolean refreshLocationSettingsOnReconnect;

    private final NavigationService.Listener navListener = new NavigationService.Listener() {
        @Override
        public void onState(@NonNull NavState state) {
            host.updateCurrentState(state);
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
            navBinder = (NavigationServiceBinder) service;
            bound = true;
            serviceConnectionPending = false;
            AppLogger.i(TAG, "NavigationService connected component=" + name);
            attachConnectedService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            AppLogger.w(TAG, "NavigationService disconnected component=" + name);
            bound = false;
            serviceConnectionPending = false;
            navBinder = null;
            host.updateCurrentState(null);
        }
    };

    ViBRoCarNavigationController(@NonNull CarContext carContext, @NonNull Host host) {
        this.carContext = carContext;
        this.host = host;
        actions = new ViBRoCarNavigationActions(carContext, host, () -> navBinder);
    }

    boolean ensureIntegrationEnabled() {
        if (isIntegrationEnabled()) {
            return true;
        }
        if (bound || serviceConnectionPending || navBinder != null) {
            unbind();
        }
        clearCurrentState();
        return false;
    }

    void bind() {
        if (!isIntegrationEnabled()) {
            clearCurrentState();
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
            clearCurrentState();
            return;
        }
        host.updateCurrentState(host.currentState());
    }

    void unbind() {
        if (!bound) {
            serviceConnectionPending = false;
            return;
        }
        AppLogger.i(TAG, "Unbinding NavigationService from Android Auto");
        try {
            if (navBinder != null) {
                navBinder.unregisterListener(navListener);
                navBinder.setCarNavigationUiVisible(false);
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

    @Nullable
    NavigationServiceBinder currentBinder() {
        return navBinder;
    }

    void addBlockedWaypoint() {
        actions.addBlockedWaypoint();
    }

    void togglePaused() {
        actions.togglePaused();
    }

    void stopNavigation() {
        actions.stopNavigation();
        unbind();
    }

    void requestLocationSettingsRefreshOnReconnect() {
        refreshLocationSettingsOnReconnect = true;
    }

    void openPhoneNavigationIfActive() {
        actions.openPhoneNavigationIfActive();
    }

    @NonNull
    String buildCurrentDirectionDetailsText() {
        return ViBRoAutoDirectionDetailsText.build(carContext, navBinder);
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
        navBinder.ensureForegroundNotification();
        navBinder.setCarNavigationUiVisible(true);
        navBinder.registerListener(navListener);
        refreshLocationSettingsIfRequested();
    }

    private void refreshLocationSettingsIfRequested() {
        if (!refreshLocationSettingsOnReconnect) {
            return;
        }
        refreshLocationSettingsOnReconnect = false;
        navBinder.refreshLocationUpdateSettings();
    }

    private void clearCurrentState() {
        host.updateCurrentState(null);
    }

    private boolean isIntegrationEnabled() {
        return AppAndroidAutoSettings.isIntegrationEnabled(carContext);
    }
}
