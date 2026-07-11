package vibro.navigator.poi.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PoiSuggestionPopupControllerTest {
    private static final int ROW_HEIGHT = 50;
    private static final int MIN_VISIBLE_HEIGHT = 40;

    @Test
    public void popupLayout_prefersAboveFieldWhenThereIsRoom() {
        PoiSuggestionPopupController.PopupLayout layout = PoiSuggestionPopupController.popupLayout(
                0,
                1000,
                600,
                80,
                5,
                ROW_HEIGHT,
                MIN_VISIBLE_HEIGHT
        );

        assertEquals(150, layout.heightPx);
        assertEquals(-230, layout.verticalOffsetPx);
    }

    @Test
    public void popupLayout_clampsAboveFieldToAvailableSpace() {
        PoiSuggestionPopupController.PopupLayout layout = PoiSuggestionPopupController.popupLayout(
                100,
                1000,
                220,
                80,
                5,
                ROW_HEIGHT,
                MIN_VISIBLE_HEIGHT
        );

        assertEquals(120, layout.heightPx);
        assertEquals(-200, layout.verticalOffsetPx);
    }

    @Test
    public void popupLayout_fallsBelowFieldWhenAboveSpaceIsTooSmall() {
        PoiSuggestionPopupController.PopupLayout layout = PoiSuggestionPopupController.popupLayout(
                0,
                1000,
                20,
                80,
                5,
                ROW_HEIGHT,
                MIN_VISIBLE_HEIGHT
        );

        assertEquals(150, layout.heightPx);
        assertEquals(0, layout.verticalOffsetPx);
    }
}
