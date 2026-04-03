package com.vibenavigator;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

import com.vibenavigator.nav.NavState;
import com.vibenavigator.nav.NavigationLifecyclePolicy;
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
        List<String> perms = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!perms.isEmpty()) {
            AppLogger.i(TAG, "Missing permissions=" + perms);
            boolean showRationale = false;
            for (String p : perms) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, p)) {
                    showRationale = true;
                    break;
                }
            }
            if (showRationale) {
                AppLogger.i(TAG, "Showing permission rationale for permissions=" + perms);
                String msg = getString(R.string.msg_permission_location_rationale);
                if (perms.contains(Manifest.permission.POST_NOTIFICATIONS)) {
                    msg = msg + "\n\n" + getString(R.string.msg_permission_notifications_rationale);
                }
                new AlertDialog.Builder(this)
                        .setTitle(R.string.msg_permission_required)
                        .setMessage(msg)
                        .setPositiveButton(android.R.string.ok, (d, w) ->
                                ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), REQ_PERMS))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            } else {
                AppLogger.i(TAG, "Requesting permissions directly permissions=" + perms);
                ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), REQ_PERMS);
            }
            return;
        }

        if (!isLocationEnabled()) {
            AppLogger.w(TAG, "Location services are disabled");
            new AlertDialog.Builder(this)
                    .setTitle(R.string.msg_permission_required)
                    .setMessage(R.string.msg_location_disabled)
                    .setPositiveButton(R.string.action_open_settings, (d, w) ->
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return;
        }

        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            AppLogger.w(TAG, "Notifications are disabled for the app");
            new AlertDialog.Builder(this)
                    .setTitle(R.string.msg_permission_required)
                    .setMessage(R.string.msg_enable_notifications)
                    .setPositiveButton(R.string.action_open_settings, (d, w) -> {
                        Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                        startActivity(i);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }

        maybeRequestIgnoreBatteryOptimizations();
        AppLogger.i(TAG, "Environment checks passed, starting navigation service");
        startNavigationService();
        autoStartNavigation = false;
    }

    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null) return false;
        boolean gps = false;
        boolean net = false;
        try {
            gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            net = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to query location providers", e);
        }
        return gps || net;
    }

    private void maybeRequestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm == null) {
            return;
        }
        if (pm.isIgnoringBatteryOptimizations(getPackageName())) {
            AppLogger.i(TAG, "Battery optimization exemption already granted");
            return;
        }
        AppLogger.i(TAG, "Prompting for battery optimization exemption");
        new AlertDialog.Builder(this)
                .setTitle(R.string.msg_permission_required)
                .setMessage(R.string.msg_battery_opt_rationale)
                .setPositiveButton(R.string.action_open_settings, (d, w) -> {
                    Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    i.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void startNavigationService() {
        Intent start = new Intent(this, NavigationService.class);
        start.setAction(NavigationService.ACTION_START);
        start.putExtra(EXTRA_PROFILE, getIntent().getStringExtra(EXTRA_PROFILE));
        start.putExtra(EXTRA_DEST_NAME, getIntent().getStringExtra(EXTRA_DEST_NAME));
        start.putExtra(EXTRA_DEST_LAT, getIntent().getDoubleExtra(EXTRA_DEST_LAT, Double.NaN));
        start.putExtra(EXTRA_DEST_LON, getIntent().getDoubleExtra(EXTRA_DEST_LON, Double.NaN));
        ArrayList<String> stops = getIntent().getStringArrayListExtra(EXTRA_STOPS);
        if (stops != null) {
            start.putStringArrayListExtra(EXTRA_STOPS, stops);
        }
        AppLogger.i(TAG, "Starting foreground navigation service profile="
                + safe(getIntent().getStringExtra(EXTRA_PROFILE))
                + " destination=(" + getIntent().getDoubleExtra(EXTRA_DEST_LAT, Double.NaN)
                + "," + getIntent().getDoubleExtra(EXTRA_DEST_LON, Double.NaN)
                + ") stops=" + (stops == null ? 0 : stops.size()));
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
        ArrayList<String> stops = getIntent().getStringArrayListExtra(EXTRA_STOPS);
        return "resumeExisting=" + shouldResumeExistingNavigation()
                + ", profile=" + safe(getIntent().getStringExtra(EXTRA_PROFILE))
                + ", destName=" + safe(getIntent().getStringExtra(EXTRA_DEST_NAME))
                + ", destLat=" + getIntent().getDoubleExtra(EXTRA_DEST_LAT, Double.NaN)
                + ", destLon=" + getIntent().getDoubleExtra(EXTRA_DEST_LON, Double.NaN)
                + ", stops=" + (stops == null ? 0 : stops.size());
    }

    private boolean shouldResumeExistingNavigation() {
        return getIntent().getBooleanExtra(EXTRA_RESUME_EXISTING, false);
    }

    private boolean hasNavigationRequest() {
        Intent intent = getIntent();
        if (intent == null) {
            return false;
        }
        return intent.hasExtra(EXTRA_PROFILE)
                && intent.hasExtra(EXTRA_DEST_LAT)
                && intent.hasExtra(EXTRA_DEST_LON);
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
