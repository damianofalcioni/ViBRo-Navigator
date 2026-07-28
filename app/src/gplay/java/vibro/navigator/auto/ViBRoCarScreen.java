package vibro.navigator.auto;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.AppManager;
import androidx.car.app.CarToast;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Template;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.nav.service.NavigationServiceBinder;

// Android Auto requires templates, so the active screen renders the phone landscape UI onto the car map surface.
public final class ViBRoCarScreen extends Screen {

    private static final String TAG = "ViBRoCarScreen";
    private static final long SURFACE_COUNTDOWN_TICK_MS = 1_000L;

    private final CarContext carContext;
    private final ViBRoCarTemplates templates;
    private final ViBRoAutoCustomButtonController customButtonController;
    private TaskScheduler uiScheduler;
    private ViBRoAutoSurfaceRenderer surfaceRenderer;
    private final Runnable surfaceCountdownTicker = new Runnable() {
        @Override
        public void run() {
            surfaceRenderer.render();
            uiScheduler.postDelayed(this, SURFACE_COUNTDOWN_TICK_MS);
        }
    };

    private NavigationServiceBinder navBinder;
    private boolean bound;
    private boolean refreshLocationSettingsOnReconnect;
    @Nullable
    private NavState currentState;

    private final NavigationService.Listener navListener = state -> {
        currentState = state;
        surfaceRenderer.setState(state);
        invalidate();
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            navBinder = (NavigationServiceBinder) service;
            bound = true;
            AppLogger.i(TAG, "NavigationService connected component=" + name);
            navBinder.ensureForegroundNotification();
            navBinder.registerListener(navListener);
            if (refreshLocationSettingsOnReconnect) {
                refreshLocationSettingsOnReconnect = false;
                navBinder.refreshLocationUpdateSettings();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            AppLogger.w(TAG, "NavigationService disconnected component=" + name);
            bound = false;
            navBinder = null;
            currentState = null;
            surfaceRenderer.setState(null);
            invalidate();
        }
    };

    public ViBRoCarScreen(@NonNull CarContext carContext) {
        this(carContext, AndroidTaskScheduler.main());
    }

    ViBRoCarScreen(@NonNull CarContext carContext, @NonNull TaskScheduler uiScheduler) {
        super(carContext);
        this.carContext = carContext;
        this.uiScheduler = uiScheduler;
        ViBRoAutoSurfaceControls controls = new ViBRoAutoSurfaceControls();
        templates = new ViBRoCarTemplates(carContext, controls);
        surfaceRenderer = new ViBRoAutoSurfaceRenderer(carContext, controls, uiScheduler);
        customButtonController = new ViBRoAutoCustomButtonController(carContext, new AutoCustomButtonHost());
        getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                carContext.getCarService(AppManager.class).setSurfaceCallback(surfaceRenderer);
                bindNavigationService();
                uiScheduler.post(surfaceCountdownTicker);
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                uiScheduler.removeCallbacks(surfaceCountdownTicker);
                surfaceRenderer.clearSurface();
                unbindNavigationService();
            }

            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                surfaceRenderer.dispose();
            }
        });
    }

    public void handleIntent(@NonNull Intent intent) {
        AppLogger.i(TAG, "Android Auto intent action=" + safe(intent.getAction())
                + " data=" + safe(intent.getDataString()));
        invalidate();
    }

    @Override
    @NonNull
    public Template onGetTemplate() {
        NavState state = currentState;
        if (state != null) {
            surfaceRenderer.setState(state);
        }
        return templates.build(state);
    }

    private void bindNavigationService() {
        if (bound) {
            return;
        }
        AppLogger.i(TAG, "Binding NavigationService from Android Auto");
        carContext.bindService(
                new Intent(carContext, NavigationService.class),
                connection,
                Context.BIND_AUTO_CREATE
        );
    }

    private void unbindNavigationService() {
        if (!bound) {
            return;
        }
        AppLogger.i(TAG, "Unbinding NavigationService from Android Auto");
        try {
            if (navBinder != null) {
                navBinder.unregisterListener(navListener);
            }
            carContext.unbindService(connection);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to unbind navigation service", e);
        } finally {
            bound = false;
            navBinder = null;
        }
    }

    private void addBlockedWaypoint() {
        if (navBinder == null) {
            AppLogger.w(TAG, "Blocked-road requested before service binding completed");
            return;
        }
        if (!navBinder.canAddBlockedWaypoint()) {
            AppLogger.w(TAG, "Blocked-road requested while blocked-road rerouting is unavailable");
            return;
        }
        navBinder.addBlockedWaypoint();
    }

    private void togglePaused() {
        if (navBinder == null) {
            AppLogger.w(TAG, "Pause/resume requested before service binding completed");
            return;
        }
        if (navBinder.isPaused()) {
            navBinder.resume();
        } else {
            navBinder.pause();
        }
    }

    private void stopNavigation() {
        if (navBinder == null) {
            AppLogger.w(TAG, "Stop requested before service binding completed");
            return;
        }
        navBinder.stop();
    }

    private void exportCurrentRoute() {
        ViBRoAutoRouteExporter.exportCurrentRoute(carContext, navBinder, this::showToast);
    }

    private void openPhoneSettings() {
        refreshLocationSettingsOnReconnect = true;
        ViBRoAutoPhoneLauncher.openSettings(carContext);
    }

    private void showToast(int messageResId) {
        CarToast.makeText(carContext, messageResId, CarToast.LENGTH_SHORT).show();
    }

    private void openPhoneApp() {
        ViBRoAutoPhoneLauncher.openMain(carContext);
    }

    private final class ViBRoAutoSurfaceControls
            implements ViBRoAutoSurfaceRenderer.Controls, ViBRoCarTemplates.Actions {
        @Override
        public void onOpenPhoneApp() {
            openPhoneApp();
        }

        @Override
        public void onBlockedRoad() {
            addBlockedWaypoint();
        }

        @Override
        public void onStopNavigation() {
            stopNavigation();
        }

        @Override
        public void onTogglePaused() {
            togglePaused();
        }

        @Override
        public void onExportRoute() {
            exportCurrentRoute();
        }

        @Override
        public void onOpenSettings() {
            openPhoneSettings();
        }

        @Override
        public void onToggleCustomButton() {
            customButtonController.toggleSelectedSetting();
        }

        @Override
        @NonNull
        public String buildCurrentDirectionDetailsText() {
            return ViBRoAutoDirectionDetailsText.build(carContext, navBinder);
        }
    }

    private final class AutoCustomButtonHost implements ViBRoAutoCustomButtonController.Host {
        @Nullable
        @Override
        public NavigationServiceBinder currentBinder() {
            return navBinder;
        }

        @Override
        public void openPhoneSettings() {
            ViBRoCarScreen.this.openPhoneSettings();
        }

        @Override
        public void refreshSurfaceTheme() {
            surfaceRenderer.refreshTheme();
            invalidate();
        }

        @Override
        public void refreshSurface() {
            surfaceRenderer.render();
            invalidate();
        }

        @Override
        public void showToast(int messageResId) {
            ViBRoCarScreen.this.showToast(messageResId);
        }
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value == null ? "null" : value;
    }
}
