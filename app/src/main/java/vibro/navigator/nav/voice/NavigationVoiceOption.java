package vibro.navigator.nav.voice;

import androidx.annotation.NonNull;

public final class NavigationVoiceOption {
    @NonNull
    public final String voiceName;
    @NonNull
    public final String label;

    public NavigationVoiceOption(@NonNull String voiceName, @NonNull String label) {
        this.voiceName = voiceName;
        this.label = label;
    }

    @NonNull
    @Override
    public String toString() {
        return label;
    }
}
