package vibro.navigator.poi.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Looper;
import android.widget.EditText;

import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiHistoryStore;
import vibro.navigator.poi.search.PoiSearchClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class PoiInputControllerTest {

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
    public void setPoi_doesNotTriggerSearchSuggestions() {
        AtomicInteger searchCalls = new AtomicInteger();
        PoiSearchClient searchClient = (query, limit) -> {
            searchCalls.incrementAndGet();
            return Collections.emptyList();
        };
        Poi selected = new Poi("Saved destination", 48.2082d, 16.3738d);
        Poi[] listenerSelection = new Poi[1];
        PoiInputController controller = new PoiInputController(
                context,
                new EditText(context),
                new PoiHistoryStore(context),
                searchClient,
                poi -> listenerSelection[0] = poi
        );

        controller.setPoi(selected);
        shadowOf(Looper.getMainLooper()).idleFor(400, java.util.concurrent.TimeUnit.MILLISECONDS);

        assertEquals(0, searchCalls.get());
        assertEquals("Saved destination", controller.getRawText());
        assertSame(selected, controller.getSelectedPoi());
        assertSame(selected, listenerSelection[0]);
    }

    @Test
    public void restorePoi_doesNotTriggerSearchSuggestionsAndRetainsSelection() {
        AtomicInteger searchCalls = new AtomicInteger();
        PoiSearchClient searchClient = (query, limit) -> {
            searchCalls.incrementAndGet();
            return Collections.emptyList();
        };
        Poi selected = new Poi("Stored destination", 48.2082d, 16.3738d);
        PoiInputController controller = new PoiInputController(
                context,
                new EditText(context),
                new PoiHistoryStore(context),
                searchClient,
                poi -> {
                }
        );

        controller.restorePoi(selected);
        shadowOf(Looper.getMainLooper()).idleFor(400, java.util.concurrent.TimeUnit.MILLISECONDS);

        assertEquals(0, searchCalls.get());
        assertEquals("Stored destination", controller.getRawText());
        assertSame(selected, controller.getSelectedPoi());
    }

    @Test
    public void restoreText_doesNotTriggerSearchSuggestions() {
        AtomicInteger searchCalls = new AtomicInteger();
        PoiSearchClient searchClient = (query, limit) -> {
            searchCalls.incrementAndGet();
            return Collections.emptyList();
        };
        PoiInputController controller = new PoiInputController(
                context,
                new EditText(context),
                new PoiHistoryStore(context),
                searchClient,
                poi -> {
                }
        );

        controller.restoreText("Cafe Central");
        shadowOf(Looper.getMainLooper()).idleFor(400, java.util.concurrent.TimeUnit.MILLISECONDS);

        assertEquals(0, searchCalls.get());
        assertEquals("Cafe Central", controller.getRawText());
        assertEquals(null, controller.getSelectedPoi());
    }

    @Test
    public void typedQuery_prefersMatchingHistoryOverOnlineSearch() {
        AtomicInteger searchCalls = new AtomicInteger();
        PoiHistoryStore historyStore = new PoiHistoryStore(context);
        historyStore.addOrPromote(new Poi("Coffee Spot", 48.2082d, 16.3738d));
        PoiSearchClient searchClient = (query, limit) -> {
            searchCalls.incrementAndGet();
            return Collections.singletonList(new Poi("Coffee Online", 48.2000d, 16.3600d));
        };
        PoiInputController controller = new PoiInputController(
                context,
                new EditText(context),
                historyStore,
                searchClient,
                poi -> {
                }
        );

        controller.getEditText().setText("coffee");
        shadowOf(Looper.getMainLooper()).idleFor(400, TimeUnit.MILLISECONDS);

        assertEquals(0, searchCalls.get());
        assertEquals(1, controller.getSuggestionCountForTesting());
        assertEquals("Coffee Spot", controller.getSuggestionLabelForTesting(0));
    }

    @Test
    public void typedSingleCharacterQuery_canReturnMatchingHistoryWithoutOnlineSearch() {
        AtomicInteger searchCalls = new AtomicInteger();
        PoiHistoryStore historyStore = new PoiHistoryStore(context);
        historyStore.addOrPromote(new Poi("Coffee Spot", 48.2082d, 16.3738d));
        PoiSearchClient searchClient = (query, limit) -> {
            searchCalls.incrementAndGet();
            return Collections.emptyList();
        };
        PoiInputController controller = new PoiInputController(
                context,
                new EditText(context),
                historyStore,
                searchClient,
                poi -> {
                }
        );

        controller.getEditText().setText("c");
        shadowOf(Looper.getMainLooper()).idleFor(400, TimeUnit.MILLISECONDS);

        assertEquals(0, searchCalls.get());
        assertEquals(1, controller.getSuggestionCountForTesting());
        assertEquals("Coffee Spot", controller.getSuggestionLabelForTesting(0));
    }

    @Test
    public void typedSingleCharacterQueryWithoutHistoryMatch_doesNotSearchOnline() {
        AtomicInteger searchCalls = new AtomicInteger();
        PoiHistoryStore historyStore = new PoiHistoryStore(context);
        historyStore.addOrPromote(new Poi("Coffee Spot", 48.2082d, 16.3738d));
        PoiSearchClient searchClient = (query, limit) -> {
            searchCalls.incrementAndGet();
            return Collections.emptyList();
        };
        PoiInputController controller = new PoiInputController(
                context,
                new EditText(context),
                historyStore,
                searchClient,
                poi -> {
                }
        );

        controller.getEditText().setText("m");
        shadowOf(Looper.getMainLooper()).idleFor(400, TimeUnit.MILLISECONDS);

        assertEquals(0, searchCalls.get());
        assertEquals(0, controller.getSuggestionCountForTesting());
    }

    @Test
    public void typedQuery_fallsBackToOnlineSearchWhenHistoryHasNoMatch() throws Exception {
        CountDownLatch searchLatch = new CountDownLatch(1);
        AtomicInteger searchCalls = new AtomicInteger();
        PoiSearchClient searchClient = (query, limit) -> {
            searchCalls.incrementAndGet();
            searchLatch.countDown();
            return Collections.emptyList();
        };
        PoiInputController controller = new PoiInputController(
                context,
                new EditText(context),
                new PoiHistoryStore(context),
                searchClient,
                poi -> {
                }
        );

        controller.getEditText().setText("museum");
        shadowOf(Looper.getMainLooper()).idleFor(400, TimeUnit.MILLISECONDS);

        assertTrue(searchLatch.await(1, TimeUnit.SECONDS));
        assertEquals(1, searchCalls.get());
    }
}
