package vibro.navigator.android.automotive;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.car.app.connection.CarConnection;

public final class AndroidAutoConnectionMonitor {
    private static volatile boolean silenceGuidance;

    private AndroidAutoConnectionMonitor() {
    }

    public static void start(@NonNull Context context) {
        // Keep alerts silent until the host answers the first connection query.
        silenceGuidance = true;
        new CarConnection(context.getApplicationContext()).getType().observeForever(type ->
                silenceGuidance = type == null || type == CarConnection.CONNECTION_TYPE_PROJECTION);
    }

    public static boolean shouldSilenceGuidance() {
        return silenceGuidance;
    }
}
