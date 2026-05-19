package vibro.navigator.about;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import androidx.annotation.NonNull;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;

final class AboutTtsSettingsLauncher {
    static final String ACTION_TTS_SETTINGS = "com.android.settings.TTS_SETTINGS";
    private static final String TAG = "AboutTtsSettings";

    private AboutTtsSettingsLauncher() {
    }

    static void open(@NonNull Activity activity) {
        if (tryStart(activity, new Intent(ACTION_TTS_SETTINGS))) {
            return;
        }
        if (tryStart(activity, new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))) {
            return;
        }
        Toast.makeText(activity, R.string.msg_open_settings_failed, Toast.LENGTH_SHORT).show();
    }

    private static boolean tryStart(@NonNull Activity activity, @NonNull Intent intent) {
        try {
            activity.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            AppLogger.w(TAG, "Could not open TTS settings action=" + intent.getAction(), e);
            return false;
        }
    }
}
