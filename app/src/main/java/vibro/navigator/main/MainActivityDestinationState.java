package vibro.navigator.main;

import android.os.Bundle;

import androidx.annotation.NonNull;

import vibro.navigator.geo.LatLon;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.ui.PoiInputController;

final class MainActivityDestinationState {

    private static final String TAG = "MainActivity";
    private static final String STATE_DESTINATION_TEXT = "destinationText";
    private static final String STATE_DESTINATION_SELECTED_NAME = "destinationSelectedName";
    private static final String STATE_DESTINATION_SELECTED_LAT = "destinationSelectedLat";
    private static final String STATE_DESTINATION_SELECTED_LON = "destinationSelectedLon";

    private MainActivityDestinationState() {
    }

    static void save(@NonNull Bundle outState, @NonNull PoiInputController destinationController) {
        outState.putString(STATE_DESTINATION_TEXT, destinationController.getRawText());
        Poi selectedPoi = destinationController.getSelectedPoi();
        if (selectedPoi != null) {
            outState.putString(STATE_DESTINATION_SELECTED_NAME, selectedPoi.name);
            outState.putDouble(STATE_DESTINATION_SELECTED_LAT, selectedPoi.lat);
            outState.putDouble(STATE_DESTINATION_SELECTED_LON, selectedPoi.lon);
        }
    }

    static void restore(@NonNull Bundle savedInstanceState, @NonNull PoiInputController destinationController) {
        String selectedName = savedInstanceState.getString(STATE_DESTINATION_SELECTED_NAME);
        double selectedLat = savedInstanceState.getDouble(STATE_DESTINATION_SELECTED_LAT, Double.NaN);
        double selectedLon = savedInstanceState.getDouble(STATE_DESTINATION_SELECTED_LON, Double.NaN);
        if (selectedName != null && LatLon.isValidCoordinate(selectedLat, selectedLon)) {
            destinationController.restorePoi(new Poi(selectedName, selectedLat, selectedLon));
            AppLogger.i(TAG, "Restored selected destination POI=" + selectedName
                    + " (" + selectedLat + "," + selectedLon + ")");
            return;
        }

        String destinationText = savedInstanceState.getString(STATE_DESTINATION_TEXT);
        if (destinationText != null && !destinationText.isEmpty()) {
            destinationController.restoreText(destinationText);
            AppLogger.i(TAG, "Restored destination text=" + destinationText);
        }
    }
}
