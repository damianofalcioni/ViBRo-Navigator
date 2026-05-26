package vibro.navigator.poi.search;

import android.content.Context;

import androidx.annotation.NonNull;

import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.poi.Poi;

import java.io.IOException;
import java.util.List;

public final class PoiSearchClients {
    private static final String TAG = "PoiSearchClients";

    private PoiSearchClients() {
    }

    @NonNull
    public static PoiSearchClient createDefault(@NonNull Context context) {
        return new DefaultPoiSearchClient(context);
    }

    private static final class DefaultPoiSearchClient implements PoiSearchClient {
        private final Context context;
        private final PoiSearchClient osmClient = new OsmNominatimClient();

        private DefaultPoiSearchClient(@NonNull Context context) {
            Context appContext = context.getApplicationContext();
            this.context = appContext == null ? context : appContext;
        }

        @NonNull
        @Override
        public List<Poi> search(@NonNull String query, int limit) throws IOException {
            PoiSearchClient googleClient = DistributionServices.createGooglePoiSearchClient(context);
            if (googleClient != null) {
                AppLogger.i(TAG, "Using Google POI search because it is enabled");
                return googleClient.search(query, limit);
            }
            AppLogger.i(TAG, "Using OsmNominatimClient because Google POI search is unavailable");
            return osmClient.search(query, limit);
        }
    }
}
