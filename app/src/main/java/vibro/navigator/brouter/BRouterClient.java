package vibro.navigator.brouter;

import android.content.Context;
import android.os.DeadObjectException;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.util.AppLogger;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import btools.routingapp.BRouterServiceConnection;
import btools.routingapp.IBRouterService;

public final class BRouterClient implements AutoCloseable {

    private static final String TAG = "BRouterClient";
    private static final int MAX_CONNECT_ATTEMPTS = 3;
    private static final long CONNECT_WAIT_TIMEOUT_MS = 1500L;
    private static final long CONNECT_POLL_INTERVAL_MS = 50L;
    private static final long CONNECT_RETRY_DELAY_MS = 250L;
    private static final int MAX_REQUEST_ATTEMPTS = 2;

    private final Context appContext;
    private BRouterServiceConnection connection;

    public BRouterClient(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    public boolean connect() {
        if (hasConnectedService()) {
            AppLogger.d(TAG, "Reusing existing BRouter service connection");
            return true;
        }
        for (int attempt = 1; attempt <= MAX_CONNECT_ATTEMPTS; attempt++) {
            disconnectCurrentConnection();
            AppLogger.i(TAG, "Connecting to BRouter service attempt="
                    + attempt + "/" + MAX_CONNECT_ATTEMPTS);
            connection = BRouterServiceConnection.connect(appContext);
            if (connection == null) {
                AppLogger.w(TAG, "BRouter connection object was not created attempt="
                        + attempt + "/" + MAX_CONNECT_ATTEMPTS);
                if (!waitBeforeRetry(attempt, "retrying BRouter bind")) {
                    break;
                }
                continue;
            }
            if (waitForConnectedService()) {
                AppLogger.i(TAG, "BRouter service connected=true");
                return true;
            }
            AppLogger.w(TAG, "Timed out waiting for BRouter service connection attempt="
                    + attempt + "/" + MAX_CONNECT_ATTEMPTS);
            if (!waitBeforeRetry(attempt, "retrying BRouter bind")) {
                break;
            }
        }
        AppLogger.i(TAG, "BRouter service connected=false");
        return false;
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
            } catch (DeadObjectException e) {
                AppLogger.w(TAG, "BRouter binder died during route request attempt="
                        + attempt + "/" + MAX_REQUEST_ATTEMPTS, e);
                if (!recoverFromRequestFailure(attempt, "recovering from BRouter binder death")) {
                    throw e;
                }
            } catch (RemoteException e) {
                AppLogger.w(TAG, "BRouter remote call failed during route request attempt="
                        + attempt + "/" + MAX_REQUEST_ATTEMPTS, e);
                if (!recoverFromRequestFailure(attempt, "recovering from BRouter remote failure")) {
                    throw e;
                }
            }
        }
        return null;
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
            if (connection == null) {
                return false;
            }
            if (connection.hasNullBinding()) {
                AppLogger.w(TAG, "BRouter returned a null binding");
                return false;
            }
            if (connection.hasBindingDied()) {
                AppLogger.w(TAG, "BRouter binding died before the service became available");
                return false;
            }
            if (hasConnectedService()) {
                return true;
            }
            try {
                Thread.sleep(CONNECT_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                AppLogger.w(TAG, "Interrupted while waiting for BRouter service connection", e);
                return false;
            }
        }
        return hasConnectedService();
    }

    @Nullable
    private IBRouterService requireConnectedService() {
        if (!connect()) {
            AppLogger.w(TAG, "Cannot request track because BRouter is not connected");
            return null;
        }
        IBRouterService svc = connection != null ? connection.getBrouterService() : null;
        if (svc != null && connection != null && !connection.hasBindingDied() && !connection.hasNullBinding()) {
            return svc;
        }
        AppLogger.w(TAG, "BRouter service became unavailable before route request");
        disconnectCurrentConnection();
        if (!connect()) {
            return null;
        }
        svc = connection != null ? connection.getBrouterService() : null;
        if (svc == null || connection == null || connection.hasBindingDied() || connection.hasNullBinding()) {
            AppLogger.w(TAG, "BRouter service is still unavailable after reconnect");
            return null;
        }
        return svc;
    }

    private boolean waitBeforeRetry(int attempt, @NonNull String action) {
        disconnectCurrentConnection();
        if (attempt >= MAX_CONNECT_ATTEMPTS) {
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

    private boolean recoverFromRequestFailure(int attempt, @NonNull String action) {
        disconnectCurrentConnection();
        if (attempt >= MAX_REQUEST_ATTEMPTS) {
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

    private void disconnectCurrentConnection() {
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

    @NonNull
    public static String decodeResult(@NonNull String raw) throws IOException {
        String s = raw.trim();
        if (s.startsWith("ejY0")) { // base64("z64")
            byte[] decoded = Base64.decode(s, Base64.DEFAULT);
            ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
            // skip marker prefix "z64"
            bais.skip(3);
            GZIPInputStream gis = new GZIPInputStream(bais);
            BufferedReader br = new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = br.read(buf)) >= 0) {
                sb.append(buf, 0, n);
            }
            return sb.toString();
        }
        return raw;
    }

    @Override
    public void close() {
        disconnectCurrentConnection();
    }
}
