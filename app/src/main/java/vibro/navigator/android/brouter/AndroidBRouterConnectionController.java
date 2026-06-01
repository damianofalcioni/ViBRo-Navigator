package vibro.navigator.android.brouter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import btools.routingapp.BRouterServiceConnection;
import btools.routingapp.IBRouterService;
import vibro.navigator.logging.AppLogger;

final class AndroidBRouterConnectionController implements AutoCloseable {

    private static final String TAG = "BRouterConnection";
    private static final int MAX_CONNECT_ATTEMPTS = 3;
    private static final long CONNECT_WAIT_TIMEOUT_MS = 1500L;
    private static final long CONNECT_POLL_INTERVAL_MS = 50L;
    private static final long CONNECT_RETRY_DELAY_MS = 250L;

    private final Context appContext;
    private BRouterServiceConnection connection;

    AndroidBRouterConnectionController(@NonNull Context context) {
        appContext = context.getApplicationContext();
    }

    boolean connect() {
        if (hasConnectedService()) {
            AppLogger.d(TAG, "Reusing existing BRouter service connection");
            return true;
        }
        for (int attempt = 1; attempt <= MAX_CONNECT_ATTEMPTS; attempt++) {
            if (tryConnectAttempt(attempt)) {
                return true;
            }
            if (!waitBeforeRetry(attempt)) {
                break;
            }
        }
        AppLogger.i(TAG, "BRouter service connected=false");
        return false;
    }

    @Nullable
    IBRouterService connectedService() {
        if (connection == null || connection.hasBindingDied() || connection.hasNullBinding()) {
            return null;
        }
        return connection.getBrouterService();
    }

    void disconnect() {
        if (connection == null) {
            return;
        }
        try {
            connection.disconnect(appContext);
            AppLogger.d(TAG, "Disconnected from BRouter service");
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to disconnect BRouter service cleanly", e);
        } finally {
            connection = null;
        }
    }

    private boolean tryConnectAttempt(int attempt) {
        disconnect();
        AppLogger.i(TAG, "Connecting to BRouter service attempt="
                + attempt + "/" + MAX_CONNECT_ATTEMPTS);
        connection = BRouterServiceConnection.connect(appContext);
        if (connection == null) {
            AppLogger.w(TAG, "BRouter connection object was not created attempt="
                    + attempt + "/" + MAX_CONNECT_ATTEMPTS);
            return false;
        }
        if (waitForConnectedService()) {
            AppLogger.i(TAG, "BRouter service connected=true");
            return true;
        }
        AppLogger.w(TAG, "Timed out waiting for BRouter service connection attempt="
                + attempt + "/" + MAX_CONNECT_ATTEMPTS);
        return false;
    }

    private boolean hasConnectedService() {
        return connection != null
                && !connection.hasBindingDied()
                && !connection.hasNullBinding()
                && connection.getBrouterService() != null;
    }

    private boolean waitForConnectedService() {
        long deadline = System.currentTimeMillis() + CONNECT_WAIT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (connectionFailedWhileWaiting()) {
                return false;
            }
            if (hasConnectedService()) {
                return true;
            }
            if (!sleepUntilNextConnectionPoll()) {
                return false;
            }
        }
        return hasConnectedService();
    }

    private boolean connectionFailedWhileWaiting() {
        if (connection == null) {
            return true;
        }
        if (connection.hasNullBinding()) {
            AppLogger.w(TAG, "BRouter returned a null binding");
            return true;
        }
        if (connection.hasBindingDied()) {
            AppLogger.w(TAG, "BRouter binding died before the service became available");
            return true;
        }
        return false;
    }

    private boolean sleepUntilNextConnectionPoll() {
        try {
            Thread.sleep(CONNECT_POLL_INTERVAL_MS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            AppLogger.w(TAG, "Interrupted while waiting for BRouter service connection", e);
            return false;
        }
    }

    private boolean waitBeforeRetry(int attempt) {
        disconnect();
        if (attempt >= MAX_CONNECT_ATTEMPTS) {
            return false;
        }
        try {
            Thread.sleep(CONNECT_RETRY_DELAY_MS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            AppLogger.w(TAG, "Interrupted while retrying BRouter bind", e);
            return false;
        }
    }

    @Override
    public void close() {
        disconnect();
    }
}
