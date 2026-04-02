package com.vibenavigator.poi.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.core.content.ContextCompat;

import com.vibenavigator.R;
import com.vibenavigator.poi.CoordinateParser;
import com.vibenavigator.poi.Poi;
import com.vibenavigator.poi.PoiHistoryStore;
import com.vibenavigator.poi.search.PoiSearchClient;
import com.vibenavigator.util.AppLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class PoiInputController {

    public interface Listener {
        void onPoiSelected(@NonNull Poi poi);
    }

    private final EditText editText;
    private final PoiHistoryStore history;
    private final PoiSearchClient searchClient;
    private final Listener listener;

    private final ListPopupWindow popup;
    private final PoiSuggestionAdapter adapter;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final String logTag;

    private Future<?> inFlight;
    private Runnable pendingSearch;
    private Poi selectedPoi;
    private boolean programmaticChange;

    public PoiInputController(
            @NonNull Context context,
            @NonNull EditText editText,
            @NonNull PoiHistoryStore history,
            @NonNull PoiSearchClient searchClient,
            @NonNull Listener listener
    ) {
        this.editText = editText;
        this.history = history;
        this.searchClient = searchClient;
        this.listener = listener;
        this.logTag = "PoiInputController#" + Integer.toHexString(System.identityHashCode(this));
        AppLogger.i(logTag, "Created controller");

        adapter = new PoiSuggestionAdapter(context, new PoiSuggestionAdapter.Listener() {
            @Override
            public void onSuggestionClicked(@NonNull PoiSuggestion suggestion) {
                selectPoi(suggestion.poi);
            }

            @Override
            public void onDeleteClicked(@NonNull PoiSuggestion suggestion) {
                deleteHistoryItem(suggestion);
            }
        });
        popup = new ListPopupWindow(context);
        popup.setAnchorView(editText);
        popup.setAdapter(adapter);
        popup.setModal(false);
        popup.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(context, R.color.black)));
        popup.setOnItemClickListener((parent, view, position, id) -> {
            PoiSuggestion s = (PoiSuggestion) adapter.getItem(position);
            selectPoi(s.poi);
        });

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                AppLogger.d(logTag, "Input focused text=" + getRawText().trim());
                maybeShowHistory();
            } else {
                AppLogger.d(logTag, "Input lost focus");
                popup.dismiss();
            }
        });
        editText.setOnClickListener(v -> {
            AppLogger.d(logTag, "Input clicked");
            maybeShowHistory();
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!programmaticChange) {
                    selectedPoi = null;
                }
                scheduleSearch(s.toString());
                programmaticChange = false;
            }
        });
    }

    public void dispose() {
        AppLogger.i(logTag, "Disposing controller");
        popup.dismiss();
        executor.shutdownNow();
    }

    public void setText(@NonNull String text) {
        AppLogger.d(logTag, "Programmatically setting text=" + text);
        programmaticChange = true;
        editText.setText(text);
        editText.setSelection(text.length());
    }

    @NonNull
    public EditText getEditText() {
        return editText;
    }

    public void showHistory() {
        List<PoiSuggestion> items = new ArrayList<>();
        for (Poi p : history.list()) {
            items.add(new PoiSuggestion(p, true));
        }
        adapter.setItems(items);
        AppLogger.d(logTag, "Showing history items=" + items.size());
        if (!items.isEmpty() && editText.hasFocus()) {
            popup.show();
            popup.getListView().setItemsCanFocus(true);
        }
    }

    private void maybeShowHistory() {
        if (!getRawText().trim().isEmpty()) {
            return;
        }
        showHistory();
    }

    @NonNull
    public String getRawText() {
        return editText.getText() != null ? editText.getText().toString() : "";
    }

    @Nullable
    public Poi getSelectedPoi() {
        return selectedPoi;
    }

    @Nullable
    public Poi parseCurrentPoi() {
        String raw = getRawText().trim();
        if (raw.isEmpty()) {
            return null;
        }
        Poi parsed = CoordinateParser.tryParse(raw, raw);
        AppLogger.d(logTag, "Parsed current text as coordinates success=" + (parsed != null) + " raw=" + raw);
        return parsed;
    }

    private void deleteHistoryItem(@NonNull PoiSuggestion suggestion) {
        AppLogger.i(logTag, "Deleting history item=" + suggestion.poi.displayLabel());
        history.remove(suggestion.poi);
        showHistory();
    }

    private void scheduleSearch(@NonNull String raw) {
        if (pendingSearch != null) {
            mainHandler.removeCallbacks(pendingSearch);
        }

        String query = raw.trim();
        Poi coords = CoordinateParser.tryParse(query, query);
        if (coords != null) {
            AppLogger.d(logTag, "Recognized direct coordinate entry query=" + query);
            adapter.setItems(singleSuggestion(coords, false));
            if (editText.hasFocus()) {
                popup.show();
                popup.getListView().setItemsCanFocus(true);
            }
            return;
        }

        if (query.length() <= 3) {
            if (query.isEmpty()) {
                AppLogger.d(logTag, "Empty query, showing history");
                showHistory();
            } else {
                AppLogger.d(logTag, "Query too short for search query=" + query);
                popup.dismiss();
                adapter.setItems(new ArrayList<>());
            }
            return;
        }

        pendingSearch = () -> runSearch(query);
        AppLogger.d(logTag, "Scheduling search query=" + query);
        mainHandler.postDelayed(pendingSearch, 300);
    }

    private void runSearch(@NonNull String query) {
        if (inFlight != null) {
            inFlight.cancel(true);
            AppLogger.d(logTag, "Cancelled in-flight search before starting query=" + query);
        }
        inFlight = executor.submit(() -> {
            try {
                AppLogger.i(logTag, "Running search query=" + query);
                List<Poi> results = searchClient.search(query, 10);
                List<PoiSuggestion> suggestions = new ArrayList<>();
                for (Poi p : results) {
                    suggestions.add(new PoiSuggestion(p, false));
                }
                mainHandler.post(() -> {
                    adapter.setItems(suggestions);
                    AppLogger.i(logTag, "Search finished query=" + query + " suggestions=" + suggestions.size());
                    if (!suggestions.isEmpty() && editText.hasFocus()) {
                        popup.show();
                        popup.getListView().setItemsCanFocus(true);
                    }
                });
            } catch (IOException e) {
                AppLogger.e(logTag, "Search failed query=" + query, e);
                mainHandler.post(() -> adapter.setItems(new ArrayList<>()));
            }
        });
    }

    private void selectPoi(@NonNull Poi poi) {
        String label = poi.displayLabel();
        programmaticChange = true;
        editText.setText(label);
        editText.setSelection(label.length());
        popup.dismiss();
        selectedPoi = poi;
        history.addOrPromote(poi);
        AppLogger.i(logTag, "Selected POI=" + poi.displayLabel());
        listener.onPoiSelected(poi);
    }

    @NonNull
    private static List<PoiSuggestion> singleSuggestion(@NonNull Poi poi, boolean deletable) {
        List<PoiSuggestion> items = new ArrayList<>();
        items.add(new PoiSuggestion(poi, deletable));
        return items;
    }
}
