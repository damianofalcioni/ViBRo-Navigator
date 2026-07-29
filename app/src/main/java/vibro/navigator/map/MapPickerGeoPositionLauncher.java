package vibro.navigator.map;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;

import vibro.navigator.R;
import vibro.navigator.android.intent.AndroidGeoCoordinatesViewIntent;
import vibro.navigator.geo.LatLon;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.poi.Poi;

final class MapPickerGeoPositionLauncher {
    private static final String TAG = "MapPickerGeoPosition";

    interface SelectedPoiSource {
        @Nullable
        Poi selectedPoi();
    }

    @NonNull
    private final Activity activity;
    @NonNull
    private final MapPickerScriptController scriptController;
    @NonNull
    private final SelectedPoiSource selectedPoiSource;

    MapPickerGeoPositionLauncher(
            @NonNull Activity activity,
            @NonNull MapPickerScriptController scriptController,
            @NonNull SelectedPoiSource selectedPoiSource
    ) {
        this.activity = activity;
        this.scriptController = scriptController;
        this.selectedPoiSource = selectedPoiSource;
    }

    void open() {
        Poi poi = selectedPoiSource.selectedPoi();
        if (poi != null) {
            launchGeoCoordinates(poi);
            return;
        }
        scriptController.requestCenter(this::openCenterFromJavascriptResult);
    }

    private void openCenterFromJavascriptResult(@Nullable String value) {
        try {
            launchCenter(MapPickerCenter.parseJavascriptResult(value));
        } catch (JSONException e) {
            AppLogger.w(TAG, "Failed to parse map center for geo intent", e);
            showPositionUnavailable();
        }
    }

    private void launchCenter(@Nullable MapPickerCenter center) {
        if (center == null) {
            showPositionUnavailable();
            return;
        }
        Poi poi = poiForCoordinates(center.lat, center.lon);
        if (poi == null) {
            showPositionUnavailable();
            return;
        }
        launchGeoCoordinates(poi);
    }

    private void launchGeoCoordinates(@NonNull Poi poi) {
        try {
            activity.startActivity(AndroidGeoCoordinatesViewIntent.createChooser(
                    activity,
                    poi.lat,
                    poi.lon,
                    poi.displayLabel()
            ));
            AppLogger.i(TAG, "Opened map position=" + poi.displayLabel());
        } catch (ActivityNotFoundException e) {
            AppLogger.w(TAG, "No app can open map position=" + poi.displayLabel(), e);
            Toast.makeText(activity, R.string.msg_map_position_no_app, Toast.LENGTH_SHORT).show();
        } catch (RuntimeException e) {
            AppLogger.w(TAG, "Failed to open map position=" + poi.displayLabel(), e);
            Toast.makeText(activity, R.string.msg_map_position_open_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    private Poi poiForCoordinates(double lat, double lon) {
        if (!LatLon.isValidCoordinate(lat, lon)) {
            return null;
        }
        return new Poi(activity.getString(R.string.format_coordinates, lat, lon), lat, lon);
    }

    private void showPositionUnavailable() {
        Toast.makeText(activity, R.string.msg_map_position_unavailable, Toast.LENGTH_SHORT).show();
    }
}
