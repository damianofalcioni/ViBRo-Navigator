package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vibro.navigator.poi.Poi;

@RunWith(RobolectricTestRunner.class)
public class SavedRouteStoreTest {
    private static final Poi DESTINATION = new Poi("Destination", 48.2082d, 16.3738d);
    private static final Poi DESTINATION_2 = new Poi("Destination 2", 45.4642d, 9.19d);
    private static final Poi STOP_A = new Poi("Stop A", 47.0707d, 15.4395d);
    private static final Poi STOP_B = new Poi("Stop B", 46.0569d, 14.5058d);

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences(SavedRouteStore.PREFS, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void save_persistsDestinationStopsAndNewestFirstOrder() {
        SavedRouteStore store = new SavedRouteStore(context);
        store.save("First route", DESTINATION, Arrays.asList(STOP_A, STOP_B));
        store.save("Second route", DESTINATION_2, Collections.emptyList());

        List<SavedRoute> routes = new SavedRouteStore(context).list();

        assertEquals(2, routes.size());
        assertEquals("Second route", routes.get(0).name);
        assertEquals(DESTINATION_2.name, routes.get(0).destination.name);
        assertTrue(routes.get(0).stops.isEmpty());
        assertEquals("First route", routes.get(1).name);
        assertEquals(DESTINATION.name, routes.get(1).destination.name);
        assertEquals(Arrays.asList(STOP_A.name, STOP_B.name), Arrays.asList(
                routes.get(1).stops.get(0).name,
                routes.get(1).stops.get(1).name
        ));
    }

    @Test
    public void renameAndRemove_updateStoredRoutes() {
        SavedRouteStore store = new SavedRouteStore(context);
        SavedRoute route = store.save("Original", DESTINATION, Collections.singletonList(STOP_A));

        assertTrue(store.rename(route.id, "Updated"));
        assertFalse(store.rename(route.id, " "));

        List<SavedRoute> renamedRoutes = new SavedRouteStore(context).list();
        assertEquals("Updated", renamedRoutes.get(0).name);

        store.remove(route.id);

        assertTrue(new SavedRouteStore(context).list().isEmpty());
    }
}
