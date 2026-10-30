package vibro.navigator.nav.service;

import java.util.Collections;
import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.session.NavigationSessionResourceAdapter;
import vibro.navigator.nav.time.NavigationDisplayTime;
import static org.junit.Assert.*;

public class NavigationHeadingCacheFreshnessTest {
    private final NavigationServiceStateCache cache = new NavigationServiceStateCache();
    private final NavigationSession session = new NavigationSession();

    @Test
    public void compassSamplesCannotExtendCachedSpeedAndEtaBeyondTheObservationDeadline() {
        start();
        NavState fresh = build(1_000);
        assertFalse(fresh.routeStatus.progress.destinationLine.contains("--"));
        cache.storeStructuralState(fresh, null);
        assertTrue(cache.canRefreshHeadingOnly(null, 5_000));
        cache.storeHeadingState(NavigationSessionResourceAdapter.withDisplayHeading(session, fresh, 90d, 5f, 5_000));
        assertTrue(cache.canRefreshHeadingOnly(null, 11_000));
        assertFalse(cache.canRefreshHeadingOnly(null, 11_001));
        NavState expired = build(11_001);
        assertTrue(Float.isNaN(expired.gpsStatus.telemetry.speedMps));
        assertTrue(expired.routeStatus.progress.destinationLine.contains("--"));
        cache.storeStructuralState(expired, null);
        assertTrue(cache.canRefreshHeadingOnly(null, 12_000));
        assertTrue(cache.canRefreshHeadingOnly(null, 30_000));
    }

    @Test
    public void aFreshFixStartsANewCacheDeadlineAndClearDropsOldState() {
        start();
        cache.storeStructuralState(build(11_001), null);
        assertTrue(cache.canRefreshHeadingOnly(null, 20_000));
        accept(24_000, 10);
        NavState recovered = build(24_000);
        assertFalse(recovered.routeStatus.progress.destinationLine.contains("--"));
        cache.storeStructuralState(recovered, null);
        assertTrue(cache.canRefreshHeadingOnly(null, 34_000));
        assertFalse(cache.canRefreshHeadingOnly(null, 34_001));
        cache.clear();
        assertFalse(cache.canRefreshHeadingOnly(null, 24_000));
    }

    @Test
    public void futureDatedCachedTelemetryRequiresStructuralRefresh() {
        start();
        cache.storeStructuralState(build(1_000), null);
        assertFalse(cache.canRefreshHeadingOnly(null, 999));
    }

    private void start() {
        session.loadRequest(new NavigationRequest(NavigationRoutingMode.STRAIGHT_LINE, null,
                "Target", new LatLon(0, .01), Collections.emptyList()));
        assertTrue(NavigationSessionResourceAdapter.start(session, TestNavigationTextResources.metric(), 1_000));
        session.setActiveLocationProviders("gps");
        accept(1_000, 0);
    }

    private void accept(long time, double east) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(time, time);
        location.setLongitude(east / 111_320);
        location.setAccuracy(5);
        location.setSpeed(2);
        location.setSpeedAccuracyMetersPerSecond(.1f);
        assertFalse(NavigationSessionResourceAdapter.onRawLocationChanged(session,
                TestNavigationTextResources.metric(), location, time, 3_000).isDropped());
    }

    private NavState build(long elapsedMs) {
        return NavigationSessionResourceAdapter.buildState(session, TestNavigationTextResources.metric(), -1,
                new NavigationDisplayTime(1_800_000_000_000L + elapsedMs, elapsedMs), null, 90d, 5f, null, false, true);
    }
}
