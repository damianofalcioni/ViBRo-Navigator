package vibro.navigator.poi.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiHistoryStore;
import vibro.navigator.testutil.InMemorySharedPreferences;

public class PoiSuggestionSearchControllerTest {
    private static final String QUERY_INITIAL = "museum";
    private static final String QUERY_UPDATED = "museumx";
    private static final String QUERY_COFFEE = "cof";
    private static final String HISTORY_COFFEE = "Coffee Spot";
    private static final String ONLINE_COFFEE = "Coffee Online";
    private static final String TEST_LOG_TAG = "PoiSuggestionSearchControllerTest";

    private final SharedPreferences preferences = new InMemorySharedPreferences();

    @Test
    public void scheduleSearch_discardsInFlightResultWhenQueryChangesBeforeNextDebounceRuns() {
        FakeScheduler scheduler = new FakeScheduler();
        CapturingSearchDispatcher dispatcher = new CapturingSearchDispatcher();
        CapturingPresenter presenter = new CapturingPresenter();
        PoiSuggestionSearchController controller = new PoiSuggestionSearchController(
                scheduler,
                new PoiHistoryStore(preferences),
                (query, limit) -> Collections.singletonList(new Poi(query, 48.2082d, 16.3738d)),
                TEST_LOG_TAG,
                presenter,
                dispatcher
        );

        controller.scheduleSearch(QUERY_INITIAL);
        scheduler.runDelayed();
        controller.scheduleSearch(QUERY_UPDATED);
        dispatcher.runTask(0);

        assertEquals(1, presenter.suggestions.size());
        assertExternalMapSearch(presenter.suggestions.get(0), QUERY_UPDATED);
        assertTrue(dispatcher.future(0).cancelled);
    }

    @Test
    public void scheduleSearch_doesNotCancelCompletedSearchOnNextImmediateSuggestion() {
        FakeScheduler scheduler = new FakeScheduler();
        CapturingSearchDispatcher dispatcher = new CapturingSearchDispatcher();
        CapturingPresenter presenter = new CapturingPresenter();
        PoiSuggestionSearchController controller = new PoiSuggestionSearchController(
                scheduler,
                new PoiHistoryStore(preferences),
                (query, limit) -> Collections.singletonList(new Poi(query, 48.2082d, 16.3738d)),
                TEST_LOG_TAG,
                presenter,
                dispatcher
        );

        controller.scheduleSearch(QUERY_INITIAL);
        scheduler.runDelayed();
        dispatcher.runTask(0);
        controller.scheduleSearch("48.2082,16.3738");

        assertEquals(1, presenter.suggestions.size());
        assertEquals("48.2082,16.3738", presenter.suggestions.get(0).poi().displayLabel());
        assertFalse(dispatcher.future(0).cancelled);
    }

    @Test
    public void scheduleSearch_showsHistoryImmediatelyAndAppendsOnlineResultsForThreeCharacterQuery() {
        FakeScheduler scheduler = new FakeScheduler();
        CapturingSearchDispatcher dispatcher = new CapturingSearchDispatcher();
        CapturingPresenter presenter = new CapturingPresenter();
        PoiHistoryStore historyStore = new PoiHistoryStore(preferences);
        historyStore.addOrPromote(new Poi(HISTORY_COFFEE, 48.2082d, 16.3738d));
        PoiSuggestionSearchController controller = new PoiSuggestionSearchController(
                scheduler,
                historyStore,
                (query, limit) -> Collections.singletonList(new Poi(ONLINE_COFFEE, 48.2000d, 16.3600d)),
                TEST_LOG_TAG,
                presenter,
                dispatcher
        );

        controller.scheduleSearch(QUERY_COFFEE);
        assertEquals(2, presenter.suggestions.size());
        assertEquals(HISTORY_COFFEE, presenter.suggestions.get(0).poi().displayLabel());
        assertTrue(presenter.suggestions.get(0).deletable);
        assertExternalMapSearch(presenter.suggestions.get(1), QUERY_COFFEE);
        assertEquals(0, dispatcher.taskCount());

        scheduler.runDelayed();
        dispatcher.runTask(0);

        assertEquals(3, presenter.suggestions.size());
        assertEquals(HISTORY_COFFEE, presenter.suggestions.get(0).poi().displayLabel());
        assertEquals(ONLINE_COFFEE, presenter.suggestions.get(1).poi().displayLabel());
        assertExternalMapSearch(presenter.suggestions.get(2), QUERY_COFFEE);
        assertTrue(presenter.suggestions.get(0).deletable);
        assertFalse(presenter.suggestions.get(1).deletable);
    }

