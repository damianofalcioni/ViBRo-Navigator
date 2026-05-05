package vibro.navigator.poi.search;

import androidx.annotation.NonNull;

import vibro.navigator.BuildConfig;
import vibro.navigator.logging.AppLogger;

public final class PoiSearchClients {
    private static final String TAG = "PoiSearchClients";

    private PoiSearchClients() {
    }

    @NonNull
    public static PoiSearchClient createDefault() {
        String key = BuildConfig.GOOGLE_MAPS_API_KEY;
        if (key != null && !key.trim().isEmpty()) {
            AppLogger.i(TAG, "Using GoogleGeocodeClient because API key is configured");
            return new GoogleGeocodeClient(key.trim());
        }
        AppLogger.i(TAG, "Using OsmNominatimClient because no Google API key is configured");
        return new OsmNominatimClient();
    }
}
