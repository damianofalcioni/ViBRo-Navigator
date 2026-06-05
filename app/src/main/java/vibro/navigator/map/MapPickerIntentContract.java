package vibro.navigator.map;

import vibro.navigator.R;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;
import vibro.navigator.poi.Poi;

final class MapPickerIntentContract {

    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_INITIAL_NAME = "initial_name";
    private static final String EXTRA_INITIAL_LAT = "initial_lat";
    private static final String EXTRA_INITIAL_LON = "initial_lon";
    private static final String EXTRA_RESULT_NAME = "result_name";
    private static final String EXTRA_RESULT_LAT = "result_lat";
    private static final String EXTRA_RESULT_LON = "result_lon";

    private MapPickerIntentContract() {
    }

    @NonNull
    static Intent createIntent(
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
    static Poi parseInitialPoi(@NonNull Intent intent) {
        return restorePoi(
                intent.getStringExtra(EXTRA_INITIAL_NAME),
                intent.getDoubleExtra(EXTRA_INITIAL_LAT, Double.NaN),
                intent.getDoubleExtra(EXTRA_INITIAL_LON, Double.NaN)
        );
    }

    @Nullable
    static Poi parseResult(@NonNull Context context, @Nullable Intent data) {
        if (data == null) {
            return null;
        }
        double lat = data.getDoubleExtra(EXTRA_RESULT_LAT, Double.NaN);
        double lon = data.getDoubleExtra(EXTRA_RESULT_LON, Double.NaN);
        if (!LatLon.isValidCoordinate(lat, lon)) {
            return null;
        }
        String name = data.getStringExtra(EXTRA_RESULT_NAME);
        if (name == null || name.trim().isEmpty()) {
            name = context.getString(R.string.format_coordinates, lat, lon);
        }
        return new Poi(name, lat, lon);
    }

    static void putResult(@NonNull Intent data, @NonNull Poi poi) {
        data.putExtra(EXTRA_RESULT_NAME, poi.name);
        data.putExtra(EXTRA_RESULT_LAT, poi.lat);
        data.putExtra(EXTRA_RESULT_LON, poi.lon);
    }

    @Nullable
    static Poi restorePoi(@Nullable String name, double lat, double lon) {
        if (!LatLon.isValidCoordinate(lat, lon)) {
            return null;
        }
        String safeName = name == null ? "" : name;
        return new Poi(safeName, lat, lon);
    }
}

