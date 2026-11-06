package vibro.navigator.nav.model;

import androidx.annotation.NonNull;

import vibro.navigator.nav.route.VoiceHint;

/** Structured guidance shared with external displays; never recovered from localized text. */
public final class NavManeuver {
    @NonNull public final VoiceHint hint;
    @NonNull public final String instruction;
    public final double distanceMeters;
    public final double timeSeconds;

    public NavManeuver(@NonNull VoiceHint hint, @NonNull String instruction,
            double distanceMeters, double timeSeconds) {
        this.hint = hint;
        this.instruction = instruction;
        this.distanceMeters = Math.max(0, distanceMeters);
        this.timeSeconds = timeSeconds;
    }
}
