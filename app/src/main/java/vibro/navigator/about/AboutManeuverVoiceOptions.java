package vibro.navigator.about;

import android.app.Activity;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.nav.voice.NavigationVoiceOption;
import vibro.navigator.settings.AppSettings;

final class AboutManeuverVoiceOptions {
    private AboutManeuverVoiceOptions() {
    }

    @NonNull
    static List<NavigationVoiceOption> withBaseOptions(
            @NonNull Activity activity,
            @NonNull List<NavigationVoiceOption> availableVoiceOptions
    ) {
        List<NavigationVoiceOption> options = new ArrayList<>();
        options.add(new NavigationVoiceOption(
                AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT,
                activity.getString(R.string.label_maneuver_voice_system_default)
        ));
        options.addAll(availableVoiceOptions);
        return options;
    }

    static boolean shouldPersistSelectedVoice(
            boolean voiceListLoaded,
            @NonNull String savedVoiceName,
            @NonNull String selectedVoiceName
    ) {
        if (selectedVoiceName.equals(savedVoiceName)) {
            return false;
        }
        return voiceListLoaded
                || isBaseVoiceName(savedVoiceName)
                || !isBaseVoiceName(selectedVoiceName);
    }

    private static boolean isBaseVoiceName(@NonNull String voiceName) {
        return AppSettings.MANEUVER_VOICE_DISABLED.equals(voiceName)
                || AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT.equals(voiceName);
    }

    @Nullable
    static NavigationVoiceOption selectedVoiceOption(@Nullable Spinner spinner) {
        if (spinner == null) {
            return null;
        }
        Object selected = spinner.getSelectedItem();
        if (selected instanceof NavigationVoiceOption) {
            return (NavigationVoiceOption) selected;
        }
        return null;
    }
}
