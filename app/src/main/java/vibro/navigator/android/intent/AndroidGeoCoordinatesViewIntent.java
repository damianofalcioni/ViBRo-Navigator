package vibro.navigator.android.intent;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;

public final class AndroidGeoCoordinatesViewIntent {

    private AndroidGeoCoordinatesViewIntent() {
    }

    @NonNull
    public static Intent create(double lat, double lon, @Nullable String label) {
        if (!LatLon.isValidCoordinate(lat, lon)) {
            throw new IllegalArgumentException("Invalid geo coordinates lat=" + lat + " lon=" + lon);
        }
        return new Intent(Intent.ACTION_VIEW, Uri.parse(uriString(lat, lon, label)));
    }

    @NonNull
    public static Intent createChooser(
            @NonNull Context context,
            double lat,
            double lon,
            @Nullable String label
    ) {
        Intent chooser = Intent.createChooser(
                create(lat, lon, label),
                context.getString(R.string.action_open_map_position)
        );
        disableAutoLaunchSingleChoice(chooser);
        return chooser;
    }

    @NonNull
    static String uriString(double lat, double lon, @Nullable String label) {
        String coordinates = coordinates(lat, lon);
        String trimmedLabel = label == null ? "" : label.trim();
        if (trimmedLabel.isEmpty()) {
            return "geo:" + coordinates + "?q=" + coordinates;
        }
        return "geo:" + coordinates + "?q=" + coordinates + "(" + Uri.encode(trimmedLabel) + ")";
    }

    @NonNull
    private static String coordinates(double lat, double lon) {
        return String.format(Locale.US, "%.6f,%.6f", lat, lon);
    }

    private static void disableAutoLaunchSingleChoice(@NonNull Intent chooser) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            chooser.putExtra(Intent.EXTRA_AUTO_LAUNCH_SINGLE_CHOICE, false);
        }
    }
}
