package vibro.navigator.android.location;

import android.annotation.SuppressLint;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.location.NavigationLocationListener;
import vibro.navigator.nav.location.NavigationLocationProviders;

/** Each provider owns a listener so reconciliation never cancels another provider's fixes. */
final class AndroidLocationSubscriptions {
    @Nullable private final LocationManager manager;
    private final Executor executor;
    private final NavigationLocationListener listener;
    private final Map<String, LocationListener> listeners = new HashMap<>();
    private final Map<String, Long> intervals = new HashMap<>();

    AndroidLocationSubscriptions(@Nullable LocationManager manager, Executor executor, NavigationLocationListener listener) {
        this.manager = manager;
        this.executor = executor;
        this.listener = listener;
    }

    List<String> reconcile(List<String> providers, long gpsIntervalMs, long networkIntervalMs) {
        for (String active : new ArrayList<>(listeners.keySet())) {
            if (!providers.contains(active)) {
                remove(active);
            }
        }
        List<String> requested = new ArrayList<>(providers.size());
        for (String provider : providers) {
            long interval = NavigationLocationProviders.NETWORK_PROVIDER.equals(provider) ? networkIntervalMs : gpsIntervalMs;
            if (Long.valueOf(interval).equals(intervals.get(provider)) || request(provider, interval)) {
                intervals.put(provider, interval);
                requested.add(provider);
            }
        }
        return requested;
    }

    void clear() {
        for (String provider : new ArrayList<>(listeners.keySet())) {
            remove(provider);
        }
    }

    @SuppressLint("MissingPermission")
    private boolean request(String provider, long intervalMs) {
        if (manager == null) {
            return false;
        }
        LocationListener callback = listenerFor(provider);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manager.requestLocationUpdates(provider, intervalMs, 0f, executor, callback);
            } else {
                manager.requestLocationUpdates(provider, intervalMs, 0f, callback);
            }
            return true;
        } catch (RuntimeException e) {
            remove(provider);
            AppLogger.w("NavLocation", "Failed to request location updates provider=" + provider, e);
            return false;
        }
    }

    private LocationListener listenerFor(String provider) {
        LocationListener callback = listeners.get(provider);
        if (callback == null) {
            callback = new AndroidLocationProviderAccess.AndroidLocationListenerAdapter(listener);
            listeners.put(provider, callback);
        }
        return callback;
    }

    @SuppressLint("MissingPermission")
    private void remove(String provider) {
        LocationListener callback = listeners.remove(provider);
        intervals.remove(provider);
        try {
            if (manager != null && callback != null) {
                manager.removeUpdates(callback);
            }
        } catch (SecurityException e) {
            AppLogger.w("NavLocation", "Permission denied while removing provider=" + provider, e);
        }
    }
}
