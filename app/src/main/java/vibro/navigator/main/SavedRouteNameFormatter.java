package vibro.navigator.main;

import android.content.Context;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import vibro.navigator.R;

final class SavedRouteNameFormatter {
    private static final String TIMESTAMP_PATTERN = "yyyyMMdd-HHmmss";

    private SavedRouteNameFormatter() {
    }

    @NonNull
    static String defaultName(@NonNull Context context, long timestampMillis) {
        String timestamp = new SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US)
                .format(new Date(timestampMillis));
        return context.getString(R.string.format_default_saved_route_name, timestamp);
    }
}
