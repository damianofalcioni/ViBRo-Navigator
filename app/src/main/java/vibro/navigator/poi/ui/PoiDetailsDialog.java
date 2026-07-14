package vibro.navigator.poi.ui;

import android.app.AlertDialog;
import android.content.Context;

import androidx.annotation.NonNull;

import vibro.navigator.poi.PoiDetails;

final class PoiDetailsDialog {
    private PoiDetailsDialog() {
    }

    static void show(@NonNull Context context, @NonNull PoiSuggestion suggestion) {
        PoiDetails details = suggestion.poi.details();
        if (details == null) {
            return;
        }
        new AlertDialog.Builder(context)
                .setTitle(suggestion.displayLabel(context))
                .setMessage(PoiDetailsFormatter.format(context, details))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
