package com.vibenavigator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.vibenavigator.poi.Poi;
import com.vibenavigator.util.AppLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MapPickerActivity extends Activity {

    private static final String TAG = "MapPickerActivity";
    private static final int REQUEST_LOCATION_PERMISSION = 4001;
    private static final long LOCATION_TIMEOUT_MS = 10_000L;
    private static final double DEFAULT_CENTER_LAT = 20.0d;
    private static final double DEFAULT_CENTER_LON = 0.0d;
    private static final int DEFAULT_ZOOM = 2;
    private static final int SELECTED_ZOOM = 16;
    private static final int CURRENT_LOCATION_ZOOM = 17;

    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_INITIAL_NAME = "initial_name";
    private static final String EXTRA_INITIAL_LAT = "initial_lat";
    private static final String EXTRA_INITIAL_LON = "initial_lon";
    private static final String EXTRA_RESULT_NAME = "result_name";
    private static final String EXTRA_RESULT_LAT = "result_lat";
    private static final String EXTRA_RESULT_LON = "result_lon";

    private static final String STATE_SELECTED_NAME = "selected_name";
    private static final String STATE_SELECTED_LAT = "selected_lat";
    private static final String STATE_SELECTED_LON = "selected_lon";

    @NonNull
    public static Intent createIntent(
            @NonNull Context context,
            @NonNull String title,
            @Nullable Poi initialPoi
    ) {
        Intent intent = new Intent(context, MapPickerActivity.class);
        intent.putExtra(EXTRA_TITLE, title);
        if (initialPoi != null) {
            intent.putExtra(EXTRA_INITIAL_NAME, initialPoi.name);
            intent.putExtra(EXTRA_INITIAL_LAT, initialPoi.lat);
            intent.putExtra(EXTRA_INITIAL_LON, initialPoi.lon);
        }
        return intent;
    }

    @Nullable
    public static Poi parseResult(@NonNull Context context, @Nullable Intent data) {
        if (data == null) {
            return null;
        }
        double lat = data.getDoubleExtra(EXTRA_RESULT_LAT, Double.NaN);
        double lon = data.getDoubleExtra(EXTRA_RESULT_LON, Double.NaN);
        if (Double.isNaN(lat) || Double.isNaN(lon)) {
            return null;
        }
        String name = data.getStringExtra(EXTRA_RESULT_NAME);
        if (name == null || name.trim().isEmpty()) {
            name = context.getString(R.string.format_coordinates, lat, lon);
        }
        return new Poi(name, lat, lon);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable locationTimeoutRunnable = () -> {
        stopLocationUpdates();
        Toast.makeText(this, R.string.msg_map_location_unavailable, Toast.LENGTH_SHORT).show();
        AppLogger.w(TAG, "Timed out waiting for a current location fix");
    };
    private final LocationListener singleFixListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            AppLogger.i(TAG, "Received current location provider=" + safeProvider(location)
                    + " lat=" + location.getLatitude()
                    + " lon=" + location.getLongitude());
            boolean selectPoint = pendingCurrentLocationSelection;
            stopLocationUpdates();
            showCurrentLocation(location, selectPoint);
        }
    };

    private WebView mapWebView;
    private boolean pageLoaded;
    @Nullable
    private Poi selectedPoi;
    @Nullable
    private Poi initialPoi;
    @Nullable
    private LocationManager locationManager;
    private boolean requestingLocationUpdate;
    private boolean pendingCurrentLocationSelection = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        mapWebView = findViewById(R.id.mapWebView);
        ImageButton mapZoomInButton = findViewById(R.id.mapZoomInButton);
        ImageButton mapZoomOutButton = findViewById(R.id.mapZoomOutButton);
        ImageButton mapCurrentLocationButton = findViewById(R.id.mapCurrentLocationButton);
        ImageButton mapCancelButton = findViewById(R.id.mapCancelButton);
        ImageButton mapUseSelectionButton = findViewById(R.id.mapUseSelectionButton);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (savedInstanceState != null) {
            selectedPoi = restorePoi(
                    savedInstanceState.getString(STATE_SELECTED_NAME),
                    savedInstanceState.getDouble(STATE_SELECTED_LAT, Double.NaN),
                    savedInstanceState.getDouble(STATE_SELECTED_LON, Double.NaN)
            );
        }
        initialPoi = restorePoi(
                getIntent().getStringExtra(EXTRA_INITIAL_NAME),
                getIntent().getDoubleExtra(EXTRA_INITIAL_LAT, Double.NaN),
                getIntent().getDoubleExtra(EXTRA_INITIAL_LON, Double.NaN)
        );
        if (selectedPoi == null) {
            selectedPoi = initialPoi;
        }

        configureWebView();

        mapZoomInButton.setOnClickListener(v -> runMapCommand("window.mapPicker.zoomIn();"));
        mapZoomOutButton.setOnClickListener(v -> runMapCommand("window.mapPicker.zoomOut();"));
        mapCurrentLocationButton.setOnClickListener(v -> centerOnCurrentLocation());
        mapCancelButton.setOnClickListener(v -> finish());
        mapUseSelectionButton.setOnClickListener(v -> finishWithSelection());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapWebView != null) {
            mapWebView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (mapWebView != null) {
            mapWebView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopLocationUpdates();
        if (mapWebView != null) {
            mapWebView.removeJavascriptInterface("AndroidBridge");
            mapWebView.destroy();
            mapWebView = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedPoi != null) {
            outState.putString(STATE_SELECTED_NAME, selectedPoi.name);
            outState.putDouble(STATE_SELECTED_LAT, selectedPoi.lat);
            outState.putDouble(STATE_SELECTED_LON, selectedPoi.lon);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_LOCATION_PERMISSION) {
            return;
        }
        if (hasLocationPermission()) {
            centerOnCurrentLocation(pendingCurrentLocationSelection);
            return;
        }
        Toast.makeText(this, R.string.msg_permission_required, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void configureWebView() {
        WebSettings settings = mapWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        mapWebView.setBackgroundColor(ContextCompat.getColor(this, R.color.black));
        mapWebView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mapWebView.addJavascriptInterface(new MapJavascriptBridge(), "AndroidBridge");
        mapWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                pageLoaded = true;
                initializeMap();
            }
        });
        mapWebView.loadUrl("file:///android_asset/map_picker.html");
    }

    private void initializeMap() {
        Poi centerPoi = selectedPoi != null ? selectedPoi : initialPoi;
        double centerLat = centerPoi != null ? centerPoi.lat : DEFAULT_CENTER_LAT;
        double centerLon = centerPoi != null ? centerPoi.lon : DEFAULT_CENTER_LON;
        int zoom = centerPoi != null ? SELECTED_ZOOM : DEFAULT_ZOOM;
        String selectedLat = selectedPoi != null ? formatJsDouble(selectedPoi.lat) : "null";
        String selectedLon = selectedPoi != null ? formatJsDouble(selectedPoi.lon) : "null";
        String script = String.format(
                Locale.US,
                "window.mapPicker.initialize(%s,%s,%d,%s,%s);",
                formatJsDouble(centerLat),
                formatJsDouble(centerLon),
                zoom,
                selectedLat,
                selectedLon
        );
        runMapCommand(script);
        AppLogger.i(TAG, "Initialized map centerLat=" + centerLat + " centerLon=" + centerLon
                + " zoom=" + zoom + " hasSelection=" + (selectedPoi != null));
        if (centerPoi == null) {
            centerOnCurrentLocation(false);
        }
    }

    private void finishWithSelection() {
        if (selectedPoi == null) {
            Toast.makeText(this, R.string.msg_map_selection_required, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent data = new Intent();
        data.putExtra(EXTRA_RESULT_NAME, selectedPoi.name);
        data.putExtra(EXTRA_RESULT_LAT, selectedPoi.lat);
        data.putExtra(EXTRA_RESULT_LON, selectedPoi.lon);
        setResult(RESULT_OK, data);
        AppLogger.i(TAG, "Returning map selection=" + selectedPoi.displayLabel());
        finish();
    }

    private void centerOnCurrentLocation() {
        centerOnCurrentLocation(true);
    }

    private void centerOnCurrentLocation(boolean selectPoint) {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_LOCATION_PERMISSION
            );
            return;
        }

        Location bestLastKnownLocation = findBestLastKnownLocation();
        if (bestLastKnownLocation != null) {
            AppLogger.i(TAG, "Using last known location provider=" + safeProvider(bestLastKnownLocation));
            showCurrentLocation(bestLastKnownLocation, selectPoint);
            return;
        }

        if (locationManager == null) {
            Toast.makeText(this, R.string.msg_map_location_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> providers = new ArrayList<>();
        if (isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            providers.add(LocationManager.GPS_PROVIDER);
        }
        if (isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            providers.add(LocationManager.NETWORK_PROVIDER);
        }
        if (providers.isEmpty()) {
            Toast.makeText(this, R.string.msg_location_disabled, Toast.LENGTH_SHORT).show();
            return;
        }

        stopLocationUpdates();
        try {
            for (String provider : providers) {
                locationManager.requestLocationUpdates(provider, 0L, 0f, singleFixListener, Looper.getMainLooper());
            }
            pendingCurrentLocationSelection = selectPoint;
            requestingLocationUpdate = true;
            mainHandler.postDelayed(locationTimeoutRunnable, LOCATION_TIMEOUT_MS);
            Toast.makeText(this, R.string.msg_map_location_searching, Toast.LENGTH_SHORT).show();
            AppLogger.i(TAG, "Requested fresh location from providers=" + providers);
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Failed to request current location updates", e);
            Toast.makeText(this, R.string.msg_permission_required, Toast.LENGTH_SHORT).show();
        }
    }

    private void runMapCommand(@NonNull String script) {
        if (!pageLoaded || mapWebView == null) {
            return;
        }
        mapWebView.evaluateJavascript(script, null);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Nullable
    private Location findBestLastKnownLocation() {
        if (locationManager == null || !hasLocationPermission()) {
            return null;
        }
        Location best = null;
        String[] providers = new String[]{
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
        };
        for (String provider : providers) {
            try {
                Location candidate = locationManager.getLastKnownLocation(provider);
                if (candidate == null) {
                    continue;
                }
                if (best == null || isBetterLocation(candidate, best)) {
                    best = candidate;
                }
            } catch (SecurityException e) {
                AppLogger.w(TAG, "Failed to read last known location provider=" + provider, e);
            }
        }
        return best;
    }

    private boolean isProviderEnabled(@NonNull String provider) {
        if (locationManager == null) {
            return false;
        }
        try {
            return locationManager.isProviderEnabled(provider);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to read provider state provider=" + provider, e);
            return false;
        }
    }

    private void stopLocationUpdates() {
        mainHandler.removeCallbacks(locationTimeoutRunnable);
        if (!requestingLocationUpdate || locationManager == null) {
            requestingLocationUpdate = false;
            pendingCurrentLocationSelection = true;
            return;
        }
        try {
            locationManager.removeUpdates(singleFixListener);
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Failed to remove location updates", e);
        }
        requestingLocationUpdate = false;
        pendingCurrentLocationSelection = true;
    }

    private void showCurrentLocation(@NonNull Location location, boolean selectPoint) {
        Poi poi = poiForCoordinates(location.getLatitude(), location.getLongitude());
        if (selectPoint) {
            selectedPoi = poi;
        }
        runMapCommand(String.format(
                Locale.US,
                "window.mapPicker.centerOn(%s,%s,%d,%s);",
                formatJsDouble(poi.lat),
                formatJsDouble(poi.lon),
                CURRENT_LOCATION_ZOOM,
                selectPoint ? "true" : "false"
        ));
    }

    private boolean isBetterLocation(@NonNull Location candidate, @NonNull Location best) {
        if (candidate.hasAccuracy() && best.hasAccuracy()) {
            float accuracyDelta = candidate.getAccuracy() - best.getAccuracy();
            if (accuracyDelta < -10f) {
                return true;
            }
            if (accuracyDelta > 10f) {
                return false;
            }
        }
        return candidate.getTime() > best.getTime();
    }

    @NonNull
    private Poi poiForCoordinates(double lat, double lon) {
        return new Poi(getString(R.string.format_coordinates, lat, lon), lat, lon);
    }

    @Nullable
    private static Poi restorePoi(@Nullable String name, double lat, double lon) {
        if (Double.isNaN(lat) || Double.isNaN(lon)) {
            return null;
        }
        String safeName = name == null ? "" : name;
        return new Poi(safeName, lat, lon);
    }

    @NonNull
    private static String formatJsDouble(double value) {
        return String.format(Locale.US, "%.8f", value);
    }

    @NonNull
    private static String safeProvider(@NonNull Location location) {
        String provider = location.getProvider();
        return provider == null ? "unknown" : provider;
    }

    private final class MapJavascriptBridge {
        @JavascriptInterface
        public void onSelectionChanged(double lat, double lon) {
            runOnUiThread(() -> {
                selectedPoi = poiForCoordinates(lat, lon);
                AppLogger.i(TAG, "Map selection changed lat=" + lat + " lon=" + lon);
            });
        }
    }
}
