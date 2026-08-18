package vibro.navigator.nav.ui;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.NonNull;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.nav.service.NavigationServiceBinder;

final class NavigationActivityServiceConnection implements ServiceConnection {
    interface Host {
        void onBinderConnected(@NonNull NavigationServiceBinder binder);

        void onBinderDisconnected();

        boolean finishIfBoundServiceHasStopped();

        boolean consumeLocationSettingsRefreshRequest();
    }

    private static final String TAG = "NavigationActivity";

    @NonNull
    private final NavigationService.Listener navListener;
    @NonNull
    private final Host host;

    NavigationActivityServiceConnection(
            @NonNull NavigationService.Listener navListener,
            @NonNull Host host
    ) {
        this.navListener = navListener;
        this.host = host;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        NavigationServiceBinder binder = (NavigationServiceBinder) service;
        host.onBinderConnected(binder);
        AppLogger.i(TAG, "NavigationService connected component=" + name);
        if (host.finishIfBoundServiceHasStopped()) {
            return;
        }
        binder.ensureForegroundNotification();
        binder.setNavigationUiVisible(true);
        binder.registerListener(navListener);
        if (host.consumeLocationSettingsRefreshRequest()) {
            binder.refreshLocationUpdateSettings();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        AppLogger.w(TAG, "NavigationService disconnected component=" + name);
        host.onBinderDisconnected();
    }

    void detach(@NonNull NavigationServiceBinder binder) {
        binder.setCompassStreetViewport(null);
        binder.unregisterListener(navListener);
        binder.setNavigationUiVisible(false);
    }
}
