package vibro.navigator.android.intent;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.IntentCompat;

import vibro.navigator.nav.session.NavigationSessionInitialRoute;

/** One-shot result channel for a caller that needs to recover from a failed initial route. */
public final class AndroidInitialRouteResult {
    private static final String EXTRA_RESULT = "vibro.navigator.extra.INITIAL_ROUTE_RESULT";
    private static final int FINISHED = 0;
    private static final int MISSING_ROUTING_DATA = 1;

    private AndroidInitialRouteResult() { }

    public static void putInto(Intent intent, NavigationSessionInitialRoute.Callback callback) {
        intent.putExtra(EXTRA_RESULT, new ResultReceiver(new Handler(Looper.getMainLooper())) {
            @Override
            protected void onReceiveResult(int resultCode, @Nullable Bundle resultData) {
                callback.finished(resultCode == MISSING_ROUTING_DATA);
            }
        });
    }

    @Nullable
    public static NavigationSessionInitialRoute.Callback fromIntent(@NonNull Intent intent) {
        ResultReceiver receiver = IntentCompat.getParcelableExtra(intent, EXTRA_RESULT, ResultReceiver.class);
        return receiver == null ? null : missing -> receiver.send(missing ? MISSING_ROUTING_DATA : FINISHED, null);
    }
}
