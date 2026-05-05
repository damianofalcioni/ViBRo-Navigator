package vibro.navigator.poi.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import vibro.navigator.R;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiHistoryStore;
import vibro.navigator.logging.AppLogger;

final class PoiHistoryActionController {
    interface SelectionUpdater {
        void onHistoryItemRenamed(@NonNull Poi originalPoi, @NonNull Poi renamedPoi);
    }

    @NonNull
    private final EditText anchorEditText;
    @NonNull
    private final PoiHistoryStore history;
    @NonNull
    private final String logTag;
    @NonNull
    private final Runnable historyPresenter;
    @NonNull
    private final SelectionUpdater selectionUpdater;

    PoiHistoryActionController(
            @NonNull EditText anchorEditText,
            @NonNull PoiHistoryStore history,
            @NonNull String logTag,
            @NonNull Runnable historyPresenter,
            @NonNull SelectionUpdater selectionUpdater
    ) {
        this.anchorEditText = anchorEditText;
        this.history = history;
        this.logTag = logTag;
        this.historyPresenter = historyPresenter;
        this.selectionUpdater = selectionUpdater;
    }

    void deleteHistoryItem(@NonNull PoiSuggestion suggestion) {
        AppLogger.i(logTag, "Deleting history item=" + suggestion.poi.displayLabel());
        history.remove(suggestion.poi);
        historyPresenter.run();
    }

    void promptRenameHistoryItem(@NonNull PoiSuggestion suggestion) {
        Context context = anchorEditText.getContext();
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
            saveHistoryRename(dialog, input, poi, updatedName);
        }));
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        input.requestFocus();
    }

    private void saveHistoryRename(
            @NonNull AlertDialog dialog,
            @NonNull EditText input,
            @NonNull Poi poi,
            @NonNull String updatedName
    ) {
        if (updatedName.isEmpty()) {
            input.setError(anchorEditText.getContext().getString(R.string.msg_invalid_destination_name));
            return;
        }
        if (!history.rename(poi, updatedName)) {
            dialog.dismiss();
            return;
        }

        selectionUpdater.onHistoryItemRenamed(poi, new Poi(updatedName, poi.lat, poi.lon));
        AppLogger.i(logTag, "Renamed history item key=" + poi.stableKey() + " newName=" + updatedName);
        historyPresenter.run();
        dialog.dismiss();
    }
}
