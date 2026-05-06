package vibro.navigator.poi.search;

import androidx.annotation.NonNull;

import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.logging.AppLogger;

public final class PoiSearchClients {
    private static final String TAG = "PoiSearchClients";

    private PoiSearchClients() {
    }

    @NonNull
    public static PoiSearchClient createDefault() {
        PoiSearchClient googleClient = DistributionServices.createGooglePoiSearchClient();
        if (googleClient != null) {
            AppLogger.i(TAG, "Using Google POI search because API key is configured");
            return googleClient;
        }
        AppLogger.i(TAG, "Using OsmNominatimClient because no Google API key is configured");
        return new OsmNominatimClient();
    }
}
