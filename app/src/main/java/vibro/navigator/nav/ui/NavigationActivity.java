package vibro.navigator.nav.ui;

import vibro.navigator.R;


import vibro.navigator.nav.intent.NavigationRequestIntentContract;
import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.nav.service.NavigationServiceBinder;
import vibro.navigator.nav.startup.NavigationSettingsLauncher;
import vibro.navigator.nav.startup.NavigationStartupCoordinator;
import vibro.navigator.nav.policy.NavigationLifecyclePolicy;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.presentation.NavStateComposer;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import vibro.navigator.logging.AppLogger;

public class NavigationActivity extends Activity {

    public static final String EXTRA_RESUME_EXISTING = "resume_existing";

    private static final String TAG = "NavigationActivity";

    private NavigationServiceBinder navBinder;
    private boolean bound;
    private final NavigationLifecyclePolicy lifecyclePolicy = new NavigationLifecyclePolicy();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private NavigationActivityRenderer renderer;
    private NavigationActivityBackHandler backHandler;
    private final NavigationStartupCoordinator startupCoordinator =
            new NavigationStartupCoordinator(new NavigationStartupHost());
    private final Runnable countdownTicker = new Runnable() {
        @Override
        public void run() {
            renderer.renderGpsStatus();
            uiHandler.postDelayed(this, 1000L);
        }
    };

    private final NavigationService.Listener navListener = state -> runOnUiThread(() -> render(state));

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            navBinder = (NavigationServiceBinder) service;
            bound = true;
            AppLogger.i(TAG, "NavigationService connected component=" + name);
            navBinder.ensureForegroundNotification();
            navBinder.setNavigationUiVisible(true);
            navBinder.registerListener(navListener);
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        startupCoordinator.setAutoStartNavigation(
                savedInstanceState == null && hasNavigationRequest() && !shouldResumeExistingNavigation()
        );
        AppLogger.i(TAG, "onCreate savedState=" + (savedInstanceState != null)
                + " autoStartNavigation=" + startupCoordinator.isAutoStartNavigation()
                + " request=" + describeNavigationRequest());
        backHandler = new NavigationActivityBackHandler(this, lifecyclePolicy, this::runLegacyBackFallback);
        backHandler.registerPredictiveBackCallbackIfSupported();

        renderer = new NavigationActivityRenderer(this, uiHandler);
        render(NavStateComposer.waiting(this));
        configureControls();

