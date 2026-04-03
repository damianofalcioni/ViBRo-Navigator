package com.vibenavigator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.vibenavigator.nav.NavState;
import com.vibenavigator.nav.NavigationLifecyclePolicy;
import com.vibenavigator.nav.NavigationRequest;
import com.vibenavigator.nav.NavigationService;
import com.vibenavigator.nav.NavigationStartupCoordinator;
import com.vibenavigator.util.AppLogger;

public class NavigationActivity extends Activity {

    public static final String EXTRA_RESUME_EXISTING = "resume_existing";

    private static final String TAG = "NavigationActivity";

    private TextView next;
    private TextView afterNext;
    private TextView gpsStatus;
    private TextView remaining;
    private Button blocked;
    private Button stop;

    private NavigationService.LocalBinder navBinder;
    private boolean bound;
    private final NavigationLifecyclePolicy lifecyclePolicy = new NavigationLifecyclePolicy();
    @Nullable
    private NavState currentState;
    private String lastRenderedStateKey = "";
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final NavigationStartupCoordinator startupCoordinator =
            new NavigationStartupCoordinator(new NavigationStartupHost());
    private final Runnable countdownTicker = new Runnable() {
        @Override
        public void run() {
            renderCountdown();
            uiHandler.postDelayed(this, 1000L);
        }
    };

    private final NavigationService.Listener navListener = state -> runOnUiThread(() -> render(state));

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            navBinder = (NavigationService.LocalBinder) service;
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

        ImageButton aboutButton = findViewById(R.id.aboutButton);
        aboutButton.setOnClickListener(v -> {
            AppLogger.i(TAG, "About button tapped");
            startActivity(new Intent(this, AboutActivity.class));
        });

        next = findViewById(R.id.nextDirectionText);
        afterNext = findViewById(R.id.afterNextDirectionText);
        gpsStatus = findViewById(R.id.gpsStatusText);
        remaining = findViewById(R.id.remainingText);
        blocked = findViewById(R.id.blockedRoadButton);
        stop = findViewById(R.id.stopNavButton);

        render(NavState.waiting(this));

        blocked.setOnClickListener(v -> {
            if (navBinder != null) {
                AppLogger.i(TAG, "Blocked-road reroute requested from UI");
                navBinder.addBlockedWaypoint();
            } else {
                AppLogger.w(TAG, "Blocked-road button tapped before service binding completed");
            }
        });

        stop.setOnClickListener(v -> {
            if (navBinder != null) {
                AppLogger.i(TAG, "Stop navigation requested from UI");
                navBinder.stop();
            }
            finish();
        });

        ensureReadyThenStart();
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
    protected void onStop() {
        super.onStop();
        AppLogger.i(TAG, "onStop bound=" + bound);
        uiHandler.removeCallbacks(countdownTicker);
        if (bound) {
            try {
                if (navBinder != null) {
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

    private void render(@NonNull NavState state) {
        currentState = state;
        next.setText(state.nextLine);
        afterNext.setText(state.afterNextLine);
        renderCountdown();
        remaining.setText(state.remainingBlock);
        String stateKey = state.nextLine + "|" + state.afterNextLine + "|" + state.accuracyLine
                + "|" + state.nextEvaluationDeadlineElapsedMs + "|" + state.remainingBlock;
        if (!stateKey.equals(lastRenderedStateKey)) {
            lastRenderedStateKey = stateKey;
            AppLogger.d(TAG, "Rendered state next=" + state.nextLine
                    + " afterNext=" + state.afterNextLine
                    + " accuracy=" + state.accuracyLine
                    + " nextEvalDeadline=" + state.nextEvaluationDeadlineElapsedMs
                    + " remaining=" + state.remainingBlock);
        }
    }

    private void renderCountdown() {
        if (currentState == null) {
            gpsStatus.setText(getString(
                    R.string.format_nav_gps_status,
                    getString(R.string.nav_status_unavailable),
                    getString(R.string.nav_status_unavailable)
            ));
            return;
        }
        String nextEvaluationValue = getString(R.string.nav_status_unavailable);
        long remainingMs = Math.max(0L, currentState.nextEvaluationDeadlineElapsedMs - SystemClock.elapsedRealtime());
        if (currentState.nextEvaluationDeadlineElapsedMs != NavState.NO_DEADLINE && remainingMs > 0L) {
            long remainingSeconds = (long) Math.ceil(remainingMs / 1000.0);
            nextEvaluationValue = getString(R.string.format_nav_next_position_check_value, remainingSeconds);
        }
        gpsStatus.setText(getString(
                R.string.format_nav_gps_status,
                currentState.accuracyLine,
                nextEvaluationValue
        ));
    }

    private void ensureReadyThenStart() {
        if (!startupCoordinator.isAutoStartNavigation()) {
            AppLogger.i(TAG, "NavigationActivity attached in resume mode, waiting for existing service state");
        }
        startupCoordinator.ensureReadyThenStart();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        NavigationLifecyclePolicy.BackPressAction action = lifecyclePolicy.onNavigationBackPressed();
        if (action == NavigationLifecyclePolicy.BackPressAction.MOVE_TASK_TO_BACKGROUND) {
            AppLogger.i(TAG, "Back pressed during navigation, moving task to background");
            if (!moveTaskToBack(true)) {
                finish();
            }
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AppLogger.i(TAG, "Permission result permissions=" + describePermissions(permissions, grantResults));
        startupCoordinator.onRequestPermissionsResult(requestCode);
    }

    @NonNull
    private String describeNavigationRequest() {
        return "resumeExisting=" + shouldResumeExistingNavigation()
                + ", " + NavigationRequest.fromIntent(getIntent()).describe();
    }

    private boolean shouldResumeExistingNavigation() {
        return getIntent().getBooleanExtra(EXTRA_RESUME_EXISTING, false);
    }

    private boolean hasNavigationRequest() {
        return NavigationRequest.fromIntent(getIntent()).isComplete();
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
            return NavigationRequest.fromIntent(getIntent());
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
                    .setPositiveButton(R.string.action_open_settings, (d, w) -> startActivity(settingsIntent))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }

        @Override
        public void showBatteryOptimizationDialog(@NonNull Intent settingsIntent) {
            new AlertDialog.Builder(NavigationActivity.this)
                    .setTitle(R.string.msg_permission_required)
                    .setMessage(R.string.msg_battery_opt_rationale)
                    .setPositiveButton(R.string.action_open_settings, (d, w) -> startActivity(settingsIntent))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }

        @Override
        public void startNavigationService(@NonNull NavigationRequest request) {
            Intent start = new Intent(NavigationActivity.this, NavigationService.class);
            start.setAction(NavigationService.ACTION_START);
            request.putInto(start);
            AppLogger.i(TAG, "Starting foreground navigation service " + request.describe());
            ContextCompat.startForegroundService(NavigationActivity.this, start);
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

    @NonNull
    private static String safe(@Nullable String value) {
        return value == null ? "null" : value;
    }
}
