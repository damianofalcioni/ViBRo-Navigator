package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.util.ReflectionHelpers.getField;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.GraphicsMode;

import vibro.navigator.R;
import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.model.NavGpsTelemetry;
import vibro.navigator.nav.model.NavState;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ViBRoAutoGpsStatusPainterTest {
    private static final String LAYOUT = "layout";

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

        float panelHeight = painter.prepare(area, status, 1f, false);
        painter.draw(canvas, area, 1f, panelHeight, true);
        StaticLayout first = getField(painter, LAYOUT);
        assertEquals(gps + " • 3 s", first.getText().toString());
        assertEquals(ViBRoAutoRenderScale.sp(context, 22f, 1f), first.getPaint().getTextSize(), 0f);
        assertTrue(first.getLineCount() > 1);
        int panelTop = Math.round(area.bottom - panelHeight);
        assertEquals(210, Color.alpha(bitmap.getPixel(area.centerX(), panelTop + 5)));
        assertEquals(Color.TRANSPARENT, bitmap.getPixel(area.centerX(), panelTop - 1));
        assertEquals(Color.TRANSPARENT, bitmap.getPixel(area.centerX(), area.bottom));

        now[0] = 3_000L;
        painter.prepare(area, status, 1f, false);
        StaticLayout countdown = getField(painter, LAYOUT);
        assertEquals(gps + " • 1 s", countdown.getText().toString());

        painter.prepare(area, new NavGpsStatus(gps, NavState.NO_DEADLINE), 1f, false);
        StaticLayout unavailable = getField(painter, LAYOUT);
        assertEquals(gps + " • --", unavailable.getText().toString());
    }

    @Test
    public void onlyCurrentSpeedUsesRouteRedAndBoldWhileOverTheLimit() throws Exception {
        var contexts = new ViBRoCarAppServiceSettingsGplayTest();
        contexts.setUp();
        var context = contexts.testCarContext();
        var painter = new ViBRoAutoGpsStatusPainter(context, () -> 1_000L);
        var area = new Rect(20, 20, 500, 160);
        String speed = "50 km/h";
        String line = speed + " ↑130 m • 4 m • (8)";
        var telemetry = new NavGpsTelemetry(50 / 3.6f, line, speed, "", "", "", "", "", "", "");
        var status = new NavGpsStatus(line, NavState.NO_DEADLINE, telemetry);

        painter.prepare(area, status, 1f, true);
        StaticLayout overLimit = getField(painter, LAYOUT);
        Spanned text = (Spanned) overLimit.getText();
        ForegroundColorSpan color = text.getSpans(0, text.length(), ForegroundColorSpan.class)[0];
        StyleSpan style = text.getSpans(0, text.length(), StyleSpan.class)[0];
        assertEquals(context.getColor(R.color.compass_route), color.getForegroundColor());
        assertEquals(Typeface.BOLD, style.getStyle());
        assertEquals(0, text.getSpanStart(color));
        assertEquals(0, text.getSpanStart(style));
        assertEquals(speed.length(), text.getSpanEnd(color));
        assertEquals(speed.length(), text.getSpanEnd(style));

        painter.prepare(area, status, 1f, false);
        StaticLayout normal = getField(painter, LAYOUT);
        assertTrue(!(normal.getText() instanceof Spanned));
    }
}
