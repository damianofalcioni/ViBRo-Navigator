package com.vibenavigator.poi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class PoiHistoryStoreTest {

    private Context context;

    @Before
    public void setUp() {
        Application application = ApplicationProvider.getApplicationContext();
        context = application;
        context.getSharedPreferences("vibenavigator_poi_history", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void rename_updatesMatchingStoredDestinationNameWithoutChangingCoordinates() {
        PoiHistoryStore store = new PoiHistoryStore(context);
        Poi renamed = new Poi("Coffee Spot", 48.2082d, 16.3738d);
        store.addOrPromote(new Poi("Coffee", 48.2082d, 16.3738d));
        store.addOrPromote(new Poi("Office", 48.2100d, 16.3700d));

        boolean changed = store.rename(new Poi("Anything", 48.2082d, 16.3738d), "  Coffee Spot  ");

        assertTrue(changed);
        List<Poi> items = store.list();
        assertEquals(2, items.size());
        assertEquals("Office", items.get(0).name);
        assertEquals(renamed.name, items.get(1).name);
        assertEquals(renamed.lat, items.get(1).lat, 0.0);
        assertEquals(renamed.lon, items.get(1).lon, 0.0);
    }

    @Test
    public void rename_rejectsBlankNames() {
        PoiHistoryStore store = new PoiHistoryStore(context);
        store.addOrPromote(new Poi("Coffee", 48.2082d, 16.3738d));

        boolean changed = store.rename(new Poi("Coffee", 48.2082d, 16.3738d), "   ");

        assertFalse(changed);
        List<Poi> items = store.list();
        assertEquals(1, items.size());
        assertEquals("Coffee", items.get(0).name);
    }

    @Test
    public void search_returnsCaseInsensitiveMatchesInHistoryOrder() {
        PoiHistoryStore store = new PoiHistoryStore(context);
        store.addOrPromote(new Poi("Museum Quarter", 48.2030d, 16.3580d));
        store.addOrPromote(new Poi("Coffee Spot", 48.2082d, 16.3738d));
        store.addOrPromote(new Poi("Office", 48.2100d, 16.3700d));

        List<Poi> items = store.search("  cofF  ", 10);

        assertEquals(1, items.size());
        assertEquals("Coffee Spot", items.get(0).name);
    }
}
