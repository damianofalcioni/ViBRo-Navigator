package vibro.navigator.map;

import vibro.navigator.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import vibro.navigator.android.location.AndroidMapPickerLocationController;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.poi.Poi;
import vibro.navigator.logging.AppLogger;

public final class MapPickerActivity extends Activity {

    private static final String TAG = "MapPickerActivity";
    private static final double DEFAULT_CENTER_LAT = 20.0d;
    private static final double DEFAULT_CENTER_LON = 0.0d;
    private static final int DEFAULT_ZOOM = 2;
    private static final int SELECTED_ZOOM = 16;
    private static final int CURRENT_LOCATION_ZOOM = 17;

    private static final String STATE_SELECTED_NAME = "selected_name";
    private static final String STATE_SELECTED_LAT = "selected_lat";
    private static final String STATE_SELECTED_LON = "selected_lon";

    @NonNull
    public static Intent createIntent(
            @NonNull Context context,
            @NonNull String title,
            @Nullable Poi initialPoi
    ) {
        return MapPickerIntentContract.createIntent(context, title, initialPoi);
    }

    @Nullable
    public static Poi parseResult(@NonNull Context context, @Nullable Intent data) {
        return MapPickerIntentContract.parseResult(context, data);
    }

    private WebView mapWebView;
    private final MapPickerScriptController scriptController = new MapPickerScriptController();
    @Nullable
    private Poi selectedPoi;
    @Nullable
    private Poi initialPoi;
    @Nullable
    private AndroidMapPickerLocationController locationController;
    @Nullable
    private MapPoiOverlayController poiOverlayController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        mapWebView = findViewById(R.id.mapWebView);
        scriptController.attach(mapWebView);
        ImageButton mapZoomInButton = findViewById(R.id.mapZoomInButton);
        ImageButton mapZoomOutButton = findViewById(R.id.mapZoomOutButton);
        ImageButton mapCurrentLocationButton = findViewById(R.id.mapCurrentLocationButton);
        ImageButton mapCancelButton = findViewById(R.id.mapCancelButton);
        ImageButton mapUseSelectionButton = findViewById(R.id.mapUseSelectionButton);
        locationController = new AndroidMapPickerLocationController(
                this,
                new AndroidMapPickerLocationController.Callback() {
                    @Override
                    public void onCurrentLocation(@NonNull NavigationLocation location, boolean selectPoint) {
                        showCurrentLocation(location, selectPoint);
                    }

                    @Override
                    public void onLocationMessage(int messageResId) {
                        showLocationMessage(messageResId);
                    }
                }
        );
        poiOverlayController = MapPoiOverlayController.attach(this, scriptController);

        if (savedInstanceState != null) {
            selectedPoi = restorePoi(
                    savedInstanceState.getString(STATE_SELECTED_NAME),
                    savedInstanceState.getDouble(STATE_SELECTED_LAT, Double.NaN),
                    savedInstanceState.getDouble(STATE_SELECTED_LON, Double.NaN)
            );
        }
        initialPoi = MapPickerIntentContract.parseInitialPoi(getIntent());
        if (selectedPoi == null) {
            selectedPoi = initialPoi;
        }

        configureWebView();

        mapZoomInButton.setOnClickListener(v -> scriptController.zoomIn());
        mapZoomOutButton.setOnClickListener(v -> scriptController.zoomOut());
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
        if (poiOverlayController != null) {
            poiOverlayController.shutdown();
            poiOverlayController = null;
        }
        if (mapWebView != null) {
            mapWebView.removeJavascriptInterface("AndroidBridge");
            mapWebView.destroy();
            mapWebView = null;
        }
        scriptController.detach();
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
                scriptController.onPageLoaded();
                injectPoiLayerScript();
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

    private void injectPoiLayerScript() {
        MapPickerPoiScriptInjector.inject(this, scriptController);
    }

    private void initializeMap() {
        Poi centerPoi = selectedPoi != null ? selectedPoi : initialPoi;
        MapInitialView initialView = MapInitialView.from(centerPoi, selectedPoi);
        scriptController.initialize(
                initialView.centerLat,
                initialView.centerLon,
                initialView.zoom,
                initialView.selectedLat,
                initialView.selectedLon
        );
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
                    selectedPoi != null ? MapPickerScriptController.formatJsDouble(selectedPoi.lat) : "null",
                    selectedPoi != null ? MapPickerScriptController.formatJsDouble(selectedPoi.lon) : "null"
            );
        }
    }

    private void finishWithSelection() {
        if (selectedPoi == null) {
            Toast.makeText(this, R.string.msg_map_selection_required, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent data = new Intent();
        MapPickerIntentContract.putResult(data, selectedPoi);
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

    private void showCurrentLocation(@NonNull NavigationLocation location, boolean selectPoint) {
        Poi poi = poiForCoordinates(location.getLatitude(), location.getLongitude());
        if (selectPoint) {
            selectedPoi = poi;
        }
        scriptController.centerOn(poi, CURRENT_LOCATION_ZOOM, selectPoint);
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
        return MapPickerIntentContract.restorePoi(name, lat, lon);
    }

    private final class MapJavascriptBridge {
        @JavascriptInterface
        public void onSelectionChanged(double lat, double lon) {
            runOnUiThread(() -> {
                selectedPoi = poiForCoordinates(lat, lon);
                AppLogger.i(TAG, "Map selection changed lat=" + lat + " lon=" + lon);
            });
        }

        @JavascriptInterface
        public void onPoiSelected(@Nullable String name, double lat, double lon) {
            runOnUiThread(() -> {
                selectedPoi = poiForMapMarker(name, lat, lon);
                AppLogger.i(TAG, "Map POI selected=" + selectedPoi.displayLabel());
            });
        }

        @JavascriptInterface
        public void onMapViewChanged() {
            MapPoiOverlayController controller = poiOverlayController;
            if (controller != null) {
                runOnUiThread(controller::onMapViewChanged);
            }
        }
    }

    @NonNull
    private Poi poiForMapMarker(@Nullable String name, double lat, double lon) {
        if (name != null && !name.trim().isEmpty()) {
            return new Poi(name.trim(), lat, lon);
        }
        return poiForCoordinates(lat, lon);
    }
}

