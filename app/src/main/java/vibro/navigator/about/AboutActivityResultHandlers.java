package vibro.navigator.about;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class AboutActivityResultHandlers {
    private AboutActivityResultHandlers() {
    }

    static void handleActivityResult(
            @NonNull AboutSettingsSwitches settingsSwitches,
            @Nullable AboutDiagnosticSection diagnosticSection,
            @NonNull AboutDatabaseBackupActions databaseBackupActions,
            int requestCode,
            int resultCode,
            @Nullable Intent data,
            @NonNull Runnable afterDiagnosticChanged
    ) {
        if (settingsSwitches.onActivityResult(requestCode, resultCode, data)) {
            afterDiagnosticChanged.run();
            return;
        }
        if (diagnosticSection != null && diagnosticSection.onActivityResult(requestCode, resultCode, data)) {
            afterDiagnosticChanged.run();
            return;
        }
        databaseBackupActions.handleActivityResult(requestCode, resultCode, data);
    }

    static void handlePermissionResult(
            @NonNull AboutSettingsSwitches settingsSwitches,
            @Nullable AboutDiagnosticSection diagnosticSection,
            int requestCode,
            @NonNull int[] grantResults,
            @NonNull Runnable afterDiagnosticChanged
    ) {
        settingsSwitches.onRequestPermissionsResult(requestCode, grantResults);
        if (diagnosticSection != null && diagnosticSection.onRequestPermissionsResult(requestCode, grantResults)) {
            afterDiagnosticChanged.run();
        }
    }
}
