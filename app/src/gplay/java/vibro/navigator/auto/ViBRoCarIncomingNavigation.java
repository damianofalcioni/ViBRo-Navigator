package vibro.navigator.auto;

import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.model.Template;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vibro.navigator.R;
import vibro.navigator.android.automotive.AndroidCarNavigationStarter;
import vibro.navigator.android.time.AndroidElapsedRealtimeClock;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.intent.IntentLocationParser;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import vibro.navigator.poi.CoordinateParser;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.search.PoiSearchClients;

/** Latest-intent-wins resolution, with destination selection before ambiguous address results. */
final class ViBRoCarIncomingNavigation {
    interface Access {
        List<Poi> search(String query) throws IOException;
        @Nullable NavigationRequest prepare(Poi destination);
        boolean start(NavigationRequest request);
    }
    private final TaskScheduler scheduler;
    private final Runnable invalidate;
    private final ExecutorService worker;
    private final Access access;
    private final ViBRoCarDestinationLoad load;
    private final ViBRoCarDestinationTemplates templates;
    private List<Poi> results = Collections.emptyList();
    @Nullable private Poi pendingDestination;
    @Nullable private String pendingQuery;
    private boolean showing;
    private boolean disposed;
    private int message = R.string.nav_route_notice_no_route_found;

    ViBRoCarIncomingNavigation(CarContext context, TaskScheduler scheduler, Runnable invalidate) {
        this(context, scheduler, invalidate, Executors.newSingleThreadExecutor(), new AndroidAccess(context));
    }

    ViBRoCarIncomingNavigation(CarContext context, TaskScheduler scheduler, Runnable invalidate,
            ExecutorService worker, Access access) {
        this(context, scheduler, invalidate, worker, access, AndroidElapsedRealtimeClock.INSTANCE);
    }

    ViBRoCarIncomingNavigation(CarContext context, TaskScheduler scheduler, Runnable invalidate,
            ExecutorService worker, Access access, ElapsedRealtimeClock clock) {
        this.scheduler = scheduler;
        this.invalidate = invalidate;
        this.worker = worker;
        this.access = access;
        load = new ViBRoCarDestinationLoad(scheduler, clock, () -> {
            message = R.string.auto_destination_timeout;
            invalidate.run();
        });
        templates = new ViBRoCarDestinationTemplates(context);
    }

    void handle(Intent intent) {
        String query = IntentLocationParser.parseToQuery(intent.getAction(), intent.getDataString(),
                intent.getStringExtra(Intent.EXTRA_TEXT));
        if (query == null || disposed) {
            return;
        }
        cancel();
        showing = true;
        pendingQuery = query;
        Poi coordinate = CoordinateParser.tryParse(query, null);
        if (coordinate != null) {
            select(coordinate);
        } else {
            search();
        }
    }

    private void search() {
        int token = beginLoading();
        String query = pendingQuery;
        load.submitted(token, worker.submit(() -> resolve(query, token)));
        invalidate.run();
    }

    private void resolve(String query, int token) {
        try {
            List<Poi> matches = access.search(query);
            scheduler.post(() -> showResults(matches, token));
        } catch (IOException e) {
            AppLogger.w("CarNavigation", "Destination search failed", e);
            scheduler.post(() -> showResults(Collections.emptyList(), token));
        }
    }

    private void showResults(List<Poi> matches, int token) {
        if (!load.accept(token)) { return; }
        results = new ArrayList<>();
        for (Poi poi : matches) {
            if (poi.hasValidCoordinates()) { results.add(poi); }
        }
        load.finish();
        invalidate.run();
    }

    private void select(Poi destination) {
        if (disposed || !showing) { return; }
        pendingDestination = destination;
        results = Collections.emptyList();
        int token = beginLoading();
        load.submitted(token, worker.submit(() -> {
            NavigationRequest request = access.prepare(destination);
            scheduler.post(() -> finishStart(request, token));
        }));
        invalidate.run();
    }

    private void finishStart(@Nullable NavigationRequest request, int token) {
        if (!load.accept(token)) { return; }
        load.finish();
        if (request != null && access.start(request)) {
            pendingDestination = null;
            showing = false;
        } else {
            message = R.string.auto_setup_required;
        }
        invalidate.run();
    }

    void retryPending() {
        // Returning from phone setup can retry; a timed-out request needs an explicit retry.
        if (message == R.string.auto_setup_required) { retryRequested(); }
    }

    private void retryRequested() {
        if (!showing || load.isLoading() || disposed) { return; }
        if (pendingDestination != null) { select(pendingDestination); }
        else if (pendingQuery != null) { search(); }
    }

    @Nullable
    Template template() {
        if (!showing) { return null; }
        load.refresh();
        return templates.build(results, load.isLoading(), message, pendingDestination != null || pendingQuery != null,
                actionsFor(load.revision()));
    }

    private ViBRoCarDestinationTemplates.Actions actionsFor(int revision) {
        return new ViBRoCarDestinationTemplates.Actions() {
            @Override public void cancel() {
                if (load.matches(revision)) { ViBRoCarIncomingNavigation.this.cancel(); invalidate.run(); }
            }
            @Override public void retry() {
                if (load.matches(revision)) { retryRequested(); }
            }
            @Override public void select(Poi poi) {
                if (load.matches(revision)) { ViBRoCarIncomingNavigation.this.select(poi); }
            }
        };
    }

    private int beginLoading() {
        message = R.string.nav_route_notice_no_route_found;
        return load.begin();
    }

    void cancel() {
        load.cancel();
        showing = false;
        pendingDestination = null;
        pendingQuery = null;
        results = Collections.emptyList();
        message = R.string.nav_route_notice_no_route_found;
    }

    void dispose() {
        disposed = true;
        cancel();
        worker.shutdownNow();
    }

    private static final class AndroidAccess implements Access {
        private final CarContext context;
        AndroidAccess(CarContext context) { this.context = context; }
        @Override public List<Poi> search(String query) throws IOException {
            return PoiSearchClients.createDefault(context).search(query, 5);
        }
        @Override public NavigationRequest prepare(Poi destination) {
            return AndroidCarNavigationStarter.prepare(context, destination);
        }
        @Override public boolean start(NavigationRequest request) {
            return AndroidCarNavigationStarter.start(context, request);
        }
    }
}