    @Test
    public void scheduleSearch_skipsOnlineResultWhenHistoryAlreadyHasSameCoordinates() {
        FakeScheduler scheduler = new FakeScheduler();
        CapturingSearchDispatcher dispatcher = new CapturingSearchDispatcher();
        CapturingPresenter presenter = new CapturingPresenter();
        PoiHistoryStore historyStore = new PoiHistoryStore(preferences);
        historyStore.addOrPromote(new Poi(HISTORY_COFFEE, 48.2082d, 16.3738d));
        PoiSuggestionSearchController controller = new PoiSuggestionSearchController(
                scheduler,
                historyStore,
                (query, limit) -> Collections.singletonList(new Poi(ONLINE_COFFEE, 48.2082d, 16.3738d)),
                TEST_LOG_TAG,
                presenter,
                dispatcher
        );

        controller.scheduleSearch(QUERY_COFFEE);
        scheduler.runDelayed();
        dispatcher.runTask(0);

        assertEquals(2, presenter.suggestions.size());
        assertEquals(HISTORY_COFFEE, presenter.suggestions.get(0).poi().displayLabel());
        assertExternalMapSearch(presenter.suggestions.get(1), QUERY_COFFEE);
    }

    @Test
    public void scheduleSearch_keepsHistoryMatchesWhenOnlineSearchFails() {
        FakeScheduler scheduler = new FakeScheduler();
        CapturingSearchDispatcher dispatcher = new CapturingSearchDispatcher();
        CapturingPresenter presenter = new CapturingPresenter();
        PoiHistoryStore historyStore = new PoiHistoryStore(preferences);
        historyStore.addOrPromote(new Poi(HISTORY_COFFEE, 48.2082d, 16.3738d));
        PoiSuggestionSearchController controller = new PoiSuggestionSearchController(
                scheduler,
                historyStore,
                (query, limit) -> {
                    throw new IOException("offline");
                },
                TEST_LOG_TAG,
                presenter,
                dispatcher
        );

        controller.scheduleSearch(QUERY_COFFEE);
        scheduler.runDelayed();
        dispatcher.runTask(0);

        assertEquals(2, presenter.suggestions.size());
        assertEquals(HISTORY_COFFEE, presenter.suggestions.get(0).poi().displayLabel());
        assertExternalMapSearch(presenter.suggestions.get(1), QUERY_COFFEE);
    }

    @Test
    public void scheduleSearch_showsExternalMapSearchWhileOnlineSearchRunsWithoutHistory() {
        FakeScheduler scheduler = new FakeScheduler();
        CapturingSearchDispatcher dispatcher = new CapturingSearchDispatcher();
        CapturingPresenter presenter = new CapturingPresenter();
        PoiSuggestionSearchController controller = new PoiSuggestionSearchController(
                scheduler,
                new PoiHistoryStore(preferences),
                (query, limit) -> Collections.singletonList(new Poi(ONLINE_COFFEE, 48.2000d, 16.3600d)),
                TEST_LOG_TAG,
                presenter,
                dispatcher
        );

        controller.scheduleSearch(QUERY_COFFEE);

        assertEquals(1, presenter.suggestions.size());
        assertExternalMapSearch(presenter.suggestions.get(0), QUERY_COFFEE);
        assertEquals(0, dispatcher.taskCount());
    }

    private static void assertExternalMapSearch(@NonNull PoiSuggestion suggestion, @NonNull String query) {
        assertTrue(suggestion.isExternalMapSearch());
        assertEquals(query, suggestion.externalMapSearchQuery());
        assertFalse(suggestion.deletable);
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

        int taskCount() {
            return tasks.size();
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
