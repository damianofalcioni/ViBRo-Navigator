package vibro.navigator.poi.ui;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import vibro.navigator.R;
import vibro.navigator.android.intent.AndroidGoogleMapsSearchLauncher;

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
    @NonNull
    private final Runnable externalSearchStarted;

    PoiSuggestionActionController(
            @NonNull Context context,
            @NonNull PoiHistoryActionController historyActions,
            @NonNull SelectionHandler selectionHandler,
            @NonNull DeleteHandler deleteHandler,
            @NonNull Runnable externalSearchStarted
    ) {
        this.context = context;
        this.historyActions = historyActions;
        this.selectionHandler = selectionHandler;
        this.deleteHandler = deleteHandler;
        this.externalSearchStarted = externalSearchStarted;
    }

    @Override
    public void onSuggestionClicked(@NonNull PoiSuggestion suggestion) {
        if (suggestion.isExternalMapSearch()) {
            openExternalMapSearch(suggestion);
            return;
        }
        selectionHandler.onSuggestionSelected(suggestion);
    }

    @Override
    public void onInfoClicked(@NonNull PoiSuggestion suggestion) {
        PoiDetailsDialog.show(context, suggestion);
    }

    @Override
    public void onEditClicked(@NonNull PoiSuggestion suggestion) {
        if (suggestion.isExternalMapSearch()) {
            return;
        }
        historyActions.promptRenameHistoryItem(suggestion);
    }

    @Override
    public void onDeleteClicked(@NonNull PoiSuggestion suggestion) {
        if (suggestion.isExternalMapSearch()) {
            return;
        }
        deleteHandler.onSuggestionDeleted(suggestion);
    }

    private void openExternalMapSearch(@NonNull PoiSuggestion suggestion) {
        externalSearchStarted.run();
        if (!AndroidGoogleMapsSearchLauncher.open(context, suggestion.externalMapSearchQuery())) {
            Toast.makeText(context, R.string.msg_google_maps_search_open_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
