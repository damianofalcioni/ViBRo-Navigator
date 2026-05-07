package vibro.navigator.nav.compass;

public final class CompassOrientationCue {
    public final float targetHeadingDegrees;

    public CompassOrientationCue(float targetHeadingDegrees) {
        this.targetHeadingDegrees = normalizeHeading(targetHeadingDegrees);
    }

    private static float normalizeHeading(float headingDegrees) {
        float normalized = headingDegrees % 360f;
        if (normalized < 0f) {
            normalized += 360f;
        }
        return normalized;
    }
}
