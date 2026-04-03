package com.vibenavigator;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.poi.PoiHistoryStore;
import com.vibenavigator.poi.search.PoiSearchClient;
import com.vibenavigator.poi.ui.PoiInputController;
import com.vibenavigator.util.AppLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class MainActivityStopController {

    private static final String STATE_STOP_TEXTS = "stopTexts";
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
    private final List<PoiInputController> stopControllers = new ArrayList<>();

    MainActivityStopController(
            @NonNull Activity activity,
            @NonNull LinearLayout stopsContainer,
            @NonNull PoiHistoryStore historyStore,
            @NonNull PoiSearchClient searchClient
    ) {
        this.activity = activity;
        this.stopsContainer = stopsContainer;
        this.historyStore = historyStore;
        this.searchClient = searchClient;
    }

    void restoreState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        ArrayList<String> stopTexts = savedInstanceState.getStringArrayList(STATE_STOP_TEXTS);
        if (stopTexts == null) {
            return;
        }
        AppLogger.i(TAG, "Restoring stop rows count=" + stopTexts.size());
        for (String text : stopTexts) {
            addStopRow(text);
        }
    }

    void saveState(@NonNull Bundle outState) {
        ArrayList<String> stopTexts = new ArrayList<>();
        for (PoiInputController controller : stopControllers) {
            stopTexts.add(controller.getRawText());
        }
        outState.putStringArrayList(STATE_STOP_TEXTS, stopTexts);
        AppLogger.d(TAG, "Saved instance state stopCount=" + stopTexts.size());
    }

    void addStopRow(@Nullable String initialText) {
        View row = activity.getLayoutInflater().inflate(R.layout.item_stop_row, stopsContainer, false);
        EditText stopEdit = row.findViewById(R.id.stopEdit);
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
            controller.setText(initialText);
        }
        AppLogger.i(TAG, "Added stop row initialText=" + safe(initialText) + " totalStops=" + stopControllers.size());

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
}
