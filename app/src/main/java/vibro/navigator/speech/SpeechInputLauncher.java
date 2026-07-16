package vibro.navigator.speech;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public interface SpeechInputLauncher {

    enum StartMode {
        ACTIVITY,
        DIRECT,
        PENDING_PERMISSION,
        UNAVAILABLE
    }

    @NonNull
    StartMode start(
            @NonNull Intent recognizerIntent,
            int requestCode,
            @NonNull Callback callback
    );

    boolean handleRequestPermissionsResult(int requestCode, @NonNull int[] grantResults);

    void dispose();

    interface Callback {
        void onSpeechInputResult(@NonNull Intent data);

        void onSpeechInputMessage(@StringRes int messageResId);

        void onSpeechInputCancelled();
    }
}
