package vibro.navigator.nav.ui;

import vibro.navigator.R;


import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.android.intent.AndroidNavigationRequestIntentContract;
import vibro.navigator.android.time.AndroidElapsedRealtimeClock;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.android.window.AndroidNavigationLockScreenWindow;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.nav.service.NavigationServiceBinder;
import vibro.navigator.nav.startup.NavigationStartupCoordinator;
import vibro.navigator.nav.policy.NavigationLifecyclePolicy;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.presentation.NavStateComposer;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;

public class NavigationActivity extends Activity {

    public static final String EXTRA_RESUME_EXISTING = "resume_existing";

    private static final String TAG = "NavigationActivity";

    private NavigationServiceBinder navBinder;
    private boolean bound;
    private boolean appliedLightTheme;
    private boolean refreshLocationSettingsOnReconnect;
    private final NavigationLifecyclePolicy lifecyclePolicy = new NavigationLifecyclePolicy();
    private final TaskScheduler uiScheduler = AndroidTaskScheduler.main();
    private final NavigationStartupSegmentsTreeAccess startupSegmentsTreeAccess =
            new NavigationStartupSegmentsTreeAccess(this);
    private NavigationActivityCommands commands;
    private NavigationActivityRenderer renderer;
    private NavigationActivityBackHandler backHandler;
    private final NavigationStartupCoordinator startupCoordinator =
            NavigationActivityStartupHost.createCoordinator(this);
    private final Runnable countdownTicker = new Runnable() {
        @Override
        public void run() {
            renderer.renderLiveDetails();
            uiScheduler.postDelayed(this, 1000L);
        }
    };

    private final NavigationService.Listener navListener = state -> uiScheduler.post(() -> render(state));

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            navBinder = (NavigationServiceBinder) service;
            bound = true;
            AppLogger.i(TAG, "NavigationService connected component=" + name);
            navBinder.ensureForegroundNotification();
            navBinder.setNavigationUiVisible(true);
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
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        appliedLightTheme = AndroidAppTheme.apply(this);
        super.onCreate(savedInstanceState);
        AndroidNavigationLockScreenWindow.allowOverLockScreen(this);
        setContentView(R.layout.activity_navigation);
        startupCoordinator.setAutoStartNavigation(
                savedInstanceState == null && hasNavigationRequest() && !shouldResumeExistingNavigation()
        );
        AppLogger.i(TAG, "onCreate savedState=" + (savedInstanceState != null)
                + " autoStartNavigation=" + startupCoordinator.isAutoStartNavigation()
                + " request=" + describeNavigationRequest());
        backHandler = new NavigationActivityBackHandler(this, lifecyclePolicy, this::runLegacyBackFallback);
        backHandler.registerPredictiveBackCallbackIfSupported();

        renderer = new NavigationActivityRenderer(
                this,
                uiScheduler,
                AndroidElapsedRealtimeClock.INSTANCE,
                () -> refreshLocationSettingsOnReconnect = true
        );
        commands = new NavigationActivityCommands(this, () -> navBinder);
        render(NavStateComposer.waiting(this));
        configureControls();

        ensureReadyThenStart();
    }

    private void configureControls() {
        renderer.configureControls(new NavigationActivityRenderer.Controls() {
            @Override
            public void onBlockedRoad() {
                commands.addBlockedWaypointFromUi();
            }

            @Override
            public void onStopNavigation() {
                commands.showStopNavigationConfirmation();
            }

            @Override
            public void onTogglePaused() {
                commands.togglePausedFromUi();
            }

            @Override
            public void onExportRoute() {
                commands.exportCurrentRouteFromUi();
            }
        });
    }

    private void showShortToast(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        startupCoordinator.setAutoStartNavigation(hasNavigationRequest() && !shouldResumeExistingNavigation());
        AppLogger.i(TAG, "onNewIntent autoStartNavigation=" + startupCoordinator.isAutoStartNavigation()
                + " request=" + describeNavigationRequest());
        ensureReadyThenStart();
    }

    @Override
    protected void onStart() {
        super.onStart();
        uiScheduler.post(countdownTicker);
        AppLogger.i(TAG, "Binding NavigationService");
        bindService(new Intent(this, NavigationService.class), connection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AndroidAppTheme.recreateIfThemeChanged(this, appliedLightTheme);
        renderer.refreshSettings();
        startupCoordinator.onResume();
    }

    @Override
    protected void onPause() {
        startupCoordinator.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        AppLogger.i(TAG, "onStop bound=" + bound);
        uiScheduler.removeCallbacks(countdownTicker);
        renderer.cancelPendingCompassTransition();
        renderer.dismissDetailsDialogs();
        if (bound) {
            try {
                if (navBinder != null) {
                    navBinder.setCompassStreetViewport(null);
                    navBinder.setNavigationUiVisible(false);
                    navBinder.unregisterListener(navListener);
                }
            } catch (Exception e) {
                AppLogger.w(TAG, "Failed to unregister navigation listener", e);
            }
            unbindService(connection);
            bound = false;
            navBinder = null;
        }
    }

    @Override
    protected void onDestroy() {
        backHandler.unregisterPredictiveBackCallbackIfNeeded();
        super.onDestroy();
    }

    private void render(@NonNull NavState state) {
        renderer.render(state, navBinder);
    }

    private void ensureReadyThenStart() {
        if (!startupCoordinator.isAutoStartNavigation()) {
            AppLogger.i(TAG, "NavigationActivity attached in resume mode, waiting for existing service state");
        }
        if (startupSegmentsTreeAccess.shouldRequest(startupCoordinator.isAutoStartNavigation())) {
            startupSegmentsTreeAccess.startPicker(this::ensureReadyThenStart);
            return;
        }
        startupCoordinator.ensureReadyThenStart();
    }

    @Override
    @SuppressLint("GestureBackNavigation")
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        backHandler.onLegacyBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AppLogger.i(TAG, "Permission result permissions=" + describePermissions(permissions, grantResults));
        boolean customButtonHandled = renderer != null
                && renderer.onRequestPermissionsResult(requestCode, grantResults);
        if (!customButtonHandled && NavigationActivityPermissionResultHandler.disableSurroundingStreetsWhenStorageDenied(
                this,
                permissions,
                grantResults
        )) {
            showShortToast(R.string.msg_compass_surrounding_streets_storage_permission_required);
        }
        startupCoordinator.onRequestPermissionsResult(requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (startupSegmentsTreeAccess.handleActivityResult(requestCode, resultCode, data, this::ensureReadyThenStart)) {
            return;
        }
        if (renderer != null && renderer.customButtonUi().onActivityResult(requestCode, resultCode, data)) {
            return;
        }
    }

    @SuppressWarnings("deprecation")
    private void runLegacyBackFallback() {
        super.onBackPressed();
    }

    @NonNull
    private String describeNavigationRequest() {
        return "resumeExisting=" + shouldResumeExistingNavigation()
                + ", " + AndroidNavigationRequestIntentContract.fromIntent(getIntent()).describe();
    }

    private boolean shouldResumeExistingNavigation() {
        return getIntent().getBooleanExtra(EXTRA_RESUME_EXISTING, false);
    }

    private boolean hasNavigationRequest() {
        return AndroidNavigationRequestIntentContract.fromIntent(getIntent()).isComplete();
    }

    void onStartupSettingsOpened() {
        startupCoordinator.onSettingsOpened();
    }

    @NonNull
    private static String describePermissions(@NonNull String[] permissions, @NonNull int[] grantResults) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < permissions.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(permissions[i]).append("=")
                    .append(i < grantResults.length ? grantResults[i] : "missing");
        }
        return sb.toString();
    }

}

