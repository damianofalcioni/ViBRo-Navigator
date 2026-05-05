package vibro.navigator.brouter;

import android.content.Context;
import android.os.DeadObjectException;
import android.os.Bundle;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;

import java.io.IOException;

import btools.routingapp.IBRouterService;

public final class BRouterClient implements AutoCloseable {

    private static final String TAG = "BRouterClient";
    private static final long CONNECT_RETRY_DELAY_MS = 250L;
    private static final int MAX_REQUEST_ATTEMPTS = 2;

    private final BRouterConnectionController connectionController;

    public BRouterClient(@NonNull Context context) {
        connectionController = new BRouterConnectionController(context);
    }

    public boolean connect() {
        return connectionController.connect();
    }

    @Nullable
    public String getTrackFromParams(@NonNull Bundle params) throws Exception {
        for (int attempt = 1; attempt <= MAX_REQUEST_ATTEMPTS; attempt++) {
            IBRouterService svc = requireConnectedService();
            if (svc == null) {
                return null;
            }
            try {
                AppLogger.d(TAG, "Requesting track from BRouter service attempt="
                        + attempt + "/" + MAX_REQUEST_ATTEMPTS);
                return svc.getTrackFromParams(params);
            } catch (RemoteException e) {
                if (!recoverFromRouteRequestFailure(attempt, e)) {
                    throw e;
                }
            }
        }
        return null;
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

    @NonNull
    public static String decodeResult(@NonNull String raw) throws IOException {
        return BRouterResponseDecoder.decode(raw);
    }

    @Override
    public void close() {
        connectionController.close();
    }
}
