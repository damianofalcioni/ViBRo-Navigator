package vibro.navigator.auto;

import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import vibro.navigator.R;
import vibro.navigator.android.automotive.AndroidCarNavigationStarter;
import vibro.navigator.android.language.AndroidAppLanguage;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.intent.IntentLocationParser;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.model.NavigationRequest;
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
    private final CarContext context;
    private final TaskScheduler scheduler;
    private final Runnable invalidate;
    private final ExecutorService worker;
    private final Access access;
    private List<Poi> results = Collections.emptyList();
    @Nullable private Future<?> pending;
    @Nullable private Poi pendingDestination;
    private int generation;
    private boolean loading;
    private boolean showing;
    private boolean disposed;
    private int message = R.string.nav_route_notice_no_route_found;

    ViBRoCarIncomingNavigation(CarContext context, TaskScheduler scheduler, Runnable invalidate) {
        this(context, scheduler, invalidate, Executors.newSingleThreadExecutor(), new AndroidAccess(context));
    }

    ViBRoCarIncomingNavigation(CarContext context, TaskScheduler scheduler, Runnable invalidate,
            ExecutorService worker, Access access) {
        this.context = context;
        this.scheduler = scheduler;
        this.invalidate = invalidate;
        this.worker = worker;
        this.access = access;
    }

    void handle(Intent intent) {
        String query = IntentLocationParser.parseToQuery(intent.getAction(), intent.getDataString(),
                intent.getStringExtra(Intent.EXTRA_TEXT));
        if (query == null || disposed) {
            return;
        }
        cancel();
        showing = true;
        loading = true;
        int token = generation;
        pending = worker.submit(() -> resolve(query, token));
        invalidate.run();
    }

    private void resolve(String query, int token) {
        Poi coordinate = CoordinateParser.tryParse(query, null);
        if (coordinate != null) {
            scheduler.post(() -> {
                if (isCurrent(token)) { select(coordinate); }
            });
            return;
        }
        try {
            List<Poi> matches = access.search(query);
            scheduler.post(() -> showResults(matches, token));
        } catch (IOException e) {
            AppLogger.w("CarNavigation", "Destination search failed", e);
            scheduler.post(() -> showResults(Collections.emptyList(), token));
        }
    }

    private void showResults(List<Poi> matches, int token) {
        if (!isCurrent(token)) { return; }
        results = new ArrayList<>();
        for (Poi poi : matches) {
            if (poi.hasValidCoordinates()) { results.add(poi); }
        }
        loading = false;
        invalidate.run();
    }

    private void select(Poi destination) {
        pendingDestination = destination;
        results = Collections.emptyList();
        loading = true;
        int token = ++generation;
        pending = worker.submit(() -> {
            NavigationRequest request = access.prepare(destination);
            scheduler.post(() -> finishStart(request, token));
        });
        invalidate.run();
    }

    private void finishStart(@Nullable NavigationRequest request, int token) {
        if (!isCurrent(token)) { return; }
        loading = false;
        if (request != null && access.start(request)) {
            pendingDestination = null;
            showing = false;
        } else {
            message = R.string.auto_setup_required;
        }
        invalidate.run();
    }

    void retryPending() {
        if (showing && !loading && pendingDestination != null) {
            select(pendingDestination);
        }
    }

    @Nullable
    Template template() {
        if (!showing) { return null; }
        Action cancel = new Action.Builder().setTitle(text(R.string.action_cancel))
                .setOnClickListener(() -> { cancel(); invalidate.run(); }).build();
        return results.isEmpty() ? messageTemplate(cancel) : resultTemplate(cancel);
    }

    @SuppressWarnings("deprecation") // Header setters support the retained Car API 1 hosts.
    private Template resultTemplate(Action cancel) {
        ItemList.Builder list = new ItemList.Builder();
        for (Poi poi : results) {
            list.addItem(new Row.Builder().setTitle(poi.displayLabel())
                    .setOnClickListener(() -> select(poi)).build());
        }
        return new ListTemplate.Builder().setTitle(text(R.string.hint_search_destination))
                .setHeaderAction(Action.APP_ICON).setSingleList(list.build())
                .setActionStrip(new ActionStrip.Builder().addAction(cancel).build()).build();
    }

    @SuppressWarnings("deprecation") // Header setters support the retained Car API 1 hosts.
    private Template messageTemplate(Action cancel) {
        MessageTemplate.Builder messageTemplate = new MessageTemplate.Builder(
                text(loading ? R.string.msg_map_poi_loading : message))
                .setLoading(loading).setTitle(text(R.string.auto_title)).setHeaderAction(Action.APP_ICON)
                .addAction(cancel);
        if (pendingDestination != null && !loading) {
            messageTemplate.addAction(new Action.Builder().setTitle(text(android.R.string.ok))
                    .setOnClickListener(this::retryPending).build());
        }
        return messageTemplate.build();
    }

    void cancel() {
        generation++;
        if (pending != null) { pending.cancel(true); }
        pending = null;
        showing = false;
        loading = false;
        pendingDestination = null;
        results = Collections.emptyList();
        message = R.string.nav_route_notice_no_route_found;
    }

    void dispose() {
        disposed = true;
        cancel();
        worker.shutdownNow();
    }

    private boolean isCurrent(int token) { return !disposed && token == generation; }
    private String text(int id) { return AndroidAppLanguage.getString(context, id); }

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
