package vibro.navigator.poi.search;

import androidx.annotation.NonNull;

import vibro.navigator.poi.Poi;

import java.io.IOException;
import java.util.List;

public interface PoiSearchClient {
    @NonNull
    List<Poi> search(@NonNull String query, int limit) throws IOException;
}
