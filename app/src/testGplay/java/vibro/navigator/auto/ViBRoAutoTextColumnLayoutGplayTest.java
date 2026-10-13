package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.GraphicsMode;

import java.lang.reflect.Field;

import vibro.navigator.R;
import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavPauseStatus;
import vibro.navigator.nav.model.NavProgressStatus;
import vibro.navigator.nav.model.NavRouteStatus;
import vibro.navigator.nav.model.NavState;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ViBRoAutoTextColumnLayoutGplayTest {
    @Test
    public void wrappedTextKeepsGpsAndDirectionsAboveBottomStatus() throws Exception {
        var fixture = new ViBRoCarAppServiceSettingsGplayTest();
        fixture.setUp();
        var carContext = fixture.testCarContext();
        carContext.setTheme(R.style.Theme_ViBRoNavigator);
        ViBRoAutoTextColumnPainter painter = new ViBRoAutoTextColumnPainter(
                carContext, new ViBRoCarAppServiceSettingsGplayTest.RecordingAutoControls(), () -> 1_000L);
        NavState state = new NavState(
                new NavRouteStatus(new NavGuidanceStatus(
                        "150 m • Turn right onto the next street towards the station",
                        "300 m • Turn left at the next junction"),
                        new NavProgressStatus("13:51", "466 m", ""), null),
                new NavGpsStatus("12 km/h ↑450 m • 6 m • (12)", 16_000L), new NavPauseStatus(false));
        Bitmap bitmap = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888);
        painter.draw(new Canvas(bitmap), state, 0f, 0f, 300f, 400f, false, 1f);
        RectF gps = textBounds(painter, "gpsStatusBounds");
        RectF directions = textBounds(painter, "directionsBounds");
        RectF status = textBounds(painter, "statusBounds");
        assertTrue(gps.height() >= 44f);
        assertTrue(directions.height() > 60f);
        assertTrue(directions.top > gps.bottom);
        assertTrue(directions.bottom < status.top);
        assertEquals((gps.bottom + 14f + status.top - 8f) / 2f, directions.centerY(), 2f);
        assertTrue(painter.handleClick(gps.centerX(), gps.bottom - 2f, state));
        bitmap.recycle();
    }

    private static RectF textBounds(ViBRoAutoTextColumnPainter painter, String fieldName) throws Exception {
        Field field = ViBRoAutoTextColumnPainter.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (RectF) field.get(painter);
    }
}
