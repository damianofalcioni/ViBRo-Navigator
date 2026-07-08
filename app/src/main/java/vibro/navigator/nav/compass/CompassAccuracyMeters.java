package vibro.navigator.nav.compass;

import vibro.navigator.nav.format.NavigationMeasurementFormatter;

final class CompassAccuracyMeters {
    private CompassAccuracyMeters() {
    }

    static float sanitize(float accuracyMeters) {
        return NavigationMeasurementFormatter.isDisplayableAccuracyMeters(accuracyMeters) ? accuracyMeters : 0f;
    }
}
