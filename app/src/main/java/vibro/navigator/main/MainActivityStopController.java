package vibro.navigator.main;

import vibro.navigator.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiHistoryStore;
import vibro.navigator.poi.search.PoiSearchClient;
import vibro.navigator.poi.ui.PoiInputController;
import vibro.navigator.poi.ui.PoiReverseGeocodeController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class MainActivityStopController {

    interface MapPickListener {
        void onPickStopFromMap(@NonNull PoiInputController stopInputController);
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
    private final MainRouteRailStopAnchors routeRailStopAnchors;
    @NonNull
    private final List<PoiInputController> stopControllers = new ArrayList<>();

    MainActivityStopController(
            @NonNull Activity activity,
            @NonNull LinearLayout stopsContainer,
            @NonNull PoiHistoryStore historyStore,
            @NonNull PoiSearchClient searchClient,
            @NonNull MapPickListener mapPickListener
    ) {
        this(activity, stopsContainer, historyStore, searchClient, mapPickListener, null);
    }

    MainActivityStopController(
            @NonNull Activity activity,
            @NonNull LinearLayout stopsContainer,
            @NonNull PoiHistoryStore historyStore,
            @NonNull PoiSearchClient searchClient,
            @NonNull MapPickListener mapPickListener,
            @Nullable MainRouteRailView routeRailView
    ) {
        this.activity = activity;
        this.stopsContainer = stopsContainer;
        this.historyStore = historyStore;
        this.searchClient = searchClient;
        this.mapPickListener = mapPickListener;
        this.routeRailStopAnchors = new MainRouteRailStopAnchors(stopsContainer, routeRailView);
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
                && new Poi(name, lat, lon).hasValidCoordinates();
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

        PoiInputController controller = new PoiInputController(
                activity,
                row.findViewById(R.id.stopEdit),
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

        row.findViewById(R.id.stopMapButton).setOnClickListener(v -> {
            mapPickListener.onPickStopFromMap(controller);
        });
        row.findViewById(R.id.removeStopButton).setOnClickListener(v -> removeStopRow(row, controller));

        stopsContainer.addView(row);
        routeRailStopAnchors.refresh();
    }

    void replaceStops(@NonNull List<Poi> stops) {
        clearRows();
        MainActivityStopRowOperations.restoreStops(this, stops);
        AppLogger.i(TAG, "Replaced stop rows count=" + stopControllers.size());
    }

    void dispose() {
        AppLogger.i(TAG, "Disposing stop rows count=" + stopControllers.size());
        clearRows();
        routeRailStopAnchors.clear();
    }

    int size() {
        return stopControllers.size();
    }

    @NonNull
    List<PoiInputController> getStopControllers() {
        return Collections.unmodifiableList(stopControllers);
    }

    int indexOf(@NonNull PoiInputController controller) {
        return stopControllers.indexOf(controller);
    }

    void setStopPoi(
            int index,
            @NonNull Poi poi,
            @NonNull PoiReverseGeocodeController reverseGeocodeController
    ) {
        if (index < 0 || index >= stopControllers.size()) {
            AppLogger.w(TAG, "Ignoring stop selection for invalid index=" + index);
            return;
        }
        reverseGeocodeController.setPoiAndResolveAddress(stopControllers.get(index), poi);
    }

    private void removeStopRow(@NonNull View row, @NonNull PoiInputController controller) {
        controller.dispose();
        stopControllers.remove(controller);
        stopsContainer.removeView(row);
        routeRailStopAnchors.refresh();
        AppLogger.i(TAG, "Removed stop row remainingStops=" + stopControllers.size());
    }

    private void clearRows() {
        MainActivityStopRowOperations.disposeAll(stopControllers);
        stopControllers.clear();
        stopsContainer.removeAllViews();
        routeRailStopAnchors.refresh();
    }

    void addRestoredStop(@NonNull Poi stop) {
        addStopRow(null);
        stopControllers.get(stopControllers.size() - 1).restorePoi(stop);
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value == null ? "null" : value;
    }
}
