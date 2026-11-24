package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.net.Uri;

import androidx.car.app.CarContext;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.PaneTemplate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import vibro.navigator.geo.LatLon;
import vibro.navigator.R;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.session.NavigationSessionInitialRoute;
import vibro.navigator.poi.Poi;

@RunWith(RobolectricTestRunner.class)
public class ViBRoCarIncomingNavigationGplayTest {
    private static final String ADDRESS_URI = "geo:0,0?q=Vienna";
    private final ArrayDeque<Runnable> ui = new ArrayDeque<>();
    private final ManualScheduler scheduler = new ManualScheduler();
    private final ManualWorker worker = new ManualWorker();
    private final FakeAccess access = new FakeAccess();
    private ViBRoCarIncomingNavigation incoming;
    private long nowMs;
    private CarContext context;

    @Before
    public void setUp() throws Exception {
        ViBRoCarAppServiceSettingsGplayTest contexts = new ViBRoCarAppServiceSettingsGplayTest();
        contexts.setUp();
        context = contexts.testCarContext();
        incoming = new ViBRoCarIncomingNavigation(context, scheduler, () -> { }, worker, access, () -> nowMs);
    }

    @Test
    public void coordinateVoiceIntentStartsNavigationWithoutOnlineSearch() {
        incoming.handle(intent("geo:0,0?q=48.2,16.3"));
        drain();
        assertEquals(0, access.searches);
        assertEquals(1, access.started.size());
        assertEquals(48.2, access.started.get(0).destination.lat, 0);
        assertNull(incoming.template());
    }

    @Test
    public void obsoleteResolutionCannotStartAfterNewerIntentOrHostStop() {
        incoming.handle(intent("google.navigation:q=48.1,16.1"));
        worker.runNext(); // Old resolution is awaiting main-thread delivery.
        incoming.handle(intent("google.navigation:q=48.2,16.2"));
        drain();
        assertEquals(1, access.started.size());
        assertEquals(48.2, access.started.get(0).destination.lat, 0);
        incoming.handle(intent("geo:0,0?q=48.3,16.3"));
        worker.runNext();
        incoming.cancel();
        drain();
        assertEquals(1, access.started.size());
        assertNull(incoming.template());
    }

    @Test
    public void addressesRequireSelectionAndMissingSetupRetainsDestinationForRetry() {
        incoming.handle(intent(ADDRESS_URI));
        drain();
        assertTrue(incoming.template() instanceof ListTemplate);
        assertTrue(access.started.isEmpty());
        incoming.cancel();
        access.ready = false;
        incoming.handle(intent("google.navigation:q=48.2,16.3"));
        drain();
        assertTrue(incoming.template() instanceof PaneTemplate);
        access.ready = true;
        incoming.retryPending();
        drain();
        assertEquals(1, access.started.size());
    }

    @Test
    public void missingRoutingDataAfterServiceLaunchShowsParkedSetupAndRetriesDestination() {
        incoming.handle(intent("geo:0,0?q=48.5,16.5"));
        drain();
        assertNull(incoming.template());

        access.results.get(0).finished(true);
        assertEquals(context.getString(R.string.auto_setup_required), statusText());
        retryAction();
        drain();
        assertEquals(2, access.started.size());
        assertEquals(48.5, access.started.get(1).destination.lat, 0);
        assertEquals(16.5, access.started.get(1).destination.lon, 0);
        access.results.get(1).finished(false);
        assertNull(incoming.template());
    }

    @Test
    public void oldMissingMapResultCannotReopenSetupAfterReplacementOrCancel() {
        incoming.handle(intent("geo:0,0?q=48.3,16.3"));
        drain();
        incoming.handle(intent("geo:0,0?q=48.4,16.4"));
        drain();
        access.results.get(0).finished(true);
        assertNull(incoming.template());
        incoming.cancel();
        access.results.get(1).finished(true);
        assertNull(incoming.template());
    }

    @Test
    public void disposalRejectsLateStartAndOrdinaryLaunchDoesNothing() {
        incoming.handle(new Intent(Intent.ACTION_MAIN));
        assertNull(incoming.template());
        incoming.handle(intent("geo:0,0?q=48.2,16.3"));
        worker.runNext();
        incoming.dispose();
        drain();
        assertTrue(access.started.isEmpty());
        assertTrue(worker.isShutdown());
    }

    @Test
    public void stalledSearchExpiresAndExplicitRetryCanResolveTheSameQuery() {
        incoming.handle(intent(ADDRESS_URI));
        assertEquals(context.getString(R.string.msg_map_poi_loading), statusText());
        assertEquals(9_000L, scheduler.delayMs);
        nowMs = 9_000;
        scheduler.delayed.remove().run();
        assertTimeout();
        incoming.retryPending(); // Foregrounding must not silently retry a timeout.
        assertTimeout();
        retryAction();
        drain();
        assertTrue(incoming.template() instanceof ListTemplate);
        assertEquals(1, access.searches);
        assertTrue(scheduler.delayed.isEmpty());
    }

    @Test
    @SuppressWarnings("deprecation") // Check the refresh identity used by API 1/2 hosts.
    public void consecutiveTimeoutsKeepTheSameStatusPaneIdentity() {
        incoming.handle(intent(ADDRESS_URI));
        PaneTemplate initial = statusTemplate();
        for (int attempt = 0; attempt < 3; attempt++) {
            assertEquals(context.getString(R.string.msg_map_poi_loading), statusText());
            nowMs += ViBRoCarDestinationLoad.TIMEOUT_MS;
            scheduler.delayed.remove().run();
            assertTimeout();
            PaneTemplate timeout = statusTemplate();
            assertEquals(initial.getTitle(), timeout.getTitle());
            assertEquals(initial.getPane().getRows().get(0).getTitle(), timeout.getPane().getRows().get(0).getTitle());
            retryAction();
        }
        drain();
        assertTrue(incoming.template() instanceof ListTemplate);
        assertEquals(1, access.searches);
    }

