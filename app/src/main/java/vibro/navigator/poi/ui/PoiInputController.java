package vibro.navigator.poi.ui;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.poi.CoordinateParser;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiCoordinateLabel;
import vibro.navigator.poi.PoiHistoryStore;
import vibro.navigator.poi.search.PoiSearchClient;
import vibro.navigator.logging.AppLogger;

public final class PoiInputController {

    public interface Listener {
        void onPoiSelected(@NonNull Poi poi);
    }

    private final EditText editText;
    private final Context context;
    private final PoiHistoryStore history;
    private final Listener listener;

    private final PoiSuggestionAdapter adapter;
    private final PoiSuggestionPopupController popupController;
    private final PoiHistoryActionController historyActions;
    private final PoiSelectedHistorySuggestionController selectedHistorySuggestions;

    private final TaskScheduler mainThreadScheduler = AndroidTaskScheduler.main();
    private final String logTag;
    private final PoiSuggestionSearchController searchController;

    private Poi selectedPoi;
    private boolean programmaticChange;
    private boolean suppressNextSearch;
    private boolean disposed;

    public PoiInputController(
            @NonNull Context context,
            @NonNull EditText editText,
            @NonNull PoiHistoryStore history,
            @NonNull PoiSearchClient searchClient,
            @NonNull Listener listener
    ) {
        this.context = context;
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
        PoiSuggestionActionController suggestionActions = createSuggestionActions(context);
        adapter = new PoiSuggestionAdapter(context, suggestionActions);
        popupController = new PoiSuggestionPopupController(
                context,
                editText,
                adapter,
                logTag,
                suggestionActions::onSuggestionClicked
        );
        selectedHistorySuggestions = new PoiSelectedHistorySuggestionController(
                editText,
                history,
                adapter,
                popupController,
                logTag
        );
        searchController = createSearchController(searchClient);
        attachInputHandlers();
    }

    @NonNull
    private PoiSuggestionActionController createSuggestionActions(@NonNull Context context) {
        return new PoiSuggestionActionController(
                context,
                historyActions,
                suggestion -> selectPoi(suggestion.selectedPoi(context)),
                this::deleteHistorySuggestion,
                this::dismissSuggestionsForExternalSearch
        );
    }

    @NonNull
    private PoiSuggestionSearchController createSearchController(@NonNull PoiSearchClient searchClient) {
        return new PoiSuggestionSearchController(
                mainThreadScheduler,
                history,
                searchClient,
                logTag,
                new PoiInputSuggestionPresenter(editText, adapter, popupController, this::showHistory)
        );
    }

    private void attachInputHandlers() {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                AppLogger.d(logTag, "Input focused text=" + getRawText().trim());
                maybeShowEmptyHistory();
            } else {
                AppLogger.d(logTag, "Input lost focus");
                popupController.dismiss();
            }
        });
        editText.setOnClickListener(v -> {
            AppLogger.d(logTag, "Input clicked");
            maybeShowSelectedOrEmptyHistory();
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
        disposed = true;
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

    public boolean replaceSelectedPoiNameIfSameCoordinates(
            @NonNull Poi expectedPoi,
            @NonNull String name
    ) {
        if (disposed
                || selectedPoi == null
                || !selectedPoi.stableKey().equals(expectedPoi.stableKey())
                || !PoiCoordinateLabel.isCoordinateLabel(selectedPoi)) {
            return false;
        }
        Poi renamedPoi = new Poi(name, expectedPoi.lat, expectedPoi.lon);
        String label = renamedPoi.displayLabel();
        AppLogger.d(logTag, "Replacing selected POI label=" + label);
        suppressNextSearch = true;
        programmaticChange = true;
        selectedPoi = renamedPoi;
        editText.setText(label);
        editText.setSelection(label.length());
        popupController.dismiss();
        history.addOrPromote(renamedPoi);
        listener.onPoiSelected(renamedPoi);
        return true;
    }

    @NonNull
    public EditText getEditText() {
        return editText;
    }

    public void showHistory() {
        selectedHistorySuggestions.showHistory();
    }

    private void maybeShowSelectedOrEmptyHistory() {
        selectedHistorySuggestions.showOrElse(selectedPoi, getRawText(), this::maybeShowEmptyHistory);
    }

    private void maybeShowEmptyHistory() {
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

    private void deleteHistorySuggestion(@NonNull PoiSuggestion suggestion) {
        selectedHistorySuggestions.delete(
                selectedPoi,
                getRawText(),
                suggestion,
                this::clearSelectedPoiTextAfterDelete,
                this::showHistory
        );
    }

    private void clearSelectedPoiTextAfterDelete() {
        AppLogger.d(logTag, "Clearing text after selected history item deletion");
        selectedPoi = null;
        programmaticChange = true;
        editText.setText("");
        editText.setSelection(0);
    }

    private void selectPoi(@NonNull Poi poi) {
        AppLogger.i(logTag, "Selected POI=" + poi.displayLabel());
        setPoi(poi);
    }

    private void dismissSuggestionsForExternalSearch() {
        popupController.dismiss();
    }

    void deleteSuggestionForTesting(int position) {
        deleteHistorySuggestion((PoiSuggestion) adapter.getItem(position));
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
        return suggestion.displayLabel(context);
    }

}
