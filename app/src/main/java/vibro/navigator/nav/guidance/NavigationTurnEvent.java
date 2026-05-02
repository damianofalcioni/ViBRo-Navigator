package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;

import vibro.navigator.nav.route.VoiceHint;

public final class NavigationTurnEvent {
    public enum Type {
        PASSED,
        IMMINENT,
        INITIAL
    }

    @NonNull
    public final Type type;
    @NonNull
    public final VoiceHint hint;
    public final double distanceMeters;
    public final double timeSeconds;

    private NavigationTurnEvent(@NonNull Type type, @NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
        this.type = type;
        this.hint = hint;
        this.distanceMeters = distanceMeters;
        this.timeSeconds = timeSeconds;
    }

    @NonNull
    public static NavigationTurnEvent passed(@NonNull VoiceHint hint) {
        return new NavigationTurnEvent(Type.PASSED, hint, 0.0, 0.0);
    }

    @NonNull
    public static NavigationTurnEvent imminent(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
        return new NavigationTurnEvent(Type.IMMINENT, hint, distanceMeters, timeSeconds);
    }

    @NonNull
    public static NavigationTurnEvent initial(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
        return new NavigationTurnEvent(Type.INITIAL, hint, distanceMeters, timeSeconds);
    }
}
