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

        adapter = new PoiSuggestionAdapter(context, this::deleteHistoryItem);
        popup = new ListPopupWindow(context);
        popup.setAnchorView(editText);
        popup.setAdapter(adapter);
        popup.setModal(true);
        popup.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(context, R.color.black)));
        popup.setOnItemClickListener((parent, view, position, id) -> {
            PoiSuggestion s = (PoiSuggestion) adapter.getItem(position);
            selectPoi(s.poi);
        });

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showHistory();
            } else {
                popup.dismiss();
            }
        });
        editText.setOnClickListener(v -> showHistory());

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
        popup.dismiss();
        executor.shutdownNow();
    }

    public void setText(@NonNull String text) {
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
        if (!items.isEmpty() && editText.hasFocus()) {
            popup.show();
            popup.getListView().setItemsCanFocus(true);
        }
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
        return CoordinateParser.tryParse(raw, raw);
    }

    private void deleteHistoryItem(@NonNull PoiSuggestion suggestion) {
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
            adapter.setItems(singleSuggestion(coords, false));
            if (editText.hasFocus()) {
                popup.show();
                popup.getListView().setItemsCanFocus(true);
            }
            return;
        }

        if (query.length() <= 3) {
            showHistory();
            return;
        }

        pendingSearch = () -> runSearch(query);
        mainHandler.postDelayed(pendingSearch, 300);
    }

    private void runSearch(@NonNull String query) {
        if (inFlight != null) {
            inFlight.cancel(true);
        }
        inFlight = executor.submit(() -> {
            try {
                List<Poi> results = searchClient.search(query, 10);
                List<PoiSuggestion> suggestions = new ArrayList<>();
                for (Poi p : results) {
                    suggestions.add(new PoiSuggestion(p, false));
                }
                mainHandler.post(() -> {
                    adapter.setItems(suggestions);
                    if (!suggestions.isEmpty() && editText.hasFocus()) {
                        popup.show();
                        popup.getListView().setItemsCanFocus(true);
                    }
                });
            } catch (IOException ignored) {
                mainHandler.post(() -> adapter.setItems(new ArrayList<>()));
            }
        });
    }

    private void selectPoi(@NonNull Poi poi) {
        String label = editText.getContext().getString(R.string.format_poi_suggestion, poi.name, poi.lat, poi.lon);
        programmaticChange = true;
        editText.setText(label);
        editText.setSelection(label.length());
        popup.dismiss();
        selectedPoi = poi;
        history.addOrPromote(poi);
        listener.onPoiSelected(poi);
    }

    @NonNull
    private static List<PoiSuggestion> singleSuggestion(@NonNull Poi poi, boolean deletable) {
        List<PoiSuggestion> items = new ArrayList<>();
        items.add(new PoiSuggestion(poi, deletable));
        return items;
    }
}
