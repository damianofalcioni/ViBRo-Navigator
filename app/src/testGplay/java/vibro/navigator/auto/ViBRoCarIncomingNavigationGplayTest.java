package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.net.Uri;

import androidx.car.app.CarContext;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.MessageTemplate;

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
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.poi.Poi;

@RunWith(RobolectricTestRunner.class)
public class ViBRoCarIncomingNavigationGplayTest {
    private final ArrayDeque<Runnable> ui = new ArrayDeque<>();
    private final ManualWorker worker = new ManualWorker();
    private final FakeAccess access = new FakeAccess();
    private ViBRoCarIncomingNavigation incoming;

    @Before
    public void setUp() throws Exception {
        ViBRoCarAppServiceSettingsGplayTest contexts = new ViBRoCarAppServiceSettingsGplayTest();
        contexts.setUp();
        CarContext context = contexts.testCarContext();
        incoming = new ViBRoCarIncomingNavigation(context, ui::add, () -> { }, worker, access);
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
        incoming.handle(intent("geo:0,0?q=Vienna"));
        drain();
        assertTrue(incoming.template() instanceof ListTemplate);
        assertTrue(access.started.isEmpty());
        incoming.cancel();
        access.ready = false;
        incoming.handle(intent("google.navigation:q=48.2,16.3"));
        drain();
        assertTrue(incoming.template() instanceof MessageTemplate);
        access.ready = true;
        incoming.retryPending();
        drain();
        assertEquals(1, access.started.size());
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
        private boolean ready = true;
        private int searches;
        @Override public List<Poi> search(String query) {
            searches++;
            return Collections.singletonList(new Poi(query, 48.2, 16.3));
        }
        @Override public NavigationRequest prepare(Poi poi) {
            return new NavigationRequest("car-fast", poi.name, new LatLon(poi.lat, poi.lon), Collections.emptyList());
        }
        @Override public boolean start(NavigationRequest request) {
            if (!ready) { return false; }
            started.add(request);
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
}
