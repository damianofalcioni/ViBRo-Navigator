package vibro.navigator.poi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import org.junit.Test;

import java.util.List;

import vibro.navigator.testutil.InMemorySharedPreferences;

public class PoiHistoryStoreTest {
    private static final String COFFEE = "Coffee";
    private static final String COFFEE_SPOT = "Coffee Spot";
    private static final String VIENNA_COORDINATES = "48.208200, 16.373800";

    private final SharedPreferences preferences = new InMemorySharedPreferences();

    @Test
    public void rename_updatesMatchingStoredDestinationNameWithoutChangingCoordinates() {
        PoiHistoryStore store = new PoiHistoryStore(preferences);
        Poi renamed = new Poi(COFFEE_SPOT, 48.2082d, 16.3738d);
        store.addOrPromote(new Poi(COFFEE, 48.2082d, 16.3738d));
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
        PoiHistoryStore store = new PoiHistoryStore(preferences);
        store.addOrPromote(new Poi(COFFEE, 48.2082d, 16.3738d));

        boolean changed = store.rename(new Poi(COFFEE, 48.2082d, 16.3738d), "   ");

        assertFalse(changed);
        List<Poi> items = store.list();
        assertEquals(1, items.size());
        assertEquals(COFFEE, items.get(0).name);
    }

    @Test
    public void addOrPromote_sameCoordinatesKeepsNewDisplayName() {
        PoiHistoryStore store = new PoiHistoryStore(preferences);
        store.addOrPromote(new Poi(VIENNA_COORDINATES, 48.2082d, 16.3738d));

        store.addOrPromote(new Poi("Stephansplatz, Vienna", 48.2082d, 16.3738d));

        List<Poi> items = store.list();
        assertEquals(1, items.size());
        assertEquals("Stephansplatz, Vienna", items.get(0).name);
        assertEquals(48.2082d, items.get(0).lat, 0.0d);
        assertEquals(16.3738d, items.get(0).lon, 0.0d);
    }

    @Test
    public void addOrPromote_blankNameStoresCoordinateFallback() {
        PoiHistoryStore store = new PoiHistoryStore(preferences);

        store.addOrPromote(new Poi("", 48.2082d, 16.3738d));

        List<Poi> items = store.list();
        assertEquals(1, items.size());
        assertEquals(VIENNA_COORDINATES, items.get(0).name);
        assertEquals(48.2082d, items.get(0).lat, 0.0d);
        assertEquals(16.3738d, items.get(0).lon, 0.0d);
    }

    @Test
    public void search_returnsCaseInsensitiveMatchesInHistoryOrder() {
        PoiHistoryStore store = new PoiHistoryStore(preferences);
        store.addOrPromote(new Poi("Museum Quarter", 48.2030d, 16.3580d));
        store.addOrPromote(new Poi(COFFEE_SPOT, 48.2082d, 16.3738d));
        store.addOrPromote(new Poi("Office", 48.2100d, 16.3700d));

        List<Poi> items = store.search("  cofF  ", 10);

        assertEquals(1, items.size());
        assertEquals(COFFEE_SPOT, items.get(0).name);
    }

    @Test
    public void addOrPromote_ignoresInvalidCoordinates() {
        PoiHistoryStore store = new PoiHistoryStore(preferences);

        store.addOrPromote(new Poi("Invalid", 91.0d, 16.3738d));

        assertTrue(store.list().isEmpty());
    }

    @Test
    public void list_ignoresStoredItemsWithInvalidCoordinates() {
        preferences.edit()
                .putString("items", "["
                        + "{\"name\":\"Invalid\",\"lat\":91.0,\"lon\":16.3738},"
                        + "{\"name\":\"Coffee\",\"lat\":48.2082,\"lon\":16.3738}"
                        + "]")
                .commit();
        PoiHistoryStore store = new PoiHistoryStore(preferences);

        List<Poi> items = store.list();

        assertEquals(1, items.size());
        assertEquals(COFFEE, items.get(0).name);
    }
}
