package vibro.navigator.poi.ui;

import android.widget.EditText;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

final class PoiInputSuggestionPresenter implements PoiSuggestionSearchController.Presenter {
    @NonNull
    private final EditText editText;
    @NonNull
    private final PoiSuggestionAdapter adapter;
    @NonNull
    private final PoiSuggestionPopupController popupController;
    @NonNull
    private final Runnable historyPresenter;

    PoiInputSuggestionPresenter(
            @NonNull EditText editText,
            @NonNull PoiSuggestionAdapter adapter,
            @NonNull PoiSuggestionPopupController popupController,
            @NonNull Runnable historyPresenter
    ) {
        this.editText = editText;
        this.adapter = adapter;
        this.popupController = popupController;
        this.historyPresenter = historyPresenter;
    }

    @Override
    public void showHistory() {
        historyPresenter.run();
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
