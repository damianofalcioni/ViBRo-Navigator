package vibro.navigator.poi.ui;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;

import vibro.navigator.R;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiDetails;

@RunWith(RobolectricTestRunner.class)
public class PoiSuggestionAdapterTest {
    private static final String CAFE = "Cafe";

    @Test
    public void getView_showsInfoButtonWhenSuggestionHasExtraTags() {
        Context context = themedContext();
        PoiSuggestionAdapter adapter = new PoiSuggestionAdapter(context, noopListener());
        PoiDetails details = new PoiDetails(
                Collections.emptyMap(),
                Collections.singletonMap("website", "https://example.test")
        );
        adapter.setItems(Collections.singletonList(new PoiSuggestion(
                new Poi(CAFE, 48.2d, 16.3d, details),
                false
        )));

        View row = adapter.getView(0, null, new LinearLayout(context));
        ImageButton infoButton = row.findViewById(R.id.poiSuggestionInfoButton);

        assertEquals(View.VISIBLE, infoButton.getVisibility());
    }

    @Test
    public void getView_hidesInfoButtonWhenSuggestionHasNoExtraTags() {
        Context context = themedContext();
        PoiSuggestionAdapter adapter = new PoiSuggestionAdapter(context, noopListener());
        adapter.setItems(Collections.singletonList(new PoiSuggestion(
                new Poi(CAFE, 48.2d, 16.3d),
                false
        )));

        View row = adapter.getView(0, null, new LinearLayout(context));
        ImageButton infoButton = row.findViewById(R.id.poiSuggestionInfoButton);

        assertEquals(View.GONE, infoButton.getVisibility());
    }

    @Test
    public void getView_placesInfoButtonAfterSuggestionText() {
        Context context = themedContext();
        PoiSuggestionAdapter adapter = new PoiSuggestionAdapter(context, noopListener());
        PoiDetails details = new PoiDetails(
                Collections.emptyMap(),
                Collections.singletonMap("website", "https://example.test")
        );
        adapter.setItems(Collections.singletonList(new PoiSuggestion(
                new Poi(CAFE, 48.2d, 16.3d, details),
                false
        )));

        LinearLayout row = (LinearLayout) adapter.getView(0, null, new LinearLayout(context));
        TextView text = row.findViewById(R.id.suggestionText);
        ImageButton infoButton = row.findViewById(R.id.poiSuggestionInfoButton);

        assertEquals(row.indexOfChild(text) + 1, row.indexOfChild(infoButton));
    }

    @Test
    public void getView_showsInfoButtonWhenSuggestionHasEntrances() {
        Context context = themedContext();
        PoiSuggestionAdapter adapter = new PoiSuggestionAdapter(context, noopListener());
        PoiDetails details = new PoiDetails(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.singletonList(new PoiDetails.Entrance(
                        48.2d,
                        16.3d,
                        "main",
                        Collections.emptyMap()
                ))
        );
        adapter.setItems(Collections.singletonList(new PoiSuggestion(
                new Poi(CAFE, 48.2d, 16.3d, details),
                false
        )));

        View row = adapter.getView(0, null, new LinearLayout(context));
        ImageButton infoButton = row.findViewById(R.id.poiSuggestionInfoButton);

        assertEquals(View.VISIBLE, infoButton.getVisibility());
    }

    private static PoiSuggestionAdapter.Listener noopListener() {
        return new PoiSuggestionAdapter.Listener() {
            @Override
            public void onSuggestionClicked(PoiSuggestion suggestion) {
            }

            @Override
            public void onInfoClicked(PoiSuggestion suggestion) {
            }

            @Override
            public void onEditClicked(PoiSuggestion suggestion) {
            }

            @Override
            public void onDeleteClicked(PoiSuggestion suggestion) {
            }
        };
    }

    private static Context themedContext() {
        return new ContextThemeWrapper(
                ApplicationProvider.getApplicationContext(),
                R.style.Theme_ViBRoNavigator
        );
    }
}
