package vibro.navigator.android.brouter;

import android.content.Context;
import android.net.Uri;

import java.util.Set;

import vibro.navigator.android.storage.AndroidPersistedUriPermissions;
import vibro.navigator.brouter.BRouterSegmentSourceSettings;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import vibro.navigator.settings.AppCompassSettings;

/** Samples Android's persisted grants without opening or querying a document provider. */
public final class AndroidBRouterStreetGrantChanges {
    private static final long CHECK_INTERVAL_MS = 5_000L;

    private final Context context;
    private final ElapsedRealtimeClock clock;
    private final Runnable onChanged;
    private Set<Uri> readableTrees;
    private long nextCheckMs;

    public AndroidBRouterStreetGrantChanges(Context context, ElapsedRealtimeClock clock, Runnable onChanged) {
        this.context = context;
        this.clock = clock;
        this.onChanged = onChanged;
    }

    public void check() {
        long now = clock.elapsedRealtimeMs();
        if (now < nextCheckMs || !AppCompassSettings.isSurroundingStreetsEnabled(context)) {
            return;
        }
        nextCheckMs = now + CHECK_INTERVAL_MS;
        Set<Uri> previous = readableTrees;
        readableTrees = BRouterSegmentSourceSettings.knownStreetTrees(context);
        AndroidPersistedUriPermissions.retainReadable(context, readableTrees);
        if (previous != null && !previous.equals(readableTrees)) {
            onChanged.run();
        }
    }

    public void checkOnNextViewport() {
        nextCheckMs = 0L;
    }

    public void reset() {
        readableTrees = null;
        checkOnNextViewport();
    }
}
