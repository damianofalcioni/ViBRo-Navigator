package vibro.navigator.android.brouter;

import android.content.Context;
import android.os.DeadObjectException;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import btools.routingapp.IBRouterService;
import vibro.navigator.brouter.BRouterRouteClient;
import vibro.navigator.brouter.BRouterRouteException;
import vibro.navigator.brouter.BRouterRouteRequest;
import vibro.navigator.logging.AppLogger;

public final class AndroidBRouterRouteClient implements BRouterRouteClient {

    private static final String TAG = "BRouterClient";
    private static final long CONNECT_RETRY_DELAY_MS = 250L;
    private static final int MAX_REQUEST_ATTEMPTS = 2;

    private final AndroidBRouterConnectionController connectionController;

    public AndroidBRouterRouteClient(@NonNull Context context) {
        connectionController = new AndroidBRouterConnectionController(context);
    }

    public boolean connect() {
        return connectionController.connect();
    }

    @Nullable
    @Override
    public String requestRoutePayload(@NonNull BRouterRouteRequest request) throws Exception {
        for (int attempt = 1; attempt <= MAX_REQUEST_ATTEMPTS; attempt++) {
            IBRouterService service = requireConnectedService();
            if (service == null) {
                return null;
            }
            RoutePayloadAttemptResult result = requestRoutePayloadAttempt(service, request, attempt);
            if (!result.retry) {
                return result.payload;
            }
        }
        return null;
    }

    @NonNull
    private RoutePayloadAttemptResult requestRoutePayloadAttempt(
            @NonNull IBRouterService service,
            @NonNull BRouterRouteRequest request,
            int attempt
    ) throws Exception {
        try {
            AppLogger.d(TAG, "Requesting track from BRouter service attempt="
                    + attempt + "/" + MAX_REQUEST_ATTEMPTS);
            String raw = service.getTrackFromParams(AndroidBRouterParams.buildRouteParams(request));
            return RoutePayloadAttemptResult.done(raw == null ? null : AndroidBRouterResponseDecoder.decode(raw));
        } catch (RemoteException e) {
            if (!recoverFromRouteRequestFailure(attempt, e)) {
                throw BRouterRouteException.serviceUnavailable(routeRequestFailureLogMessage(e), e);
            }
            return RoutePayloadAttemptResult.retry();
        }
    }

    private boolean recoverFromRouteRequestFailure(
            int attempt,
            @NonNull RemoteException error
    ) {
        AppLogger.w(TAG, routeRequestFailureLogMessage(error) + " attempt="
                + attempt + "/" + MAX_REQUEST_ATTEMPTS, error);
        return retryAfterDisconnect(attempt, MAX_REQUEST_ATTEMPTS, routeRequestRecoveryAction(error));
    }

    @NonNull
    private static String routeRequestFailureLogMessage(@NonNull RemoteException error) {
        return error instanceof DeadObjectException
                ? "BRouter binder died during route request"
                : "BRouter remote call failed during route request";
    }

    @NonNull
    private static String routeRequestRecoveryAction(@NonNull RemoteException error) {
        return error instanceof DeadObjectException
                ? "recovering from BRouter binder death"
                : "recovering from BRouter remote failure";
    }

    @Nullable
    private IBRouterService requireConnectedService() {
        if (!connect()) {
            AppLogger.w(TAG, "Cannot request track because BRouter is not connected");
            return null;
        }
        IBRouterService svc = connectedService();
        if (svc != null) {
            return svc;
        }
        AppLogger.w(TAG, "BRouter service became unavailable before route request");
        connectionController.disconnect();
        if (!connect()) {
            return null;
        }
        svc = connectedService();
        if (svc == null) {
            AppLogger.w(TAG, "BRouter service is still unavailable after reconnect");
            return null;
        }
        return svc;
    }

    @Nullable
    private IBRouterService connectedService() {
        return connectionController.connectedService();
    }

    private boolean retryAfterDisconnect(int attempt, int maxAttempts, @NonNull String action) {
        connectionController.disconnect();
        if (attempt >= maxAttempts) {
            return false;
        }
        try {
            Thread.sleep(CONNECT_RETRY_DELAY_MS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            AppLogger.w(TAG, "Interrupted while " + action, e);
            return false;
        }
    }

    @Override
    public void close() {
        connectionController.close();
    }

    private static final class RoutePayloadAttemptResult {
        @Nullable
        private final String payload;
        private final boolean retry;

        private RoutePayloadAttemptResult(@Nullable String payload, boolean retry) {
            this.payload = payload;
            this.retry = retry;
        }

        @NonNull
        private static RoutePayloadAttemptResult done(@Nullable String payload) {
            return new RoutePayloadAttemptResult(payload, false);
        }

        @NonNull
        private static RoutePayloadAttemptResult retry() {
            return new RoutePayloadAttemptResult(null, true);
        }
    }
}
