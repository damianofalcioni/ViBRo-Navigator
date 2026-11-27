package vibro.navigator.auto;

import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.model.Template;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import vibro.navigator.R;
import vibro.navigator.android.automotive.AndroidCarNavigationStarter;
import vibro.navigator.android.language.AndroidAppLanguage;
import vibro.navigator.android.time.AndroidElapsedRealtimeClock;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.intent.IntentLocationParser;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.session.NavigationSessionInitialRoute;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import vibro.navigator.poi.CoordinateParser;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.search.PoiSearchClients;

/** Latest-intent-wins resolution, with destination selection before ambiguous address results. */
final class ViBRoCarIncomingNavigation {
    interface Access {
        List<Poi> search(String query) throws IOException;
        @Nullable NavigationRequest prepare(Poi destination);
        boolean start(NavigationRequest request, NavigationSessionInitialRoute.Callback result);
        void showUnsupportedOperation();
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
        this(context, scheduler, invalidate, ViBRoCarDestinationLoad.newWorker(), new AndroidAccess(context));
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
        if (disposed) {
            return;
        }
        String data = intent.getDataString();
        String operation = IntentLocationParser.navigationOperation(data);
        if (operation != null && !"navigation".equalsIgnoreCase(operation)) {
            access.showUnsupportedOperation();
            return;
        }
        String query = IntentLocationParser.parseToQuery(intent.getAction(), data,
                intent.getStringExtra(Intent.EXTRA_TEXT));
        if (query == null) {
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
        load.submit(token, worker, () -> resolve(query, token));
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
        if (!showing) { return; }
        pendingDestination = destination;
        results = Collections.emptyList();
        int token = beginLoading();
        load.submit(token, worker, () -> {
            NavigationRequest request = access.prepare(destination);
            scheduler.post(() -> finishStart(request, token));
        });
        invalidate.run();
    }

    private void finishStart(@Nullable NavigationRequest request, int token) {
        if (!load.accept(token)) { return; }
        load.finish();
        if (request != null && access.start(request, initialRouteCallback(token))) {
            showing = false;
        } else {
            message = R.string.auto_setup_required;
        }
        invalidate.run();
    }

    private NavigationSessionInitialRoute.Callback initialRouteCallback(int token) {
        // The navigation service can wait for GPS after this car session has been destroyed.
        WeakReference<ViBRoCarIncomingNavigation> owner = new WeakReference<>(this);
        return missing -> {
            ViBRoCarIncomingNavigation navigation = owner.get();
            if (navigation != null) { navigation.finishInitialRoute(missing, token); }
        };
    }

    private void finishInitialRoute(boolean missingRoutingData, int token) {
        if (!load.matches(token)) { return; }
        if (!missingRoutingData) {
            cancel();
            return;
        }
        load.cancel();
        showing = true;
        message = R.string.auto_setup_required;
        invalidate.run();
    }

    void retryPending() {
        // Returning from phone setup can retry; a timed-out request needs an explicit retry.
        if (message == R.string.auto_setup_required) { retryRequested(); }
    }

    private void retryRequested() {
        if (!showing || load.isLoading()) { return; }
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
        @Override public boolean start(NavigationRequest request, NavigationSessionInitialRoute.Callback result) {
            return AndroidCarNavigationStarter.start(context, request, result);
        }
        @Override public void showUnsupportedOperation() {
            String message = AndroidAppLanguage.getString(context, R.string.auto_navigation_operation_unsupported);
            CarToast.makeText(context, message, CarToast.LENGTH_LONG).show();
        }
    }
}
