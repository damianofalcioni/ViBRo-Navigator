package vibro.navigator.android.location;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationListener;

final class AndroidLocationListenerAdapter implements LocationListener {
    @NonNull
    private final NavigationLocationListener listener;

    AndroidLocationListenerAdapter(@NonNull NavigationLocationListener listener) {
        this.listener = listener;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        NavigationLocation navigationLocation = AndroidLocationConverter.toNavigationLocation(location);
        if (navigationLocation != null) {
            listener.onLocationChanged(navigationLocation);
        }
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        listener.onProviderEnabled(provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        listener.onProviderDisabled(provider);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onStatusChanged(@Nullable String provider, int status, @Nullable Bundle extras) {
        listener.onProviderStatusChanged(provider, status);
    }
}
