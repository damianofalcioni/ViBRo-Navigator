package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.car.app.CarContext;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.poi.Poi;

@RunWith(RobolectricTestRunner.class)
public class ViBRoCarDestinationTemplatesTest {
    private final RecordingActions actions = new RecordingActions();

    @Test
    public void apiOneLoadingUsesTextWithoutAnUnsupportedSpinner() throws Exception {
        PaneTemplate loading = (PaneTemplate) templates(1).build(Collections.emptyList(), true, 0, true, actions);
        assertFalse(loading.getPane().isLoading());
        assertFalse(loading.getPane().getRows().get(0).getTexts().get(0).toString().isEmpty());
        assertEquals(1, loading.getPane().getActions().size());
    }

    @Test
    @SuppressWarnings("deprecation") // Assert template refresh compatibility on older hosts.
    public void statusChangesRefreshTheSameHostTemplateAndRetryRemainsExplicit() throws Exception {
        ViBRoCarDestinationTemplates templates = templates(2);
        PaneTemplate loading = (PaneTemplate) templates.build(Collections.emptyList(), true, 0, true, actions);
        assertEquals(1, loading.getPane().getActions().size());
        PaneTemplate timeout = (PaneTemplate) templates.build(Collections.emptyList(), false,
                R.string.auto_destination_timeout, true, actions);
        assertEquals(loading.getTitle(), timeout.getTitle());
        assertEquals(loading.getPane().getRows().get(0).getTitle(), timeout.getPane().getRows().get(0).getTitle());
        assertEquals(1, timeout.getPane().getRows().size());
        assertFalse(loading.getPane().getRows().get(0).getTexts().equals(timeout.getPane().getRows().get(0).getTexts()));
        timeout.getPane().getActions().get(1).getOnClickDelegate().sendClick(new OnDoneCallback() { });
        assertEquals(1, actions.retries);
    }

    @Test
    @SuppressWarnings("deprecation") // Assert the legacy strip used by API 1/2 hosts.
    public void apiOneKeepsCancelInsideTheFiveRowLimit() throws Exception {
        ListTemplate result = (ListTemplate) templates(1).build(destinations(), false, 0, false, actions);
        assertNull(result.getActionStrip());
        assertEquals(5, result.getSingleList().getItems().size());
        Row cancel = (Row) result.getSingleList().getItems().get(4);
        cancel.getOnClickDelegate().sendClick(new OnDoneCallback() { });
        assertEquals(1, actions.cancels);
    }

    @Test
    @SuppressWarnings("deprecation") // Assert the legacy strip used by API 1/2 hosts.
    public void newerHostsBoundDestinationsAndUseTheCancelActionStrip() throws Exception {
        ListTemplate result = (ListTemplate) templates(2).build(destinations(), false, 0, false, actions);
        assertEquals(5, result.getSingleList().getItems().size());
        assertEquals(1, result.getActionStrip().getActions().size());
        Row first = (Row) result.getSingleList().getItems().get(0);
        first.getOnClickDelegate().sendClick(new OnDoneCallback() { });
        assertEquals("Place 0", actions.selected.name);
    }

    private ViBRoCarDestinationTemplates templates(int api) throws Exception {
        ViBRoCarAppServiceSettingsGplayTest contexts = new ViBRoCarAppServiceSettingsGplayTest();
        contexts.setUp();
        CarContext context = contexts.testCarContext(api);
        return new ViBRoCarDestinationTemplates(context);
    }

    private static List<Poi> destinations() {
        List<Poi> values = new ArrayList<>();
        for (int index = 0; index < 6; index++) { values.add(new Poi("Place " + index, 48.2, 16.3)); }
        return values;
    }

    private static final class RecordingActions implements ViBRoCarDestinationTemplates.Actions {
        private int cancels;
        private int retries;
        private Poi selected;
        @Override public void cancel() { cancels++; }
        @Override public void retry() { retries++; }
        @Override public void select(Poi poi) { selected = poi; }
    }
}
