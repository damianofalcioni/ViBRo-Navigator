package vibro.navigator;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.poi.PoiHistoryStore;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.search.PoiSearchClient;
import vibro.navigator.poi.ui.PoiInputController;
import vibro.navigator.util.AppLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class MainActivityStopController {

    interface MapPickListener {
        void onPickStopFromMap(int stopIndex, @Nullable Poi initialPoi);
    }

    private static final String STATE_STOP_TEXTS = "stopTexts";
    private static final String STATE_STOP_SELECTED_NAMES = "stopSelectedNames";
    private static final String STATE_STOP_SELECTED_LATS = "stopSelectedLats";
    private static final String STATE_STOP_SELECTED_LONS = "stopSelectedLons";
    private static final String TAG = "MainStopController";

    @NonNull
    private final Activity activity;
    @NonNull
    private final LinearLayout stopsContainer;
    @NonNull
    private final PoiHistoryStore historyStore;
    @NonNull
    private final PoiSearchClient searchClient;
    @NonNull
    private final MapPickListener mapPickListener;
    @NonNull
    private final List<PoiInputController> stopControllers = new ArrayList<>();

    MainActivityStopController(
            @NonNull Activity activity,
            @NonNull LinearLayout stopsContainer,
            @NonNull PoiHistoryStore historyStore,
            @NonNull PoiSearchClient searchClient,
            @NonNull MapPickListener mapPickListener
    ) {
        this.activity = activity;
        this.stopsContainer = stopsContainer;
        this.historyStore = historyStore;
        this.searchClient = searchClient;
        this.mapPickListener = mapPickListener;
    }

    void restoreRows(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        ArrayList<String> stopTexts = savedInstanceState.getStringArrayList(STATE_STOP_TEXTS);
        if (stopTexts == null) {
            return;
        }
        AppLogger.i(TAG, "Restoring stop row shells count=" + stopTexts.size());
        for (int i = 0; i < stopTexts.size(); i++) {
            addStopRow(null);
        }
    }

    void restoreValues(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        ArrayList<String> stopTexts = savedInstanceState.getStringArrayList(STATE_STOP_TEXTS);
        if (stopTexts == null) {
            return;
        }
        ArrayList<String> selectedNames = savedInstanceState.getStringArrayList(STATE_STOP_SELECTED_NAMES);
        double[] selectedLats = savedInstanceState.getDoubleArray(STATE_STOP_SELECTED_LATS);
        double[] selectedLons = savedInstanceState.getDoubleArray(STATE_STOP_SELECTED_LONS);
        int restoreCount = Math.min(stopTexts.size(), stopControllers.size());
        AppLogger.i(TAG, "Restoring stop row values count=" + restoreCount);
        for (int i = 0; i < restoreCount; i++) {
            restoreValue(stopControllers.get(i), stopTexts, selectedNames, selectedLats, selectedLons, i);
        }
    }

    private static void restoreValue(
            @NonNull PoiInputController controller,
            @NonNull ArrayList<String> stopTexts,
            @Nullable ArrayList<String> selectedNames,
            @Nullable double[] selectedLats,
            @Nullable double[] selectedLons,
            int index
    ) {
        String selectedName = stringAt(selectedNames, index);
        double selectedLat = doubleAt(selectedLats, index);
        double selectedLon = doubleAt(selectedLons, index);
        if (isRestorablePoi(selectedName, selectedLat, selectedLon)) {
            controller.restorePoi(new Poi(selectedName, selectedLat, selectedLon));
            return;
        }

        String text = stopTexts.get(index);
        if (text != null && !text.isEmpty()) {
            controller.restoreText(text);
        }
    }

    private static boolean isRestorablePoi(@Nullable String name, double lat, double lon) {
        return name != null
                && !name.isEmpty()
                && !Double.isNaN(lat)
                && !Double.isNaN(lon);
    }

    @Nullable
    private static String stringAt(@Nullable ArrayList<String> values, int index) {
        return values != null && index < values.size() ? values.get(index) : null;
    }

    private static double doubleAt(@Nullable double[] values, int index) {
        return values != null && index < values.length ? values[index] : Double.NaN;
    }

    void saveState(@NonNull Bundle outState) {
        ArrayList<String> stopTexts = new ArrayList<>();
        ArrayList<String> selectedNames = new ArrayList<>();
        double[] selectedLats = new double[stopControllers.size()];
        double[] selectedLons = new double[stopControllers.size()];
        int index = 0;
        for (PoiInputController controller : stopControllers) {
            stopTexts.add(controller.getRawText());
            Poi selectedPoi = controller.getSelectedPoi();
            if (selectedPoi != null) {
                selectedNames.add(selectedPoi.name);
                selectedLats[index] = selectedPoi.lat;
                selectedLons[index] = selectedPoi.lon;
            } else {
                selectedNames.add("");
                selectedLats[index] = Double.NaN;
                selectedLons[index] = Double.NaN;
            }
            index++;
        }
        outState.putStringArrayList(STATE_STOP_TEXTS, stopTexts);
        outState.putStringArrayList(STATE_STOP_SELECTED_NAMES, selectedNames);
        outState.putDoubleArray(STATE_STOP_SELECTED_LATS, selectedLats);
        outState.putDoubleArray(STATE_STOP_SELECTED_LONS, selectedLons);
        AppLogger.d(TAG, "Saved instance state stopCount=" + stopTexts.size());
    }

    void addStopRow(@Nullable String initialText) {
        View row = activity.getLayoutInflater().inflate(R.layout.item_stop_row, stopsContainer, false);
        EditText stopEdit = row.findViewById(R.id.stopEdit);
        ImageButton mapButton = row.findViewById(R.id.stopMapButton);
        ImageButton remove = row.findViewById(R.id.removeStopButton);

        PoiInputController controller = new PoiInputController(
                activity,
                stopEdit,
                historyStore,
                searchClient,
                poi -> {
                }
        );
        stopControllers.add(controller);

        if (initialText != null) {
            controller.restoreText(initialText);
        }
        AppLogger.i(TAG, "Added stop row initialText=" + safe(initialText) + " totalStops=" + stopControllers.size());

        mapButton.setOnClickListener(v -> {
            int index = stopControllers.indexOf(controller);
            if (index < 0) {
                AppLogger.w(TAG, "Stop map request ignored because controller is no longer attached");
                return;
            }
            mapPickListener.onPickStopFromMap(index, resolveInitialPoi(controller));
        });
        remove.setOnClickListener(v -> removeStopRow(row, controller));

        stopsContainer.addView(row);
    }

    void dispose() {
        AppLogger.i(TAG, "Disposing stop rows count=" + stopControllers.size());
        for (PoiInputController controller : stopControllers) {
            controller.dispose();
        }
        stopControllers.clear();
    }

    int size() {
        return stopControllers.size();
    }

    @NonNull
    List<PoiInputController> getStopControllers() {
        return Collections.unmodifiableList(stopControllers);
    }

    void setStopPoi(int index, @NonNull Poi poi) {
        if (index < 0 || index >= stopControllers.size()) {
            AppLogger.w(TAG, "Ignoring stop selection for invalid index=" + index);
            return;
        }
        stopControllers.get(index).setPoi(poi);
    }

    private void removeStopRow(@NonNull View row, @NonNull PoiInputController controller) {
        controller.dispose();
        stopControllers.remove(controller);
        stopsContainer.removeView(row);
        AppLogger.i(TAG, "Removed stop row remainingStops=" + stopControllers.size());
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value == null ? "null" : value;
    }

    @Nullable
    private static Poi resolveInitialPoi(@NonNull PoiInputController controller) {
        Poi selectedPoi = controller.getSelectedPoi();
        if (selectedPoi != null) {
            return selectedPoi;
        }
        return controller.parseCurrentPoi();
    }
}
