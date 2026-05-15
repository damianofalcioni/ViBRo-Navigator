package vibro.navigator.auto;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.main.MainActivity;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.nav.service.NavigationServiceBinder;

// Android Auto requires templates, so the active screen renders the phone landscape UI onto the car map surface.
@SuppressWarnings({"PMD.TooManyMethods", "deprecation", "PMD.CouplingBetweenObjects"})
public final class ViBRoCarScreen extends Screen {

    private static final String TAG = "ViBRoCarScreen";
    private static final long SURFACE_COUNTDOWN_TICK_MS = 1_000L;

    private final CarContext carContext;
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
        surfaceRenderer = new ViBRoAutoSurfaceRenderer(carContext, new ViBRoAutoSurfaceControls());
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
        if (state == null) {
            return buildConnectingTemplate();
        }
        if (isNoActiveNavigation(state)) {
            return buildNoActiveNavigationTemplate();
        }
        return buildNavigationTemplate(state);
    }

    @NonNull
    private Template buildConnectingTemplate() {
        Pane pane = new Pane.Builder()
                .addRow(new Row.Builder()
                        .setTitle(text(R.string.auto_title))
                        .addText(text(R.string.auto_connecting))
                        .build())
                .build();
        return buildPaneTemplate(text(R.string.auto_title), pane);
    }

    @NonNull
    private Template buildNoActiveNavigationTemplate() {
        Pane.Builder pane = new Pane.Builder()
                .addRow(new Row.Builder()
                        .setTitle(text(R.string.auto_no_active_navigation_title))
                        .addText(text(R.string.auto_no_active_navigation_text))
                        .build());
        pane.addAction(new Action.Builder()
                .setTitle(text(R.string.auto_open_phone))
                .setOnClickListener(this::openPhoneApp)
                .build());
        return buildPaneTemplate(text(R.string.auto_title), pane.build());
    }

    @NonNull
    private Template buildNavigationTemplate(@NonNull NavState state) {
        surfaceRenderer.setState(state);
        return new NavigationTemplate.Builder()
                .setActionStrip(buildNavigationActionStrip(state))
                .build();
    }

    @NonNull
    private ActionStrip buildNavigationActionStrip(@NonNull NavState state) {
        return new ActionStrip.Builder()
                .addAction(buildIconAction(
                        R.string.action_blocked_road,
                        R.drawable.ic_blocked_road,
                        this::addBlockedWaypoint,
                        !state.pauseStatus.paused
                ))
                .addAction(buildIconAction(
                        R.string.action_stop_navigation,
                        R.drawable.ic_stop,
                        this::stopNavigation,
                        true
                ))
                .addAction(buildIconAction(
                        pauseResumeTitle(state),
                        state.pauseStatus.paused ? R.drawable.ic_play : R.drawable.ic_pause,
                        this::togglePaused,
                        true
                ))
                .build();
    }

    @NonNull
    private Action buildIconAction(int titleResId, int iconResId, @NonNull Runnable listener, boolean enabled) {
        return new Action.Builder()
                .setTitle(text(titleResId))
                .setIcon(new CarIcon.Builder(IconCompat.createWithResource(carContext, iconResId)).build())
                .setOnClickListener(listener::run)
                .setEnabled(enabled)
                .build();
    }

    @NonNull
    private Template buildPaneTemplate(@NonNull String title, @NonNull Pane pane) {
        return new PaneTemplate.Builder(pane)
                .setTitle(title)
                .setHeaderAction(Action.APP_ICON)
                .build();
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

    private void openPhoneApp() {
        Intent intent = new Intent(carContext, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        carContext.startActivity(intent);
    }

    private boolean isNoActiveNavigation(@NonNull NavState state) {
        return text(R.string.nav_no_route).equals(state.routeStatus.guidance.nextLine.trim())
                && state.routeStatus.progress.destinationLine.trim().isEmpty()
                && state.routeStatus.progress.stopProgressBlock.trim().isEmpty()
                && state.routeStatus.progress.detailBlock.trim().isEmpty();
    }

    private int pauseResumeTitle(@NonNull NavState state) {
        return state.pauseStatus.paused
                ? R.string.action_resume_navigation
                : R.string.action_pause_navigation;
    }

    private final class ViBRoAutoSurfaceControls implements ViBRoAutoSurfaceRenderer.Controls {
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
    }

    @NonNull
    private String text(int resId) {
        return carContext.getString(resId);
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value == null ? "null" : value;
    }
}
