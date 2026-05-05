package vibro.navigator.intent;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.net.URLDecoder;

final class IntentUriDecoder {

    private IntentUriDecoder() {
    }

    @NonNull
    static String decodeComponent(@NonNull String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (Exception ignored) {
            return Uri.decode(value);
        }
    }
}
