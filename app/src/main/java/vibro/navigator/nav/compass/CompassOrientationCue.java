package vibro.navigator.nav.compass;

public final class CompassOrientationCue {
    public final float targetHeadingDegrees;

    public CompassOrientationCue(float targetHeadingDegrees) {
        this.targetHeadingDegrees = normalizeHeading(targetHeadingDegrees);
    }

    public static CompassOrientationCue fromRelativeTurn(float currentHeadingDegrees, float turnDegrees) {
        return new CompassOrientationCue(currentHeadingDegrees + normalizeSignedDegrees(turnDegrees));
    }

    private static float normalizeSignedDegrees(float degrees) {
        float normalized = (degrees + 540f) % 360f - 180f;
        return normalized == -180f ? 180f : normalized;
    }

    private static float normalizeHeading(float headingDegrees) {
        float normalized = headingDegrees % 360f;
        if (normalized < 0f) {
            normalized += 360f;
        }
        return normalized;
    }
}
