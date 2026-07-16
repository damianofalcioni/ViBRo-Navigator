package vibro.navigator.android.speech;

import android.content.Context;
import android.speech.SpeechRecognizer;

import androidx.annotation.NonNull;

public final class AndroidSpeechRecognitionSupport {
    private AndroidSpeechRecognitionSupport() {
    }

    public static boolean isAvailable(@NonNull Context context) {
        AndroidSpeechRecognitionAvailability availability =
                new AndroidSpeechRecognitionAvailability(context.getPackageManager());
        return availability.hasRecognitionProvider() || SpeechRecognizer.isRecognitionAvailable(context);
    }
}
