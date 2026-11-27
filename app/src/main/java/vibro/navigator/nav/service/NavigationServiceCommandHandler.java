package vibro.navigator.nav.service;

import android.app.Service;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.android.intent.AndroidInitialRouteResult;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.session.NavigationSessionInitialRoute;

public final class NavigationServiceCommandHandler {

    public interface NavigationRequestReader {
        void read(@NonNull Intent intent);
    }

    private static final String TAG = "NavigationService";

    private final NavigationSessionInitialRoute initialRoute = new NavigationSessionInitialRoute();
    private final NavigationRequestReader navigationRequestReader;
    private final Runnable navigationStarter;
    private final Runnable navigationStopper;
    private final Runnable serviceStopper;
    private final Runnable foregroundPromoter;

    public NavigationServiceCommandHandler(
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

    public int handle(@Nullable Intent intent, int flags, int startId) {
        AppLogger.i(TAG, "onStartCommand action=" + (intent == null ? "null" : intent.getAction())
                + " flags=" + flags
                + " startId=" + startId);
        if (intent != null && NavigationService.ACTION_STOP.equals(intent.getAction())) {
            stop();
            return Service.START_NOT_STICKY;
        }

        if (intent != null && NavigationService.ACTION_START.equals(intent.getAction())) {
            initialRoute.replace(AndroidInitialRouteResult.fromIntent(intent));
            navigationRequestReader.read(intent);
            navigationStarter.run();
        }

        foregroundPromoter.run();
        return Service.START_STICKY;
    }

    void reportInitialRoute(NavigationSession session, TaskScheduler scheduler) {
        if (!initialRoute.detectMissingData(session)) {
            return;
        }
        // Report and stop outside the routing callback, only while this request still owns the service.
        var request = session.currentNavigationRequest();
        scheduler.post(() -> {
            if (request == session.currentNavigationRequest() && session.isStarted() && !session.hasActiveRoute()) {
                initialRoute.finish(true);
                stop();
            }
        });
    }

    void clearInitialRoute() {
        initialRoute.finish(false);
    }

    private void stop() {
        navigationStopper.run();
        serviceStopper.run();
    }
}
