package vibro.navigator.nav.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface NavigationLocationListener {
    void onLocationChanged(@NonNull NavigationLocation location);

    void onProviderEnabled(@NonNull String provider);

    void onProviderDisabled(@NonNull String provider);

    void onProviderStatusChanged(@Nullable String provider, int status);
}
