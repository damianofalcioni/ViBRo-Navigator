package vibro.navigator.nav;

import android.app.Service;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.util.AppLogger;

final class NavigationServiceCommandHandler {

    interface NavigationRequestReader {
        void read(@NonNull Intent intent);
    }

    private static final String TAG = "NavigationService";

    private final NavigationRequestReader navigationRequestReader;
    private final Runnable navigationStarter;
    private final Runnable navigationStopper;
    private final Runnable serviceStopper;
    private final Runnable foregroundPromoter;

    NavigationServiceCommandHandler(
            @NonNull NavigationRequestReader navigationRequestReader,
            @NonNull Runnable navigationStarter,
            @NonNull Runnable navigationStopper,
            @NonNull Runnable serviceStopper,
            @NonNull Runnable foregroundPromoter
    ) {
        this.navigationRequestReader = navigationRequestReader;
        this.navigationStarter = navigationStarter;
        this.navigationStopper = navigationStopper;
        this.serviceStopper = serviceStopper;
        this.foregroundPromoter = foregroundPromoter;
    }

    int handle(@Nullable Intent intent, int flags, int startId) {
        AppLogger.i(TAG, "onStartCommand action=" + (intent == null ? "null" : intent.getAction())
                + " flags=" + flags
                + " startId=" + startId);
        if (intent != null && NavigationService.ACTION_STOP.equals(intent.getAction())) {
            navigationStopper.run();
            serviceStopper.run();
            return Service.START_NOT_STICKY;
        }

        if (intent != null && NavigationService.ACTION_START.equals(intent.getAction())) {
            navigationRequestReader.read(intent);
            navigationStarter.run();
        }

        foregroundPromoter.run();
        return Service.START_STICKY;
    }
}
