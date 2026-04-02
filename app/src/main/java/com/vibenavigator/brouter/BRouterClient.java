package com.vibenavigator.brouter;

import android.content.Context;
import android.os.Bundle;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.util.AppLogger;

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

    private final Context appContext;
    private BRouterServiceConnection connection;

    public BRouterClient(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    public boolean connect() {
        if (connection != null && connection.getBrouterService() != null) {
            AppLogger.d(TAG, "Reusing existing BRouter service connection");
            return true;
        }
        AppLogger.i(TAG, "Connecting to BRouter service");
        connection = BRouterServiceConnection.connect(appContext);
        if (connection == null) {
            AppLogger.w(TAG, "BRouter connection object was not created");
            return false;
        }
        // Binding is async; wait briefly for onServiceConnected.
        long deadline = System.currentTimeMillis() + 1500L;
        while (System.currentTimeMillis() < deadline) {
            if (connection.getBrouterService() != null) {
                return true;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                AppLogger.w(TAG, "Interrupted while waiting for BRouter service connection", e);
                break;
            }
        }
        boolean connected = connection.getBrouterService() != null;
        AppLogger.i(TAG, "BRouter service connected=" + connected);
        return connected;
    }

    @Nullable
    public String getTrackFromParams(@NonNull Bundle params) throws Exception {
        if (!connect()) {
            AppLogger.w(TAG, "Cannot request track because BRouter is not connected");
            return null;
        }
        IBRouterService svc = connection.getBrouterService();
        if (svc == null) {
            AppLogger.w(TAG, "BRouter service became unavailable before route request");
            return null;
        }
        AppLogger.d(TAG, "Requesting track from BRouter service");
        return svc.getTrackFromParams(params);
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
        if (connection != null) {
            try {
                connection.disconnect(appContext);
                AppLogger.d(TAG, "Disconnected from BRouter service");
            } catch (Exception e) {
                AppLogger.w(TAG, "Failed to disconnect BRouter service cleanly", e);
            }
            connection = null;
        }
    }
}
