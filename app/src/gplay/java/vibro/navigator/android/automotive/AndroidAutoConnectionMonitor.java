package vibro.navigator.android.automotive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.car.app.connection.CarConnection;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import vibro.navigator.logging.AppLogger;

public final class AndroidAutoConnectionMonitor {
    private static final String TAG = "AutoConnection";
    // Match the provider used by CarConnectionTypeLiveData in the Car App Library.
    private static final Uri HOST_URI = Uri.parse("content://androidx.car.app.connection");
    private static final Executor CONNECTION_WORKER = Executors.newSingleThreadExecutor();
    private static volatile boolean silenceGuidance;

    private final Context context;
    private final Executor worker;
    private int revision;
    private boolean refreshQueued;

    private AndroidAutoConnectionMonitor(Context context, Executor worker) {
        this.context = context;
        this.worker = worker;
    }

    public static void start(@NonNull Context context) {
        start(context, CONNECTION_WORKER);
    }

    public static boolean shouldSilenceGuidance() {
        return silenceGuidance;
    }

    static void start(Context context, Executor worker) {
        silenceGuidance = true;
        AndroidAutoConnectionMonitor connection = new AndroidAutoConnectionMonitor(
                context.getApplicationContext(), worker);
        worker.execute(connection::register);
    }

    private void register() {
        try {
            if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
                silenceGuidance = false;
                return;
            }
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context receiverContext, Intent intent) {
                    if (CarConnection.ACTION_CAR_CONNECTION_UPDATED.equals(intent.getAction())) {
                        requestRefresh();
                    }
                }
            };
            ContextCompat.registerReceiver(context, receiver,
                    new IntentFilter(CarConnection.ACTION_CAR_CONNECTION_UPDATED), ContextCompat.RECEIVER_EXPORTED);
            requestRefresh();
        } catch (RuntimeException e) {
            AppLogger.w(TAG, "Could not monitor Android Auto connection", e);
        }
    }

    private synchronized void requestRefresh() {
        silenceGuidance = true;
        revision++;
        if (!refreshQueued) {
            refreshQueued = true;
            worker.execute(this::refresh);
        }
    }

    private void refresh() {
        int queryRevision;
        synchronized (this) {
            refreshQueued = false;
            queryRevision = revision;
        }
        boolean silent = readConnection();
        synchronized (this) {
            if (queryRevision == revision) {
                silenceGuidance = silent;
            }
        }
    }

    private boolean readConnection() {
        try (Cursor response = context.getContentResolver().query(
                HOST_URI, new String[]{CarConnection.CAR_CONNECTION_STATE}, null, null, null)) {
            if (response == null) {
                return context.getPackageManager().resolveContentProvider(HOST_URI.getAuthority(), 0) != null;
            }
            int column = response.getColumnIndex(CarConnection.CAR_CONNECTION_STATE);
            if (column < 0 || !response.moveToFirst() || response.isNull(column)) {
                return true;
            }
            int type = response.getInt(column);
            return type != CarConnection.CONNECTION_TYPE_NOT_CONNECTED && type != CarConnection.CONNECTION_TYPE_NATIVE;
        } catch (RuntimeException e) {
            AppLogger.w(TAG, "Could not read Android Auto connection", e);
            return true;
        }
    }
}
