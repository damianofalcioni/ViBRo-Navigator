package vibro.navigator.nav.compass;

import androidx.annotation.Nullable;

final class CompassOrientationCueResolver {
    private CompassOrientationCueResolver() {
    }

    @Nullable
    static CompassOrientationCue resolve(
            @Nullable CompassOrientationCue orientationCue,
            @Nullable Integer turnManeuverDegrees,
            float currentHeadingDegrees
    ) {
        if (turnManeuverDegrees != null) {
            return CompassOrientationCue.fromRelativeTurn(currentHeadingDegrees, turnManeuverDegrees);
        }
        return orientationCue;
    }
}
