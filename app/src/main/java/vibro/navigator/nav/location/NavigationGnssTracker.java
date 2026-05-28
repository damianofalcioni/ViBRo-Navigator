package vibro.navigator.nav.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public interface NavigationGnssTracker {
    @Nullable
    Integer getFixedSatelliteCount();

    void updateForRequestedProviders(@NonNull List<String> requestedProviders);

    void reset();
}
