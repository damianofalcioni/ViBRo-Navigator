package vibro.navigator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import vibro.navigator.poi.Poi;
import vibro.navigator.util.AppLogger;

import java.util.Locale;

public final class MapPickerActivity extends Activity {

    private static final String TAG = "MapPickerActivity";
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

    private WebView mapWebView;
    private boolean pageLoaded;
    @Nullable
    private Poi selectedPoi;
    @Nullable
    private Poi initialPoi;
    @Nullable
    private MapPickerLocationController locationController;

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
        locationController = new MapPickerLocationController(
                this,
                new MapPickerLocationController.Callback() {
                    @Override
                    public void onCurrentLocation(@NonNull Location location, boolean selectPoint) {
                        showCurrentLocation(location, selectPoint);
                    }

                    @Override
                    public void onLocationMessage(int messageResId) {
                        showLocationMessage(messageResId);
                    }
                }
        );

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
        if (locationController != null) {
            locationController.stopLocationUpdates();
            locationController = null;
        }
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
        if (locationController != null
                && locationController.onRequestPermissionsResult(requestCode)) {
            return;
        }
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

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request != null ? request.getUrl() : null;
                if (uri == null) {
                    return false;
                }
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }
        });
        mapWebView.loadUrl("file:///android_asset/map_picker.html");
    }

    private void initializeMap() {
        Poi centerPoi = selectedPoi != null ? selectedPoi : initialPoi;
        MapInitialView initialView = MapInitialView.from(centerPoi, selectedPoi);
        String script = initialView.toInitializeScript();
        runMapCommand(script);
        AppLogger.i(TAG, "Initialized map centerLat=" + initialView.centerLat + " centerLon=" + initialView.centerLon
                + " zoom=" + initialView.zoom + " hasSelection=" + (selectedPoi != null));
        if (centerPoi == null) {
            centerOnCurrentLocation(false);
        }
    }

    private static final class MapInitialView {
        final double centerLat;
        final double centerLon;
        final int zoom;
        @NonNull
        final String selectedLat;
        @NonNull
        final String selectedLon;

        private MapInitialView(
                double centerLat,
                double centerLon,
                int zoom,
                @NonNull String selectedLat,
                @NonNull String selectedLon
        ) {
            this.centerLat = centerLat;
            this.centerLon = centerLon;
            this.zoom = zoom;
            this.selectedLat = selectedLat;
            this.selectedLon = selectedLon;
        }

        @NonNull
        static MapInitialView from(@Nullable Poi centerPoi, @Nullable Poi selectedPoi) {
            return new MapInitialView(
                    centerPoi != null ? centerPoi.lat : DEFAULT_CENTER_LAT,
                    centerPoi != null ? centerPoi.lon : DEFAULT_CENTER_LON,
                    centerPoi != null ? SELECTED_ZOOM : DEFAULT_ZOOM,
                    selectedPoi != null ? formatJsDouble(selectedPoi.lat) : "null",
                    selectedPoi != null ? formatJsDouble(selectedPoi.lon) : "null"
            );
        }

        @NonNull
        String toInitializeScript() {
            return String.format(
                    Locale.US,
                    "window.mapPicker.initialize(%s,%s,%d,%s,%s);",
                    formatJsDouble(centerLat),
                    formatJsDouble(centerLon),
                    zoom,
                    selectedLat,
                    selectedLon
            );
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
        if (locationController != null) {
            locationController.centerOnCurrentLocation(selectPoint);
        }
    }

    private void runMapCommand(@NonNull String script) {
        if (!pageLoaded || mapWebView == null) {
            return;
        }
        mapWebView.evaluateJavascript(script, null);
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

    private void showLocationMessage(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
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
