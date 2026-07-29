package vibro.navigator.poi.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;

import vibro.navigator.R;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiDetails;

public class PoiSuggestionTest {
    private static final String MUSEUM_NAME = "Museum, Test City";
    private static final String MAIN_ENTRANCE_LABEL = "Entrance: main - " + MUSEUM_NAME;
    private final PoiTextResources textResources = new TestPoiTextResources();

    @Test
    public void displayLabel_formatsNominatimEntranceRows() {
        PoiSuggestion suggestion = new PoiSuggestion(entrancePoi(), false);

        assertEquals(MAIN_ENTRANCE_LABEL, suggestion.displayLabel(textResources));
    }

    @Test
    public void selectedPoi_usesRenderedEntranceLabel() {
        PoiSuggestion suggestion = new PoiSuggestion(entrancePoi(), false);

        Poi selected = suggestion.selectedPoi(textResources);

        assertEquals(MAIN_ENTRANCE_LABEL, selected.displayLabel());
        assertEquals(48.2001d, selected.lat, 0.0d);
        assertEquals(16.3001d, selected.lon, 0.0d);
    }

    @Test
    public void hasDetails_returnsTrueForNominatimDetails() {
        PoiSuggestion suggestion = new PoiSuggestion(entrancePoi(), false);

        assertTrue(suggestion.hasDetails());
    }

    @Test
    public void externalMapSearchSuggestion_exposesSearchActionState() {
        PoiSuggestion suggestion = PoiSuggestion.externalMapSearch("  Cafe Central  ");

        assertTrue(suggestion.isExternalMapSearch());
        assertTrue(suggestion.hasInfo());
        assertFalse(suggestion.hasDetails());
        assertFalse(suggestion.deletable);
        assertEquals("Cafe Central", suggestion.externalMapSearchQuery());
        assertEquals(
                textResources.getString(R.string.action_search_google_maps),
                suggestion.displayLabel(textResources)
        );
    }

    private static Poi entrancePoi() {
        PoiDetails details = new PoiDetails(
                Collections.emptyMap(),
                Collections.singletonMap("wheelchair", "yes"),
                MUSEUM_NAME,
                "main"
        );
        return new Poi(MUSEUM_NAME, 48.2001d, 16.3001d, details);
    }
}
