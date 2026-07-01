package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;

import vibro.navigator.nav.policy.NavigationSpeedBucket;

final class CompassMovingScaleHorizon {
    static final NavigationSpeedBucket DEFAULT_SPEED_BUCKET = NavigationSpeedBucket.HIGH;
    private static final float LOW_SPEED_SECONDS = 30f;
    private static final float MEDIUM_SPEED_SECONDS = 45f;
    private static final float HIGH_SPEED_SECONDS = 60f;

    private CompassMovingScaleHorizon() {
    }

    static float secondsFor(@NonNull NavigationSpeedBucket bucket) {
        switch (bucket) {
            case LOW:
                return LOW_SPEED_SECONDS;
            case MEDIUM:
                return MEDIUM_SPEED_SECONDS;
            case HIGH:
                return HIGH_SPEED_SECONDS;
            default:
                return HIGH_SPEED_SECONDS;
        }
    }
}
