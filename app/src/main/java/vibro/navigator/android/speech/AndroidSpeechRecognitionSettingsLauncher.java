package vibro.navigator.android.speech;

import vibro.navigator.R;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;

import vibro.navigator.logging.AppLogger;

public final class AndroidSpeechRecognitionSettingsLauncher {
    public static final String ACTION_VOICE_INPUT_SETTINGS = Settings.ACTION_VOICE_INPUT_SETTINGS;
    private static final String TAG = "SpeechRecognitionSettings";

    private AndroidSpeechRecognitionSettingsLauncher() {
    }

    public static void open(@NonNull Activity activity) {
        if (tryStart(activity, new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))) {
            return;
        }
        if (tryStart(activity, new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))) {
            return;
        }
        Toast.makeText(activity, R.string.msg_open_settings_failed, Toast.LENGTH_SHORT).show();
    }

    private static boolean tryStart(@NonNull Activity activity, @NonNull Intent intent) {
        try {
            activity.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            AppLogger.w(TAG, "Could not open speech recognition settings action=" + intent.getAction(), e);
            return false;
        }
    }
}
