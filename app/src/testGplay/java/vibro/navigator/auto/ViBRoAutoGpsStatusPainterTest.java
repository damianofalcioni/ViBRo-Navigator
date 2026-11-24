package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.util.ReflectionHelpers.getField;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.text.StaticLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.GraphicsMode;

import vibro.navigator.R;
import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.model.NavState;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ViBRoAutoGpsStatusPainterTest {
    @Test
    public void fullStatusWrapsInsideSafeAreaAndCountdownRefreshesWithoutANewFix() throws Exception {
        var contexts = new ViBRoCarAppServiceSettingsGplayTest();
        contexts.setUp();
        var context = contexts.testCarContext();
        context.setTheme(R.style.Theme_ViBRoNavigator);
        long[] now = {1_001L};
        var painter = new ViBRoAutoGpsStatusPainter(context, () -> now[0]);
        var area = new Rect(20, 20, 185, 160);
        var bitmap = Bitmap.createBitmap(200, 180, Bitmap.Config.ARGB_8888);
        var canvas = new Canvas(bitmap);
        String gps = "50 km/h ↑130 m • 4 m • (8)";
        var status = new NavGpsStatus(gps, 4_000L);

        painter.draw(canvas, area, status, 1f);
        StaticLayout first = getField(painter, "layout");
        assertEquals(gps + " • 3 s", first.getText().toString());
        assertTrue(first.getLineCount() > 1);
        assertEquals(Color.TRANSPARENT, bitmap.getPixel(area.centerX(), area.bottom));

        now[0] = 3_000L;
        painter.draw(canvas, area, status, 1f);
        StaticLayout countdown = getField(painter, "layout");
        assertEquals(gps + " • 1 s", countdown.getText().toString());

        painter.draw(canvas, area, new NavGpsStatus(gps, NavState.NO_DEADLINE), 1f);
        StaticLayout unavailable = getField(painter, "layout");
        assertEquals(gps + " • --", unavailable.getText().toString());
    }
}
