package vibro.navigator.poi.ui;

import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiHistoryStore;

import java.util.ArrayList;
import java.util.List;

final class PoiSelectedHistorySuggestionController {

    private static final String POPUP_REASON = "selected-history";

    @NonNull
    private final EditText editText;
    @NonNull
    private final PoiHistoryStore history;
    @NonNull
    private final PoiSuggestionAdapter adapter;
    @NonNull
    private final PoiSuggestionPopupController popupController;
    @NonNull
    private final String logTag;

    PoiSelectedHistorySuggestionController(
            @NonNull EditText editText,
            @NonNull PoiHistoryStore history,
            @NonNull PoiSuggestionAdapter adapter,
            @NonNull PoiSuggestionPopupController popupController,
            @NonNull String logTag
    ) {
        this.editText = editText;
        this.history = history;
        this.adapter = adapter;
        this.popupController = popupController;
        this.logTag = logTag;
    }

    boolean show(@Nullable Poi selectedPoi, @NonNull String rawText) {
        Poi historyPoi = selectedHistoryPoi(selectedPoi, rawText);
        if (historyPoi == null) {
            return false;
        }

        List<PoiSuggestion> items = new ArrayList<>();
        items.add(new PoiSuggestion(historyPoi, true));
        adapter.setItems(items);
        AppLogger.d(logTag, "Showing selected history item=" + historyPoi.displayLabel());
        if (editText.hasFocus()) {
            popupController.showIfPossible(POPUP_REASON);
        }
        return true;
    }

    void showOrElse(
            @Nullable Poi selectedPoi,
            @NonNull String rawText,
            @NonNull Runnable fallback
    ) {
        if (!show(selectedPoi, rawText)) {
            fallback.run();
        }
    }

    void delete(
            @Nullable Poi selectedPoi,
            @NonNull String rawText,
            @NonNull PoiSuggestion suggestion,
            @NonNull Runnable selectedDeleted,
            @NonNull Runnable historyChanged
    ) {
        AppLogger.i(logTag, "Deleting history item=" + suggestion.poi.displayLabel());
        boolean clearsSelectedPoi = isCurrentSelectedPoi(selectedPoi, suggestion.poi, rawText);
        history.remove(suggestion.poi);
        if (clearsSelectedPoi) {
            selectedDeleted.run();
        } else {
            historyChanged.run();
        }
    }

    private boolean isCurrentSelectedPoi(
            @Nullable Poi selectedPoi,
            @NonNull Poi deletedPoi,
            @NonNull String rawText
    ) {
        return selectedPoi != null
                && selectedPoi.stableKey().equals(deletedPoi.stableKey())
                && rawText.trim().equals(selectedPoi.displayLabel());
    }

    @Nullable
    private Poi selectedHistoryPoi(@Nullable Poi selectedPoi, @NonNull String rawText) {
        if (selectedPoi == null || !rawText.trim().equals(selectedPoi.displayLabel())) {
            return null;
        }
        return findHistoryPoi(selectedPoi);
    }

    @Nullable
    private Poi findHistoryPoi(@NonNull Poi poi) {
        for (Poi historyPoi : history.list()) {
            if (historyPoi.stableKey().equals(poi.stableKey())) {
                return historyPoi;
            }
        }
        return null;
    }
}
