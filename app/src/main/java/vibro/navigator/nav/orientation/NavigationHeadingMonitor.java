package vibro.navigator.nav.orientation;

import androidx.annotation.Nullable;

public interface NavigationHeadingMonitor {
    boolean start();

    void stop();

    @Nullable
    GeomagneticOrientationMonitor.Sample getLatestSample();
}
