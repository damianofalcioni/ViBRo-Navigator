package vibro.navigator.nav.format;

import androidx.annotation.NonNull;

import vibro.navigator.nav.route.VoiceHint;

final class NavUpcomingHint {
    @NonNull
    public final VoiceHint hint;
    public final double distanceMeters;
    public final double timeSeconds;

    NavUpcomingHint(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
        this.hint = hint;
        this.distanceMeters = distanceMeters;
        this.timeSeconds = timeSeconds;
    }
}
