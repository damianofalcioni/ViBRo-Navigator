package com.vibenavigator;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.activity.OnBackPressedCallback;

import com.vibenavigator.nav.NavState;
import com.vibenavigator.nav.NavigationLifecyclePolicy;
import com.vibenavigator.nav.NavigationPreflight;
import com.vibenavigator.nav.NavigationRequest;
import com.vibenavigator.nav.NavigationService;
import com.vibenavigator.util.AppLogger;

import java.util.ArrayList;
import java.util.List;

public class NavigationActivity extends AppCompatActivity {

    public static final String EXTRA_RESUME_EXISTING = "resume_existing";
    public static final String EXTRA_PROFILE = "profile";
    public static final String EXTRA_DEST_NAME = "dest_name";
    public static final String EXTRA_DEST_LAT = "dest_lat";
    public static final String EXTRA_DEST_LON = "dest_lon";
    public static final String EXTRA_STOPS = "stops";

    private static final int REQ_PERMS = 2001;
    private static final String TAG = "NavigationActivity";

    private TextView next;
    private TextView afterNext;
    private TextView gpsStatus;
    private TextView remaining;
    private Button blocked;
    private Button stop;

    private NavigationService.LocalBinder navBinder;
    private boolean bound;
    private boolean autoStartNavigation;
    private final NavigationLifecyclePolicy lifecyclePolicy = new NavigationLifecyclePolicy();
    @Nullable
    private NavState currentState;
    private String lastRenderedStateKey = "";
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
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
        autoStartNavigation = savedInstanceState == null && hasNavigationRequest() && !shouldResumeExistingNavigation();
        AppLogger.i(TAG, "onCreate savedState=" + (savedInstanceState != null)
                + " autoStartNavigation=" + autoStartNavigation
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

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                NavigationLifecyclePolicy.BackPressAction action = lifecyclePolicy.onNavigationBackPressed();
                if (action == NavigationLifecyclePolicy.BackPressAction.MOVE_TASK_TO_BACKGROUND) {
                    AppLogger.i(TAG, "Back pressed during navigation, moving task to background");
                    if (!moveTaskToBack(true)) {
                        finish();
                    }
                }
            }
        });

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
        autoStartNavigation = hasNavigationRequest() && !shouldResumeExistingNavigation();
        AppLogger.i(TAG, "onNewIntent autoStartNavigation=" + autoStartNavigation
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
        if (!autoStartNavigation) {
            AppLogger.i(TAG, "NavigationActivity attached in resume mode, waiting for existing service state");
            return;
        }

        NavigationPreflight.Status status = NavigationPreflight.inspect(this);
        if (status.hasMissingPermissions()) {
            AppLogger.i(TAG, "Missing permissions=" + status.missingPermissions);
            if (status.showPermissionRationale) {
                AppLogger.i(TAG, "Showing permission rationale for permissions=" + status.missingPermissions);
                String msg = getString(R.string.msg_permission_location_rationale);
                if (status.missingPermissions.contains(android.Manifest.permission.POST_NOTIFICATIONS)) {
                    msg = msg + "\n\n" + getString(R.string.msg_permission_notifications_rationale);
                }
                new AlertDialog.Builder(this)
                        .setTitle(R.string.msg_permission_required)
                        .setMessage(msg)
                        .setPositiveButton(android.R.string.ok, (d, w) ->
                                ActivityCompat.requestPermissions(
                                        this,
                                        status.missingPermissions.toArray(new String[0]),
                                        REQ_PERMS
                                ))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            } else {
                AppLogger.i(TAG, "Requesting permissions directly permissions=" + status.missingPermissions);
                ActivityCompat.requestPermissions(this, status.missingPermissions.toArray(new String[0]), REQ_PERMS);
            }
            return;
        }

        if (!status.locationEnabled) {
            AppLogger.w(TAG, "Location services are disabled");
            new AlertDialog.Builder(this)
                    .setTitle(R.string.msg_permission_required)
                    .setMessage(R.string.msg_location_disabled)
                    .setPositiveButton(R.string.action_open_settings, (d, w) ->
                            startActivity(NavigationPreflight.newLocationSettingsIntent()))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return;
        }

        if (!status.notificationsEnabled) {
            AppLogger.w(TAG, "Notifications are disabled for the app");
            new AlertDialog.Builder(this)
                    .setTitle(R.string.msg_permission_required)
                    .setMessage(R.string.msg_enable_notifications)
                    .setPositiveButton(R.string.action_open_settings, (d, w) ->
                            startActivity(NavigationPreflight.newNotificationSettingsIntent(this)))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }

        maybeRequestIgnoreBatteryOptimizations(status);
        AppLogger.i(TAG, "Environment checks passed, starting navigation service");
        startNavigationService();
        autoStartNavigation = false;
    }

    private void maybeRequestIgnoreBatteryOptimizations(@NonNull NavigationPreflight.Status status) {
        if (!status.needsBatteryOptimizationExemption) {
            AppLogger.i(TAG, "Battery optimization exemption already granted");
            return;
        }
        AppLogger.i(TAG, "Prompting for battery optimization exemption");
        new AlertDialog.Builder(this)
                .setTitle(R.string.msg_permission_required)
                .setMessage(R.string.msg_battery_opt_rationale)
                .setPositiveButton(R.string.action_open_settings, (d, w) ->
                        startActivity(NavigationPreflight.newBatteryOptimizationIntent(this)))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void startNavigationService() {
        Intent start = new Intent(this, NavigationService.class);
        start.setAction(NavigationService.ACTION_START);
        NavigationRequest request = NavigationRequest.fromIntent(getIntent());
        request.putInto(start);
        AppLogger.i(TAG, "Starting foreground navigation service " + request.describe());
        ContextCompat.startForegroundService(this, start);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMS) {
            AppLogger.i(TAG, "Permission result permissions=" + describePermissions(permissions, grantResults));
            ensureReadyThenStart();
        }
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
