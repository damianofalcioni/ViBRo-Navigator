package vibro.navigator.poi.search;

import androidx.annotation.Nullable;

import java.io.IOException;

public interface PoiReverseGeocodingClient {
    @Nullable
    String reverseGeocode(double lat, double lon) throws IOException;
}
