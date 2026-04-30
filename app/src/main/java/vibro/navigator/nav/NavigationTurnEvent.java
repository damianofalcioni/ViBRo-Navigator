package vibro.navigator.nav;

import androidx.annotation.NonNull;

import vibro.navigator.nav.route.VoiceHint;

final class NavigationTurnEvent {
    enum Type {
        PASSED,
        IMMINENT,
        INITIAL
    }

    @NonNull
    final Type type;
    @NonNull
    final VoiceHint hint;
    final double distanceMeters;
    final double timeSeconds;

    private NavigationTurnEvent(@NonNull Type type, @NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
        this.type = type;
        this.hint = hint;
        this.distanceMeters = distanceMeters;
        this.timeSeconds = timeSeconds;
    }

    @NonNull
    static NavigationTurnEvent passed(@NonNull VoiceHint hint) {
        return new NavigationTurnEvent(Type.PASSED, hint, 0.0, 0.0);
    }

    @NonNull
    static NavigationTurnEvent imminent(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
        return new NavigationTurnEvent(Type.IMMINENT, hint, distanceMeters, timeSeconds);
    }

    @NonNull
    static NavigationTurnEvent initial(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
        return new NavigationTurnEvent(Type.INITIAL, hint, distanceMeters, timeSeconds);
    }
}
