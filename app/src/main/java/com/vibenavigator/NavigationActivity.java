package com.vibenavigator;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.vibenavigator.nav.NavState;
import com.vibenavigator.nav.NavigationService;

import java.util.ArrayList;
import java.util.List;

public class NavigationActivity extends AppCompatActivity {

    public static final String EXTRA_PROFILE = "profile";
    public static final String EXTRA_DEST_NAME = "dest_name";
    public static final String EXTRA_DEST_LAT = "dest_lat";
    public static final String EXTRA_DEST_LON = "dest_lon";
    public static final String EXTRA_STOPS = "stops";

    private static final int REQ_PERMS = 2001;

    private TextView next;
    private TextView afterNext;
    private TextView remaining;
    private Button blocked;
    private Button stop;

    private NavigationService.LocalBinder navBinder;
    private boolean bound;

    private final NavigationService.Listener navListener = state -> runOnUiThread(() -> render(state));

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            navBinder = (NavigationService.LocalBinder) service;
            bound = true;
            navBinder.registerListener(navListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
            navBinder = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        ImageButton aboutButton = findViewById(R.id.aboutButton);
        aboutButton.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));

        next = findViewById(R.id.nextDirectionText);
        afterNext = findViewById(R.id.afterNextDirectionText);
        remaining = findViewById(R.id.remainingText);
        blocked = findViewById(R.id.blockedRoadButton);
        stop = findViewById(R.id.stopNavButton);

        render(NavState.waiting(this));

        blocked.setOnClickListener(v -> {
            if (navBinder != null) {
                navBinder.addBlockedWaypoint();
            }
        });

        stop.setOnClickListener(v -> {
            if (navBinder != null) {
                navBinder.stop();
            }
            finish();
        });

        ensureReadyThenStart();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, NavigationService.class), connection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bound) {
            try {
                if (navBinder != null) {
                    navBinder.unregisterListener(navListener);
                }
            } catch (Exception ignored) {
                // ignore
            }
            unbindService(connection);
            bound = false;
            navBinder = null;
        }
    }

    private void render(@NonNull NavState state) {
        next.setText(state.nextLine);
        afterNext.setText(state.afterNextLine);
        remaining.setText(state.remainingBlock);
    }

    private void ensureReadyThenStart() {
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
            boolean showRationale = false;
            for (String p : perms) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, p)) {
                    showRationale = true;
                    break;
                }
            }
            if (showRationale) {
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
                ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), REQ_PERMS);
            }
            return;
        }

        if (!isLocationEnabled()) {
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
        startNavigationService();
    }

    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null) return false;
        boolean gps = false;
        boolean net = false;
        try {
            gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            net = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
            // ignore
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
            return;
        }
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
        ContextCompat.startForegroundService(this, start);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMS) {
            ensureReadyThenStart();
        }
    }
}
