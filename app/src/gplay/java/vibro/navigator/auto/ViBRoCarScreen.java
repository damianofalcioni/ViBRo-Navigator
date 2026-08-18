package vibro.navigator.auto;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.AppManager;
import androidx.car.app.CarToast;
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
import vibro.navigator.nav.service.NavigationServiceBinder;
import vibro.navigator.settings.AppThemeSettings;

// Android Auto requires templates, so the active screen renders the phone landscape UI onto the car map surface.
public final class ViBRoCarScreen extends Screen {

    private static final String TAG = "ViBRoCarScreen";
    private static final long NAVIGATION_SERVICE_ATTACH_RETRY_MS = 1_000L;

    private final CarContext carContext;
    private final ViBRoCarTemplates templates;
    private final ViBRoAutoCustomButtonController customButtonController;
    private final ViBRoCarNavigationController navigationController;
    private TaskScheduler uiScheduler;
    private ViBRoAutoSurfaceRenderer surfaceRenderer;
    private final Runnable surfaceCountdownTicker = new Runnable() {
        @Override
        public void run() {
            if (navigationController.ensureIntegrationEnabled()) {
                navigationController.bind();
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
        ViBRoAutoSurfaceControls controls = new ViBRoAutoSurfaceControls();
        templates = new ViBRoCarTemplates(carContext, controls);
        surfaceRenderer = new ViBRoAutoSurfaceRenderer(
                carContext,
                controls,
                uiScheduler,
                navigationController::setCompassStreetViewport
        );
        customButtonController = new ViBRoAutoCustomButtonController(carContext, new AutoCustomButtonHost());
        getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                carContext.getCarService(AppManager.class).setSurfaceCallback(surfaceRenderer);
                if (navigationController.ensureIntegrationEnabled()) {
                    navigationController.bind();
                }
                uiScheduler.post(surfaceCountdownTicker);
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                uiScheduler.removeCallbacks(surfaceCountdownTicker);
                surfaceRenderer.clearSurface();
                navigationController.unbind();
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
        if (!navigationController.ensureIntegrationEnabled()) {
            return templates.build(null);
        }
        NavState state = currentState;
        if (state != null) {
            surfaceRenderer.setState(state);
        }
        return templates.build(state);
    }

    private void openPhoneSettings() {
        navigationController.requestLocationSettingsRefreshOnReconnect();
        ViBRoAutoPhoneLauncher.openSettings(carContext);
    }

    private void showToast(int messageResId) {
        CarToast.makeText(carContext, messageResId, CarToast.LENGTH_SHORT).show();
    }

    private void openPhoneApp() {
        navigationController.openPhoneNavigationIfActive();
    }

    private void applyCarTheme() {
        carContext.setTheme(AppThemeSettings.isLightThemeEnabled(carContext)
                ? R.style.Theme_ViBRoNavigator_Light
                : R.style.Theme_ViBRoNavigator);
    }

    private void updateCurrentState(@Nullable NavState state) {
        currentState = state;
        surfaceRenderer.setState(state);
        invalidate();
    }

    private final class ViBRoAutoSurfaceControls
            implements ViBRoAutoSurfaceRenderer.Controls, ViBRoCarTemplates.Actions {
        @Override
        public void onOpenPhoneApp() {
            openPhoneApp();
        }

        @Override
        public void onBlockedRoad() {
            navigationController.addBlockedWaypoint();
        }

        @Override
        public void onStopNavigation() {
            navigationController.stopNavigation();
        }

        @Override
        public void onTogglePaused() {
            navigationController.togglePaused();
        }

        @Override
        public void onToggleCustomButton() {
            customButtonController.toggleSelectedSetting();
        }

        @Override
        @NonNull
        public String buildCurrentDirectionDetailsText() {
            return navigationController.buildCurrentDirectionDetailsText();
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

    private final class AutoCustomButtonHost implements ViBRoAutoCustomButtonController.Host {
        @Nullable
        @Override
        public NavigationServiceBinder currentBinder() {
            return navigationController.currentBinder();
        }

        @Override
        public void openPhoneSettings() {
            ViBRoCarScreen.this.openPhoneSettings();
        }

        @Override
        public void refreshSurfaceTheme() {
            applyCarTheme();
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
