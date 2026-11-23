package vibro.navigator.auto;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Template;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import vibro.navigator.R;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.model.NavState;

// Host templates own guidance and actions; only map/route content belongs on the custom surface.
public final class ViBRoCarScreen extends Screen {

    private static final String TAG = "ViBRoCarScreen";
    private static final long NAVIGATION_SERVICE_ATTACH_RETRY_MS = 1_000L;

    private final CarContext carContext;
    private final ViBRoCarTemplates templates;
    private final ViBRoCarNavigationController navigationController;
    private final ViBRoCarHostNavigation hostNavigation;
    private final ViBRoCarIncomingNavigation incomingNavigation;
    private final TaskScheduler uiScheduler;
    private final ViBRoAutoSurfaceRenderer surfaceRenderer;
    private final Runnable serviceAttachTicker = new Runnable() {
        @Override
        public void run() {
            if (navigationController.ensureIntegrationEnabled()) {
                navigationController.bind();
            } else {
                incomingNavigation.cancel();
            }
            surfaceRenderer.render();
            uiScheduler.postDelayed(this, NAVIGATION_SERVICE_ATTACH_RETRY_MS);
        }
    };

    @Nullable
    private NavState currentState;

    public ViBRoCarScreen(@NonNull CarContext carContext) {
        this(carContext, AndroidTaskScheduler.main());
    }

    ViBRoCarScreen(@NonNull CarContext carContext, @NonNull TaskScheduler uiScheduler) {
        super(carContext);
        this.carContext = carContext;
        this.uiScheduler = uiScheduler;
        applyCarTheme();
        navigationController = new ViBRoCarNavigationController(carContext, new CarNavigationHost());
        incomingNavigation = new ViBRoCarIncomingNavigation(carContext, uiScheduler, this::invalidate);
        hostNavigation = new ViBRoCarHostNavigation(carContext, () -> {
            incomingNavigation.cancel();
            navigationController.stopNavigation();
        });
        hostNavigation.setAutoDriveAction(navigationController::enableAutoDrive);
        ViBRoCarControls controls = new ViBRoCarControls();
        templates = new ViBRoCarTemplates(carContext, controls);
        surfaceRenderer = new ViBRoAutoSurfaceRenderer(
                carContext,
                uiScheduler,
                navigationController::setCompassStreetViewport
        );

        getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                refreshCarConfiguration();
                carContext.getCarService(AppManager.class).setSurfaceCallback(surfaceRenderer);
                navigationController.setMapVisible(true);
                if (navigationController.ensureIntegrationEnabled()) {
                    navigationController.bind();
                }
                uiScheduler.removeCallbacks(serviceAttachTicker);
                uiScheduler.post(serviceAttachTicker);
                incomingNavigation.retryPending();
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                navigationController.setMapVisible(false);
                surfaceRenderer.clearSurface();
            }

            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                uiScheduler.removeCallbacks(serviceAttachTicker);
                incomingNavigation.dispose();
                hostNavigation.dispose();
                navigationController.dispose();
                surfaceRenderer.dispose();
            }
        });
    }

    public void handleIntent(@NonNull Intent intent) {
        AppLogger.i(TAG, "Android Auto intent action=" + safe(intent.getAction())
                + " data=" + safe(intent.getDataString()));
        incomingNavigation.handle(intent);
        invalidate();
    }

    @Override
    @NonNull
    public Template onGetTemplate() {
        if (!navigationController.ensureIntegrationEnabled()) {
            incomingNavigation.cancel();
            return templates.build(null);
        }
        Template incoming = incomingNavigation.template();
        if (incoming != null) {
            return incoming;
        }
        NavState state = currentState;
        if (state != null) {
            surfaceRenderer.setState(state);
        }
        return templates.build(state);
    }

    private void applyCarTheme() {
        carContext.setTheme(!carContext.isDarkMode()
                ? R.style.Theme_ViBRoNavigator_Light
                : R.style.Theme_ViBRoNavigator);
    }

    void refreshCarConfiguration() {
        applyCarTheme();
        surfaceRenderer.refreshTheme();
        invalidate();
    }

    private void updateCurrentState(@Nullable NavState state) {
        boolean templateChanged = templates.requiresInvalidation(currentState, state);
        currentState = state;
        hostNavigation.update(state);
        surfaceRenderer.setState(currentState);
        if (templateChanged) {
            invalidate();
        }
    }

    private final class ViBRoCarControls
            implements ViBRoCarTemplates.Actions {
        @Override
        public void onBlockedRoad() {
            navigationController.addBlockedWaypoint();
        }

        @Override
        public void onStopNavigation() {
            incomingNavigation.cancel();
            navigationController.stopNavigation();
        }

        @Override
        public void onTogglePaused() {
            navigationController.togglePaused();
        }

    }

    private final class CarNavigationHost implements ViBRoCarNavigationController.Host {
        @Nullable
        @Override
        public NavState currentState() {
            return currentState;
        }

        @Override
        public void updateCurrentState(@Nullable NavState state) {
            ViBRoCarScreen.this.updateCurrentState(state);
        }
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value == null ? "null" : value;
    }
}
