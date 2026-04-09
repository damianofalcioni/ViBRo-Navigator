package com.vibenavigator.poi.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Looper;
import android.widget.EditText;

import androidx.test.core.app.ApplicationProvider;

import com.vibenavigator.poi.Poi;
import com.vibenavigator.poi.PoiHistoryStore;
import com.vibenavigator.poi.search.PoiSearchClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
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
}
