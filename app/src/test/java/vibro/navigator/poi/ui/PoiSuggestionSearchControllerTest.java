package vibro.navigator.poi.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiHistoryStore;

@RunWith(RobolectricTestRunner.class)
public class PoiSuggestionSearchControllerTest {
    private static final String QUERY_INITIAL = "museum";
    private static final String QUERY_UPDATED = "museumx";

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("vibenavigator_poi_history", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void scheduleSearch_discardsInFlightResultWhenQueryChangesBeforeNextDebounceRuns() {
        FakeScheduler scheduler = new FakeScheduler();
        CapturingSearchDispatcher dispatcher = new CapturingSearchDispatcher();
        CapturingPresenter presenter = new CapturingPresenter();
        PoiSuggestionSearchController controller = new PoiSuggestionSearchController(
                scheduler,
                new PoiHistoryStore(context),
                (query, limit) -> Collections.singletonList(new Poi(query, 48.2082d, 16.3738d)),
                "PoiSuggestionSearchControllerTest",
                presenter,
                dispatcher
        );

        controller.scheduleSearch(QUERY_INITIAL);
        scheduler.runDelayed();
        controller.scheduleSearch(QUERY_UPDATED);
        dispatcher.runTask(0);

        assertEquals(0, presenter.suggestions.size());
        assertTrue(dispatcher.future(0).cancelled);
    }

    private static final class FakeScheduler implements TaskScheduler {
        private final List<Runnable> delayed = new ArrayList<>();

        @Override
        public void post(@NonNull Runnable runnable) {
            runnable.run();
        }

        @Override
        public void postDelayed(@NonNull Runnable runnable, long delayMs) {
            delayed.add(runnable);
        }

        @Override
        public void removeCallbacks(@NonNull Runnable runnable) {
            delayed.remove(runnable);
        }

        void runDelayed() {
            Runnable runnable = delayed.remove(0);
            runnable.run();
        }
    }

    private static final class CapturingSearchDispatcher
            implements PoiSuggestionSearchController.SearchDispatcher {
        private final List<Runnable> tasks = new ArrayList<>();
        private final List<CapturingFuture> futures = new ArrayList<>();

        @NonNull
        @Override
        public Future<?> submit(@NonNull Runnable runnable) {
            CapturingFuture future = new CapturingFuture();
            tasks.add(runnable);
            futures.add(future);
            return future;
        }

        void runTask(int index) {
            tasks.get(index).run();
        }

        @NonNull
        CapturingFuture future(int index) {
            return futures.get(index);
        }
    }

    private static final class CapturingFuture implements Future<Object> {
        private boolean cancelled;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelled = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return cancelled;
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public Object get(long timeout, @NonNull TimeUnit unit) {
            return null;
        }
    }

    private static final class CapturingPresenter implements PoiSuggestionSearchController.Presenter {
        private List<PoiSuggestion> suggestions = new ArrayList<>();

        @Override
        public void showHistory() {
            suggestions = new ArrayList<>();
        }

        @Override
        public void showSuggestions(
                @NonNull List<PoiSuggestion> suggestions,
                @NonNull String popupReason
        ) {
            this.suggestions = suggestions;
        }

        @Override
        public void clearSuggestionsAndDismiss() {
            suggestions = new ArrayList<>();
        }
    }
}