        ensureReadyThenStart();
    }

    private void configureControls() {
        renderer.configureControls(new NavigationActivityRenderer.Controls() {
            @Override
            public void onBlockedRoad() {
                addBlockedWaypointFromUi();
            }

            @Override
            public void onStopNavigation() {
                showStopNavigationConfirmation();
            }

            @Override
            public void onTogglePaused() {
                togglePausedFromUi();
            }
        });
    }

    private void addBlockedWaypointFromUi() {
        if (navBinder != null) {
            AppLogger.i(TAG, "Blocked-road reroute requested from UI");
            navBinder.addBlockedWaypoint();
        } else {
            AppLogger.w(TAG, "Blocked-road button tapped before service binding completed");
        }
    }

    private void togglePausedFromUi() {
        if (navBinder == null) {
            AppLogger.w(TAG, "Pause/resume tapped before service binding completed");
            return;
        }
        if (navBinder.isPaused()) {
            AppLogger.i(TAG, "Resume navigation requested from UI");
            navBinder.resume();
        } else {
            AppLogger.i(TAG, "Pause navigation requested from UI");
            navBinder.pause();
        }
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
        uiHandler.post(countdownTicker);
        AppLogger.i(TAG, "Binding NavigationService");
        bindService(new Intent(this, NavigationService.class), connection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        uiHandler.removeCallbacks(countdownTicker);
        renderer.cancelPendingCompassTransition();
        if (bound) {
            try {
                if (navBinder != null) {
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

    private void showStopNavigationConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_stop_navigation_confirm)
                .setMessage(R.string.msg_stop_navigation_confirm)
                .setPositiveButton(R.string.action_stop_navigation, (dialog, which) -> {
                    if (navBinder != null) {
                        AppLogger.i(TAG, "Stop navigation requested from UI");
                        navBinder.stop();
                    } else {
                        AppLogger.w(TAG, "Stop navigation confirmed before service binding completed");
                    }
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void ensureReadyThenStart() {
        if (!startupCoordinator.isAutoStartNavigation()) {
            AppLogger.i(TAG, "NavigationActivity attached in resume mode, waiting for existing service state");
        }
        startupCoordinator.ensureReadyThenStart();
    }

    @Override
    @SuppressLint("GestureBackNavigation")
    public void onBackPressed() {
        backHandler.onLegacyBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AppLogger.i(TAG, "Permission result permissions=" + describePermissions(permissions, grantResults));
        startupCoordinator.onRequestPermissionsResult(requestCode);
    }

    private void runLegacyBackFallback() {
        super.onBackPressed();
    }

    @NonNull
    private String describeNavigationRequest() {
        return "resumeExisting=" + shouldResumeExistingNavigation()
                + ", " + NavigationRequestIntentContract.fromIntent(getIntent()).describe();
    }

    private boolean shouldResumeExistingNavigation() {
        return getIntent().getBooleanExtra(EXTRA_RESUME_EXISTING, false);
    }

    private boolean hasNavigationRequest() {
        return NavigationRequestIntentContract.fromIntent(getIntent()).isComplete();
    }

    private final class NavigationStartupHost implements NavigationStartupCoordinator.Host {
        @NonNull
        @Override
        public Activity getActivity() {
            return NavigationActivity.this;
        }

        @NonNull
        @Override
        public NavigationRequest getNavigationRequest() {
            return NavigationRequestIntentContract.fromIntent(getIntent());
        }

        @Override
        public void requestPermissions(@NonNull String[] permissions, int requestCode) {
            ActivityCompat.requestPermissions(NavigationActivity.this, permissions, requestCode);
        }

        @Override
        public void showPermissionRationale(@NonNull String message, @NonNull Runnable onContinue) {
            new AlertDialog.Builder(NavigationActivity.this)
                    .setTitle(R.string.msg_permission_required)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (d, w) -> onContinue.run())
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }

        @Override
        public void showSettingsRedirectDialog(int messageResId, @NonNull Intent settingsIntent) {
            new AlertDialog.Builder(NavigationActivity.this)
                    .setTitle(R.string.msg_permission_required)
                    .setMessage(messageResId)
                    .setPositiveButton(R.string.action_open_settings, (d, w) -> openSettings(settingsIntent))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }

        @Override
        public void showBatteryOptimizationDialog(@NonNull Intent settingsIntent) {
            new AlertDialog.Builder(NavigationActivity.this)
                    .setTitle(R.string.msg_permission_required)
                    .setMessage(R.string.msg_battery_opt_rationale)
                    .setPositiveButton(R.string.action_open_settings, (d, w) -> openSettings(settingsIntent))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }

        @Override
        public void startNavigationService(@NonNull NavigationRequest request) {
            Intent start = new Intent(NavigationActivity.this, NavigationService.class);
            start.setAction(NavigationService.ACTION_START);
            NavigationRequestIntentContract.putInto(start, request);
            AppLogger.i(TAG, "Starting foreground navigation service " + request.describe());
            ContextCompat.startForegroundService(NavigationActivity.this, start);
        }

        private void openSettings(@NonNull Intent settingsIntent) {
            if (NavigationSettingsLauncher.launch(NavigationActivity.this, settingsIntent)) {
                startupCoordinator.onSettingsOpened();
                return;
            }
            Toast.makeText(
                    NavigationActivity.this,
                    R.string.msg_open_settings_failed,
                    Toast.LENGTH_SHORT
            ).show();
        }
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

