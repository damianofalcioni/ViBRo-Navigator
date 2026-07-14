package vibro.navigator.poi.ui;

import android.content.Context;

import androidx.annotation.NonNull;

final class PoiSuggestionActionController implements PoiSuggestionAdapter.Listener {
    interface SelectionHandler {
        void onSuggestionSelected(@NonNull PoiSuggestion suggestion);
    }

    interface DeleteHandler {
        void onSuggestionDeleted(@NonNull PoiSuggestion suggestion);
    }

    @NonNull
    private final Context context;
    @NonNull
    private final PoiHistoryActionController historyActions;
    @NonNull
    private final SelectionHandler selectionHandler;
    @NonNull
    private final DeleteHandler deleteHandler;

    PoiSuggestionActionController(
            @NonNull Context context,
            @NonNull PoiHistoryActionController historyActions,
            @NonNull SelectionHandler selectionHandler,
            @NonNull DeleteHandler deleteHandler
    ) {
        this.context = context;
        this.historyActions = historyActions;
        this.selectionHandler = selectionHandler;
        this.deleteHandler = deleteHandler;
    }

    @Override
    public void onSuggestionClicked(@NonNull PoiSuggestion suggestion) {
        selectionHandler.onSuggestionSelected(suggestion);
    }

    @Override
    public void onInfoClicked(@NonNull PoiSuggestion suggestion) {
        PoiDetailsDialog.show(context, suggestion);
    }

    @Override
    public void onEditClicked(@NonNull PoiSuggestion suggestion) {
        historyActions.promptRenameHistoryItem(suggestion);
    }

    @Override
    public void onDeleteClicked(@NonNull PoiSuggestion suggestion) {
        deleteHandler.onSuggestionDeleted(suggestion);
    }
}
