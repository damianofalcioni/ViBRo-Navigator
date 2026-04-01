package com.vibenavigator.brouter;

import android.content.Context;
import android.os.Bundle;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import btools.routingapp.BRouterServiceConnection;
import btools.routingapp.IBRouterService;

public final class BRouterClient implements AutoCloseable {

    private final Context appContext;
    private BRouterServiceConnection connection;

    public BRouterClient(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    public boolean connect() {
        if (connection != null && connection.getBrouterService() != null) {
            return true;
        }
        connection = BRouterServiceConnection.connect(appContext);
        if (connection == null) {
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
                break;
            }
        }
        return connection.getBrouterService() != null;
    }

    @Nullable
    public String getTrackFromParams(@NonNull Bundle params) throws Exception {
        if (!connect()) {
            return null;
        }
        IBRouterService svc = connection.getBrouterService();
        if (svc == null) {
            return null;
        }
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
            } catch (Exception ignored) {
                // ignore
            }
            connection = null;
        }
    }
}
