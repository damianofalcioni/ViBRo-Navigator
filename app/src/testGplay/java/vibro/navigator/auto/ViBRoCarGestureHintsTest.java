package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.car.app.model.PaneTemplate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import vibro.navigator.R;
import vibro.navigator.android.language.AndroidAppLanguage;
import vibro.navigator.settings.AppNavigationHintSettings;

@RunWith(RobolectricTestRunner.class)
public class ViBRoCarGestureHintsTest {
    private final ViBRoCarAppServiceSettingsGplayTest contexts = new ViBRoCarAppServiceSettingsGplayTest();

    @Before
    public void setUp() {
        contexts.setUp();
    }

    @Test
    public void idlePaneShowsThreeIllustratedHintsByDefault() throws Exception {
        var context = contexts.testCarContext();
        var templates = new ViBRoCarTemplates(context, () -> { });
        var rows = ((PaneTemplate) templates.build(null)).getPane().getRows();

        assertEquals(List.of(R.string.auto_no_active_navigation_title, R.string.auto_hint_switch_views,
                        R.string.auto_hint_zoom, R.string.auto_hint_tilt).stream()
                        .map(id -> AndroidAppLanguage.getString(context, id)).toList(),
                rows.stream().map(row -> row.getTitle().toString()).toList());
        assertTrue(rows.subList(1, 4).stream().allMatch(row -> row.getImage() != null));
        assertEquals(AndroidAppLanguage.getString(context, R.string.auto_hint_switch_views_text),
                rows.get(1).getTexts().get(0).toString());
    }

    @Test
    public void idlePaneShowsHintsWhenPhoneHintsAreDisabled() throws Exception {
        var context = contexts.testCarContext();
        var templates = new ViBRoCarTemplates(context, () -> { });
        AppNavigationHintSettings.setEnabled(context, false);
        assertEquals(4, ((PaneTemplate) templates.build(null)).getPane().getRows().size());
    }

    @Test
    public void olderHostsOnlyShowGesturesAvailableToThem() throws Exception {
        var clickUnsupported = contexts.testCarContext(2);
        var templates = new ViBRoCarTemplates(clickUnsupported, () -> { });
        var pane = (PaneTemplate) templates.build(null);
        assertEquals(AndroidAppLanguage.getString(clickUnsupported, R.string.auto_hint_swipe_views_text),
                pane.getPane().getRows().get(1).getTexts().get(0).toString());

        var gesturesUnsupported = new ViBRoCarTemplates(contexts.testCarContext(1), () -> { });
        assertEquals(1, ((PaneTemplate) gesturesUnsupported.build(null)).getPane().getRows().size());
    }
}
