package com.vibenavigator.poi.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListPopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.vibenavigator.R;
import com.vibenavigator.poi.CoordinateParser;
import com.vibenavigator.poi.Poi;
import com.vibenavigator.poi.PoiHistoryStore;
import com.vibenavigator.poi.search.PoiSearchClient;
import com.vibenavigator.util.AppLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final String logTag;

    private Future<?> inFlight;
    private Runnable pendingSearch;
    private int searchGeneration;
    private Poi selectedPoi;
    private boolean programmaticChange;
    private boolean suppressNextSearch;

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
            public void onEditClicked(@NonNull PoiSuggestion suggestion) {
                promptRenameHistoryItem(suggestion);
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
                if (suppressNextSearch) {
                    AppLogger.d(logTag, "Suppressing search after programmatic POI selection");
                    cancelPendingSearch();
                    cancelInFlightSearch();
                    dismissPopup();
                    suppressNextSearch = false;
                    programmaticChange = false;
                    return;
                }
                scheduleSearch(s.toString());
                programmaticChange = false;
            }
        });
    }

    public void dispose() {
        AppLogger.i(logTag, "Disposing controller");
        cancelPendingSearch();
        cancelInFlightSearch();
        dismissPopup();
    }

    public void setText(@NonNull String text) {
        AppLogger.d(logTag, "Programmatically setting text=" + text);
        programmaticChange = true;
        editText.setText(text);
        editText.setSelection(text.length());
    }

    public void restoreText(@NonNull String text) {
        AppLogger.d(logTag, "Restoring raw text without suggestions=" + text);
        suppressNextSearch = true;
        programmaticChange = true;
        selectedPoi = null;
        editText.setText(text);
        editText.setSelection(text.length());
        dismissPopup();
    }

    public void setPoi(@NonNull Poi poi) {
        String label = poi.displayLabel();
        AppLogger.d(logTag, "Programmatically setting POI=" + label);
        suppressNextSearch = true;
        programmaticChange = true;
        selectedPoi = poi;
        editText.setText(label);
        editText.setSelection(label.length());
        dismissPopup();
        history.addOrPromote(poi);
        listener.onPoiSelected(poi);
    }

    public void restorePoi(@NonNull Poi poi) {
        String label = poi.displayLabel();
        AppLogger.d(logTag, "Restoring POI without suggestions=" + label);
        suppressNextSearch = true;
        programmaticChange = true;
        selectedPoi = poi;
        editText.setText(label);
        editText.setSelection(label.length());
        dismissPopup();
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
            showPopupIfPossible("history");
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

    private void promptRenameHistoryItem(@NonNull PoiSuggestion suggestion) {
        Context context = editText.getContext();
        Poi poi = suggestion.poi;
        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setSingleLine(true);
        input.setText(poi.displayLabel());
        input.setSelection(input.getText().length());

        int horizontalPaddingPx = Math.round(context.getResources().getDisplayMetrics().density * 24f);
        FrameLayout container = new FrameLayout(context);
        container.setPadding(horizontalPaddingPx, 0, horizontalPaddingPx, 0);
        container.addView(input, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.title_edit_destination_name)
                .setView(container)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_save, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String updatedName = input.getText() != null ? input.getText().toString().trim() : "";
            if (updatedName.isEmpty()) {
                input.setError(context.getString(R.string.msg_invalid_destination_name));
                return;
            }

            if (!history.rename(poi, updatedName)) {
                dialog.dismiss();
                return;
            }

            Poi renamedPoi = new Poi(updatedName, poi.lat, poi.lon);
            if (selectedPoi != null && selectedPoi.stableKey().equals(poi.stableKey())) {
                selectedPoi = renamedPoi;
                if (getRawText().trim().equals(poi.displayLabel())) {
                    programmaticChange = true;
                    editText.setText(renamedPoi.displayLabel());
                    editText.setSelection(editText.getText().length());
                }
            }
            AppLogger.i(logTag, "Renamed history item key=" + poi.stableKey() + " newName=" + updatedName);
            showHistory();
            dialog.dismiss();
        }));
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        input.requestFocus();
    }

    private void scheduleSearch(@NonNull String raw) {
        cancelPendingSearch();

        String query = raw.trim();
        Poi coords = CoordinateParser.tryParse(query, query);
        if (coords != null) {
            cancelInFlightSearch();
            AppLogger.d(logTag, "Recognized direct coordinate entry query=" + query);
            adapter.setItems(singleSuggestion(coords, false));
            if (editText.hasFocus()) {
                showPopupIfPossible("coordinate-entry");
            }
            return;
        }

        if (query.length() <= 3) {
            cancelInFlightSearch();
            if (query.isEmpty()) {
                AppLogger.d(logTag, "Empty query, showing history");
                showHistory();
            } else {
                AppLogger.d(logTag, "Query too short for search query=" + query);
                dismissPopup();
                adapter.setItems(new ArrayList<>());
            }
            return;
        }

        pendingSearch = () -> runSearch(query);
        AppLogger.d(logTag, "Scheduling search query=" + query);
        mainHandler.postDelayed(pendingSearch, 300);
    }

    private void cancelPendingSearch() {
        if (pendingSearch != null) {
            mainHandler.removeCallbacks(pendingSearch);
            pendingSearch = null;
        }
    }

    private void runSearch(@NonNull String query) {
        cancelInFlightSearch();
        int generation = ++searchGeneration;
        inFlight = PoiSearchDispatcher.submit(() -> {
            try {
                AppLogger.i(logTag, "Running search query=" + query);
                List<Poi> results = searchClient.search(query, 10);
                List<PoiSuggestion> suggestions = new ArrayList<>();
                for (Poi p : results) {
                    suggestions.add(new PoiSuggestion(p, false));
                }
                mainHandler.post(() -> {
                    if (generation != searchGeneration) {
                        AppLogger.d(logTag, "Discarding stale search result query=" + query);
                        return;
                    }
                    adapter.setItems(suggestions);
                    AppLogger.i(logTag, "Search finished query=" + query + " suggestions=" + suggestions.size());
                    if (!suggestions.isEmpty() && editText.hasFocus()) {
                        showPopupIfPossible("search-results");
                    }
                });
            } catch (IOException e) {
                AppLogger.e(logTag, "Search failed query=" + query, e);
                mainHandler.post(() -> {
                    if (generation == searchGeneration) {
                        adapter.setItems(new ArrayList<>());
                    }
                });
            }
        });
    }

    private void cancelInFlightSearch() {
        searchGeneration++;
        if (inFlight != null) {
            inFlight.cancel(true);
            inFlight = null;
            AppLogger.d(logTag, "Cancelled in-flight search");
        }
    }

    private void selectPoi(@NonNull Poi poi) {
        AppLogger.i(logTag, "Selected POI=" + poi.displayLabel());
        setPoi(poi);
    }

    private void dismissPopup() {
        if (popup.isShowing()) {
            popup.dismiss();
        }
    }

    private void showPopupIfPossible(@NonNull String reason) {
        if (!editText.hasFocus()) {
            return;
        }
        boolean attached = ViewCompat.isAttachedToWindow(editText);
        boolean hasWindowToken = editText.getWindowToken() != null;
        boolean viewVisible = editText.getVisibility() == View.VISIBLE;
        boolean windowVisible = editText.getWindowVisibility() == View.VISIBLE;
        if (!attached || !hasWindowToken || !viewVisible || !windowVisible) {
            AppLogger.d(logTag, "Skipping popup show reason=" + reason
                    + " attached=" + attached
                    + " windowToken=" + hasWindowToken
                    + " viewVisible=" + viewVisible
                    + " windowVisible=" + windowVisible);
            return;
        }
        try {
            popup.show();
            if (popup.getListView() != null) {
                popup.getListView().setItemsCanFocus(true);
            }
        } catch (WindowManager.BadTokenException | IllegalStateException e) {
            AppLogger.w(logTag, "Skipping popup show because anchor window is not ready reason=" + reason, e);
        }
    }

    @NonNull
    private static List<PoiSuggestion> singleSuggestion(@NonNull Poi poi, boolean deletable) {
        List<PoiSuggestion> items = new ArrayList<>();
        items.add(new PoiSuggestion(poi, deletable));
        return items;
    }
}
