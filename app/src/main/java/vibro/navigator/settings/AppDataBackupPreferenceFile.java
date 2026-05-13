package vibro.navigator.settings;

import androidx.annotation.NonNull;

import java.util.List;

final class AppDataBackupPreferenceFile {

    final String prefsName;
    final List<AppDataBackupPreferenceValue> values;

    AppDataBackupPreferenceFile(@NonNull String prefsName, @NonNull List<AppDataBackupPreferenceValue> values) {
        this.prefsName = prefsName;
        this.values = values;
    }
}
