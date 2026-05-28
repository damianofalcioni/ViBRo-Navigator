package vibro.navigator.android.location;

import androidx.annotation.NonNull;

import vibro.navigator.nav.location.FusedLocationDiagnosticClient;

public final class NoOpFusedLocationDiagnosticClient implements FusedLocationDiagnosticClient {
    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void requestLastKnownLocation(@NonNull Callback callback) {
        callback.onFailure("unavailable");
    }
}
