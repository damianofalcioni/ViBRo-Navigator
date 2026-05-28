package vibro.navigator.auto;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ActivityNotFoundException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.AppManager;
import androidx.car.app.CarToast;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Template;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.io.IOException;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.main.MainActivity;
import vibro.navigator.nav.export.NavigationRouteGpxViewIntent;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.nav.service.NavigationServiceBinder;

// Android Auto requires templates, so the active screen renders the phone landscape UI onto the car map surface.
public final class ViBRoCarScreen extends Screen {

    private static final String TAG = "ViBRoCarScreen";
    private static final long SURFACE_COUNTDOWN_TICK_MS = 1_000L;

    private final CarContext carContext;
    private final ViBRoCarTemplates templates;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private ViBRoAutoSurfaceRenderer surfaceRenderer;
    private final Runnable surfaceCountdownTicker = new Runnable() {
        @Override
        public void run() {
            surfaceRenderer.render();
            uiHandler.postDelayed(this, SURFACE_COUNTDOWN_TICK_MS);
        }
    };

    private NavigationServiceBinder navBinder;
    private boolean bound;
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
        super(carContext);
        this.carContext = carContext;
        ViBRoAutoSurfaceControls controls = new ViBRoAutoSurfaceControls();
        templates = new ViBRoCarTemplates(carContext, controls);
        surfaceRenderer = new ViBRoAutoSurfaceRenderer(carContext, controls);
        getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                carContext.getCarService(AppManager.class).setSurfaceCallback(surfaceRenderer);
                bindNavigationService();
                uiHandler.post(surfaceCountdownTicker);
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                uiHandler.removeCallbacks(surfaceCountdownTicker);
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
        if (navBinder == null) {
            AppLogger.w(TAG, "Route export requested before service binding completed");
            showToast(R.string.msg_route_export_unavailable);
            return;
        }
        String gpx = navBinder.buildCurrentRouteGpx();
        if (gpx == null) {
            AppLogger.w(TAG, "Route export requested without an active route");
            showToast(R.string.msg_route_export_unavailable);
            return;
        }
        AppLogger.dMultiline(TAG, "Generated route GPX XML from Android Auto", gpx);
        try {
            Intent chooser = NavigationRouteGpxViewIntent.createChooser(carContext, gpx)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            carContext.startActivity(chooser);
            AppLogger.i(TAG, "Route GPX chooser launched from Android Auto");
        } catch (ActivityNotFoundException e) {
            AppLogger.w(TAG, "No app can open exported GPX route from Android Auto", e);
            showToast(R.string.msg_route_export_no_app);
        } catch (IOException | RuntimeException e) {
            AppLogger.w(TAG, "Failed to export current route as GPX from Android Auto", e);
            showToast(R.string.msg_route_export_failed);
        }
    }

    private void showToast(int messageResId) {
        CarToast.makeText(carContext, messageResId, CarToast.LENGTH_SHORT).show();
    }

    private void openPhoneApp() {
        Intent intent = new Intent(carContext, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        carContext.startActivity(intent);
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
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value == null ? "null" : value;
    }
}