    @Test
    public void searchResultsQueuedBeforeExpiryCannotReplaceTimeout() {
        incoming.handle(intent(ADDRESS_URI));
        worker.runNext();
        nowMs = 9_000;
        ui.remove().run(); // Result delivery wins the queue race against the scheduled timeout.
        assertTimeout();
        assertTrue(scheduler.delayed.isEmpty());
    }

    @Test
    public void profilePreparationCannotStartNavigationAfterDeadline() {
        incoming.handle(intent("google.navigation:q=48.2,16.3"));
        worker.runNext();
        nowMs = 9_000;
        ui.remove().run();
        assertTimeout();
        assertTrue(access.started.isEmpty());
        retryAction();
        drain();
        assertEquals(1, access.started.size());
        assertEquals(0, access.searches);
        assertNull(incoming.template());
    }

    @Test
    public void cancellationAndReplacementRemoveOldDeadlineCallbacks() {
        incoming.handle(intent(ADDRESS_URI));
        Runnable obsolete = scheduler.delayed.peek();
        incoming.handle(intent("geo:0,0?q=48.2,16.3"));
        obsolete.run();
        drain();
        assertEquals(1, access.started.size());
        assertTrue(scheduler.delayed.isEmpty());
        incoming.handle(intent(ADDRESS_URI));
        incoming.cancel();
        assertTrue(scheduler.delayed.isEmpty());
        drain();
        assertNull(incoming.template());
    }

    @Test
    public void expiredLoadingIsReplacedWhenTemplatesResumeAfterSleep() {
        incoming.handle(intent(ADDRESS_URI));
        nowMs = 30_000;
        assertTimeout(); // No delayed callback has run yet (Handler delays use uptime).
        assertTrue(scheduler.delayed.isEmpty());
    }

    @Test
    public void oldHostActionsCannotCancelOrReplaceANewerDestination() {
        incoming.handle(intent(ADDRESS_URI));
        PaneTemplate previous = statusTemplate();
        drain();
        ListTemplate oldResults = (ListTemplate) incoming.template();
        incoming.handle(intent("geo:0,0?q=48.4,16.4"));
        previous.getPane().getActions().get(0).getOnClickDelegate().sendClick(new OnDoneCallback() { });
        ((androidx.car.app.model.Row) oldResults.getSingleList().getItems().get(0))
                .getOnClickDelegate().sendClick(new OnDoneCallback() { });
        drain();
        assertEquals(1, access.started.size());
        assertEquals(48.4, access.started.get(0).destination.lat, 0);
        assertNull(incoming.template());
    }

    private void assertTimeout() {
        assertEquals(context.getString(R.string.auto_destination_timeout), statusText());
        assertEquals(2, statusTemplate().getPane().getActions().size());
    }

    private void retryAction() {
        statusTemplate().getPane().getActions().get(1).getOnClickDelegate().sendClick(new OnDoneCallback() { });
    }

    private PaneTemplate statusTemplate() {
        return (PaneTemplate) incoming.template();
    }

    private String statusText() {
        return statusTemplate().getPane().getRows().get(0).getTexts().get(0).toString();
    }

    private void drain() {
        while (!worker.jobs.isEmpty() || !ui.isEmpty()) {
            worker.runNext();
            while (!ui.isEmpty()) { ui.remove().run(); }
        }
    }

    private static Intent intent(String uri) {
        return new Intent(CarContext.ACTION_NAVIGATE, Uri.parse(uri));
    }

    private static final class FakeAccess implements ViBRoCarIncomingNavigation.Access {
        private final List<NavigationRequest> started = new ArrayList<>();
        private final List<NavigationSessionInitialRoute.Callback> results = new ArrayList<>();
        private boolean ready = true;
        private int searches;
        @Override public List<Poi> search(String query) {
            searches++;
            return Collections.singletonList(new Poi(query, 48.2, 16.3));
        }
        @Override public NavigationRequest prepare(Poi poi) {
            return new NavigationRequest("car-fast", poi.name, new LatLon(poi.lat, poi.lon), Collections.emptyList());
        }
        @Override public boolean start(NavigationRequest request, NavigationSessionInitialRoute.Callback result) {
            if (!ready) { return false; }
            started.add(request);
            results.add(result);
            return true;
        }
    }

    private static final class ManualWorker extends AbstractExecutorService {
        private final ArrayDeque<Runnable> jobs = new ArrayDeque<>();
        private boolean shutdown;
        @Override public void execute(Runnable command) { jobs.add(command); }
        void runNext() { if (!jobs.isEmpty()) { jobs.remove().run(); } }
        @Override public void shutdown() { shutdown = true; }
        @Override public List<Runnable> shutdownNow() { shutdown = true; return Collections.emptyList(); }
        @Override public boolean isShutdown() { return shutdown; }
        @Override public boolean isTerminated() { return shutdown && jobs.isEmpty(); }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return isTerminated(); }
    }

    private final class ManualScheduler implements TaskScheduler {
        private final ArrayDeque<Runnable> delayed = new ArrayDeque<>();
        private long delayMs;
        @Override public void post(Runnable runnable) { ui.add(runnable); }
        @Override public void postDelayed(Runnable runnable, long delay) {
            delayed.add(runnable);
            delayMs = delay;
        }
        @Override public void removeCallbacks(Runnable runnable) { delayed.remove(runnable); }
    }
}
