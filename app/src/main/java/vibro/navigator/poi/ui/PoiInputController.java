package vibro.navigator.poi.ui;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.poi.CoordinateParser;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiHistoryStore;
import vibro.navigator.poi.search.PoiSearchClient;
import vibro.navigator.logging.AppLogger;

import java.util.ArrayList;
import java.util.List;

public final class PoiInputController {

    public interface Listener {
        void onPoiSelected(@NonNull Poi poi);
    }

    private final EditText editText;
    private final PoiHistoryStore history;
    private final Listener listener;

    private final PoiSuggestionAdapter adapter;
    private final PoiSuggestionPopupController popupController;
    private final PoiHistoryActionController historyActions;

    private final TaskScheduler mainThreadScheduler = AndroidTaskScheduler.main();
    private final String logTag;
    private final PoiSuggestionSearchController searchController;

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
        this.listener = listener;
        this.logTag = "PoiInputController#" + Integer.toHexString(System.identityHashCode(this));
        AppLogger.i(logTag, "Created controller");

        historyActions = new PoiHistoryActionController(
                editText,
                history,
                logTag,
                this::showHistory,
                this::updateSelectedPoiAfterRename
        );
        adapter = createSuggestionAdapter(context);
        popupController = new PoiSuggestionPopupController(
                context,
                editText,
                adapter,
                logTag,
                this::selectPoi
        );
        searchController = createSearchController(searchClient);
        attachInputHandlers();
    }

    @NonNull
    private PoiSuggestionAdapter createSuggestionAdapter(@NonNull Context context) {
        return new PoiSuggestionAdapter(context, new PoiSuggestionAdapter.Listener() {
            @Override
            public void onSuggestionClicked(@NonNull PoiSuggestion suggestion) {
                selectPoi(suggestion.poi);
            }

            @Override
            public void onEditClicked(@NonNull PoiSuggestion suggestion) {
                historyActions.promptRenameHistoryItem(suggestion);
            }

            @Override
            public void onDeleteClicked(@NonNull PoiSuggestion suggestion) {
                historyActions.deleteHistoryItem(suggestion);
            }
        });
    }

    @NonNull
    private PoiSuggestionSearchController createSearchController(@NonNull PoiSearchClient searchClient) {
        return new PoiSuggestionSearchController(
                mainThreadScheduler,
                history,
                searchClient,
                logTag,
                new PoiSuggestionSearchController.Presenter() {
                    @Override
                    public void showHistory() {
                        PoiInputController.this.showHistory();
                    }

                    @Override
                    public void showSuggestions(
                            @NonNull List<PoiSuggestion> suggestions,
                            @NonNull String popupReason
                    ) {
                        adapter.setItems(suggestions);
                        if (!suggestions.isEmpty() && editText.hasFocus()) {
                            popupController.showIfPossible(popupReason);
                        }
                    }

                    @Override
                    public void clearSuggestionsAndDismiss() {
                        adapter.setItems(new ArrayList<>());
                        popupController.dismiss();
                    }
                }
        );
    }

    private void attachInputHandlers() {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                AppLogger.d(logTag, "Input focused text=" + getRawText().trim());
                maybeShowHistory();
            } else {
                AppLogger.d(logTag, "Input lost focus");
                popupController.dismiss();
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
                    searchController.cancelPendingSearch();
                    searchController.cancelInFlightSearch();
                    popupController.dismiss();
                    suppressNextSearch = false;
                    programmaticChange = false;
                    return;
                }
                searchController.scheduleSearch(s.toString());
                programmaticChange = false;
            }
        });
    }

    public void dispose() {
        AppLogger.i(logTag, "Disposing controller");
        searchController.cancelPendingSearch();
        searchController.cancelInFlightSearch();
        popupController.dismiss();
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
        popupController.dismiss();
    }

    public void setPoi(@NonNull Poi poi) {
        String label = poi.displayLabel();
        AppLogger.d(logTag, "Programmatically setting POI=" + label);
        suppressNextSearch = true;
        programmaticChange = true;
        selectedPoi = poi;
        editText.setText(label);
        editText.setSelection(label.length());
        popupController.dismiss();
        PoiInputFocusController.clearFocusAndHideKeyboard(editText);
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
        popupController.dismiss();
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
            popupController.showIfPossible("history");
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

    private void updateSelectedPoiAfterRename(@NonNull Poi originalPoi, @NonNull Poi renamedPoi) {
        if (selectedPoi == null || !selectedPoi.stableKey().equals(originalPoi.stableKey())) {
            return;
        }
        selectedPoi = renamedPoi;
        if (getRawText().trim().equals(originalPoi.displayLabel())) {
            programmaticChange = true;
            editText.setText(renamedPoi.displayLabel());
            editText.setSelection(editText.getText().length());
        }
    }

    private void selectPoi(@NonNull Poi poi) {
        AppLogger.i(logTag, "Selected POI=" + poi.displayLabel());
        setPoi(poi);
    }

    int getSuggestionCountForTesting() {
        return adapter.getCount();
    }

    @Nullable
    String getSuggestionLabelForTesting(int position) {
        if (position < 0 || position >= adapter.getCount()) {
            return null;
        }
        PoiSuggestion suggestion = (PoiSuggestion) adapter.getItem(position);
        return suggestion.poi.displayLabel();
    }
}
