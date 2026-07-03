package vibro.navigator.poi.search;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.logging.AppLogger;

public final class PoiReverseGeocodingClients {
    private static final String TAG = "PoiReverseClients";

    private PoiReverseGeocodingClients() {
    }

    @NonNull
    public static PoiReverseGeocodingClient createDefault(@NonNull Context context) {
        return new DefaultPoiReverseGeocodingClient(context);
    }

    private static final class DefaultPoiReverseGeocodingClient implements PoiReverseGeocodingClient {
        @NonNull
        private final Context context;
        @NonNull
        private final PoiReverseGeocodingClient osmClient = new OsmNominatimClient();

        private DefaultPoiReverseGeocodingClient(@NonNull Context context) {
            Context appContext = context.getApplicationContext();
            this.context = appContext == null ? context : appContext;
        }

        @Nullable
        @Override
        public String reverseGeocode(double lat, double lon) throws IOException {
            PoiReverseGeocodingClient googleClient = DistributionServices.createGooglePoiReverseGeocodingClient(context);
            if (googleClient != null) {
                AppLogger.i(TAG, "Using Google reverse geocoding because it is enabled");
                return googleClient.reverseGeocode(lat, lon);
            }
            AppLogger.i(TAG, "Using OpenStreetMap reverse geocoding");
            return osmClient.reverseGeocode(lat, lon);
        }
    }
}
