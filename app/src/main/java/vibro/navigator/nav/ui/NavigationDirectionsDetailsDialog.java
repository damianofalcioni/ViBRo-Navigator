package vibro.navigator.nav.ui;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import vibro.navigator.R;

final class NavigationDirectionsDetailsDialog {
    interface DirectionDetailsProvider {
        @NonNull
        List<String> buildCurrentDirectionDetails();
    }

    @NonNull
    private final Activity activity;
    @NonNull
    private final NavigationDetailsDialog detailsDialog;

    NavigationDirectionsDetailsDialog(@NonNull Activity activity) {
        this.activity = activity;
        detailsDialog = new NavigationDetailsDialog(activity, R.string.title_nav_directions_details);
    }

    void show(@Nullable DirectionDetailsProvider detailsProvider) {
        detailsDialog.show();
        update(detailsProvider);
    }

    void update(@Nullable DirectionDetailsProvider detailsProvider) {
        if (!detailsDialog.isShowing()) {
            return;
        }
        detailsDialog.update(formatDetails(detailsProvider));
    }

    void dismiss() {
        detailsDialog.dismiss();
    }

    @NonNull
    private CharSequence formatDetails(@Nullable DirectionDetailsProvider detailsProvider) {
        List<String> lines = detailsProvider == null ? Collections.emptyList()
                : detailsProvider.buildCurrentDirectionDetails();
        if (lines.isEmpty()) {
            return activity.getString(R.string.nav_directions_unavailable);
        }
        return formatLines(lines);
    }

    @NonNull
    private CharSequence formatLines(@NonNull List<String> lines) {
        StringBuilder details = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                details.append('\n');
            }
            details.append(activity.getString(R.string.format_nav_direction_details_row, i + 1, lines.get(i)));
        }
        return details;
    }
}
