package vibro.navigator.nav.location;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface FusedLocationDiagnosticClient {
    boolean isAvailable();

    void requestLastKnownLocation(@NonNull Callback callback);

    interface Callback {
        void onLocation(@Nullable Location location);

        void onFailure(@NonNull String reason);
    }
}
