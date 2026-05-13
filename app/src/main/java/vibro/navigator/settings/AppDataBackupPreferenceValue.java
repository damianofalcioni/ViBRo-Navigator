package vibro.navigator.settings;

import androidx.annotation.NonNull;

final class AppDataBackupPreferenceValue {

    final String key;
    final Object value;

    AppDataBackupPreferenceValue(@NonNull String key, @NonNull Object value) {
        this.key = key;
        this.value = value;
    }
}
