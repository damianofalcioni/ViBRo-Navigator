package vibro.navigator.android.intent;

import android.content.Intent;

import androidx.annotation.Nullable;

import vibro.navigator.intent.IntentLocationParser;

public final class AndroidIntentLocationParser {

    private AndroidIntentLocationParser() {
    }

    @Nullable
    public static String parseToQuery(@Nullable Intent intent) {
        if (intent == null) {
            return null;
        }
        return IntentLocationParser.parseToQuery(
                intent.getAction(),
                intent.getDataString(),
                intent.getStringExtra(Intent.EXTRA_TEXT)
        );
    }
}
