package vibro.navigator.auto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;

import java.util.Collections;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.nav.service.NavigationServiceBinder;

final class ViBRoAutoDirectionDetailsText {
    private ViBRoAutoDirectionDetailsText() {
    }

    @NonNull
    static String build(@NonNull CarContext carContext, @Nullable NavigationServiceBinder navBinder) {
        List<String> lines = navBinder == null ? Collections.emptyList() : navBinder.buildCurrentDirectionDetails();
        if (lines.isEmpty()) {
            return carContext.getString(R.string.nav_directions_unavailable);
        }
        return formatLines(carContext, lines);
    }

    @NonNull
    private static String formatLines(@NonNull CarContext carContext, @NonNull List<String> lines) {
        StringBuilder details = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                details.append('\n');
            }
            details.append(carContext.getString(R.string.format_nav_direction_details_row, i + 1, lines.get(i)));
        }
        return details.toString();
    }
}
