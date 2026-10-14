package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.StaticLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ViBRoAutoInstructionPainterGplayTest {
    private static final String DESCRIPTION = "Svolta leggermente a Sinistra - verso la prossima strada";
    private static final String PREFIX_WIDTH_FIELD = "prefixWidth";

    @Test
    public void onlyDescriptionMovesToEndThenRepeatsOnOneLine() {
        ViBRoAutoInstructionPainter painter = new ViBRoAutoInstructionPainter();
        painter.prepare("↰ 100 m - 10 s - " + DESCRIPTION, paint(Color.WHITE), 24f, 320f);
        assertTrue(painter.isScrolling());
        StaticLayout description = ReflectionHelpers.getField(painter, "instructionLayout");
        assertEquals(DESCRIPTION, description.getText().toString());
        assertEquals(1, description.getLineCount());
        assertEquals(DESCRIPTION.length(), description.getLineEnd(0));
        assertEquals(0, description.getEllipsisCount(0));
        float overflow = ReflectionHelpers.getField(painter, "overflow");
        long travelMs = Math.round(overflow * 1000f / 36f);
        Bitmap start = draw(painter, 0L);
        Bitmap paused = draw(painter, 600L);
        Bitmap moving = draw(painter, 1200L + travelMs / 2);
        Bitmap end = draw(painter, 1200L + travelMs + 600L);
        Bitmap repeated = draw(painter, 2400L + travelMs + 600L);
        assertTrue(instructionArea(start, painter).sameAs(instructionArea(paused, painter)));
        assertFalse(instructionArea(start, painter).sameAs(instructionArea(moving, painter)));
        assertFalse(instructionArea(moving, painter).sameAs(instructionArea(end, painter)));
        assertTrue(instructionArea(start, painter).sameAs(instructionArea(repeated, painter)));
        float prefixWidth = ReflectionHelpers.getField(painter, PREFIX_WIDTH_FIELD);
        assertTrue(prefixWidth < 160f);
        StaticLayout prefix = ReflectionHelpers.getField(painter, "prefixLayout");
        int iconWidth = (int) prefix.getPaint().measureText("↰ ");
        assertSamePrefix(start, moving, iconWidth);
        assertSamePrefix(start, end, iconWidth);
    }

    @Test
    public void countdownAndColorUpdatesPreserveProgressButNewInstructionResets() {
        ViBRoAutoInstructionPainter painter = new ViBRoAutoInstructionPainter();
        painter.prepare("↰ 100 m - 10 s - " + DESCRIPTION, paint(Color.WHITE), 24f, 320f);
        draw(painter, 1000L);
        draw(painter, 3000L);
        painter.prepare("↰ 90 m - 9 s - " + DESCRIPTION, paint(0xFFF28A35), 24f, 320f);
        assertEquals(1000L, (long) ReflectionHelpers.getField(painter, "cycleStartMs"));
        StaticLayout prefix = ReflectionHelpers.getField(painter, "prefixLayout");
        StaticLayout description = ReflectionHelpers.getField(painter, "instructionLayout");
        assertEquals("↰ 90 m", prefix.getText().toString());
        assertEquals(0xFFF28A35, prefix.getPaint().getColor());
        assertEquals(0xFFF28A35, description.getPaint().getColor());
        painter.prepare("↱ 80 m - 8 s - Svolta leggermente a Destra", paint(Color.WHITE), 24f, 320f);
        assertEquals(-1L, (long) ReflectionHelpers.getField(painter, "cycleStartMs"));
        painter.prepare("Arrivo", paint(Color.WHITE), 24f, 320f);
        assertFalse(painter.isScrolling());
        prefix = ReflectionHelpers.getField(painter, "prefixLayout");
        assertEquals("", prefix.getText().toString());
    }

    @Test
    public void resizingAndResettingStopScrollingWhenItIsNotVisible() {
        ViBRoAutoInstructionPainter painter = new ViBRoAutoInstructionPainter();
        String line = "↰ 100 m - 10 s - " + DESCRIPTION;
        painter.prepare(line, paint(Color.WHITE), 24f, 320f);
        draw(painter, 1000L);
        painter.reset();
        assertFalse(painter.isScrolling());
        painter.prepare(line, paint(Color.WHITE), 24f, 320f);
        draw(painter, 5000L);
        assertEquals(5000L, (long) ReflectionHelpers.getField(painter, "cycleStartMs"));
        painter.prepare(line, paint(Color.WHITE), 24f, 2000f);
        assertFalse(painter.isScrolling());
        painter.prepare(line, paint(Color.WHITE), 24f, 1f);
        assertFalse(painter.isScrolling());
        painter.prepare("", paint(Color.WHITE), 24f, 320f);
        assertEquals(0f, painter.height(), 0f);
        assertFalse(painter.isScrolling());
    }

    @Test
    public void shortInstructionAlternatesEveryThreeSecondsWithAStableViewport() {
        ViBRoAutoInstructionPainter painter = new ViBRoAutoInstructionPainter();
        painter.prepare("↰ 20m - 5s - Turn left", paint(Color.WHITE), 24f, 320f);
        assertFalse(painter.isScrolling());
        Bitmap distance = draw(painter, 1000L);
        float reservedWidth = ReflectionHelpers.getField(painter, PREFIX_WIDTH_FIELD);
        assertEquals(3000L, painter.nextMetricRefreshDelayMs(1000L));
        Bitmap time = draw(painter, 4000L);
        assertFalse(distance.sameAs(time));
        assertTrue(instructionArea(distance, painter).sameAs(instructionArea(time, painter)));
        assertTrue(distance.sameAs(draw(painter, 7000L)));
        painter.prepare("↰ 8m - 2s - Turn left", paint(Color.WHITE), 24f, 320f);
        assertEquals(reservedWidth, (float) ReflectionHelpers.getField(painter, PREFIX_WIDTH_FIELD), 0f);
        assertTrue(instructionArea(distance, painter).sameAs(instructionArea(draw(painter, 8000L), painter)));
        assertEquals(2000L, painter.nextMetricRefreshDelayMs(8000L));
        painter.reset();
        assertEquals(Long.MAX_VALUE, painter.nextMetricRefreshDelayMs(8000L));
    }

    private static Paint paint(int color) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        return paint;
    }

    private static Bitmap draw(ViBRoAutoInstructionPainter painter, long nowMs) {
        Bitmap bitmap = Bitmap.createBitmap(360, 64, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLACK);
        painter.draw(canvas, 0f, 0f, nowMs, 36f);
        return bitmap;
    }

    private static void assertSamePrefix(Bitmap expected, Bitmap actual, int width) {
        Bitmap expectedPrefix = Bitmap.createBitmap(expected, 0, 0, width, expected.getHeight());
        Bitmap actualPrefix = Bitmap.createBitmap(actual, 0, 0, width, actual.getHeight());
        assertTrue(expectedPrefix.sameAs(actualPrefix));
    }

    private static Bitmap instructionArea(Bitmap bitmap, ViBRoAutoInstructionPainter painter) {
        float prefixWidth = ReflectionHelpers.getField(painter, PREFIX_WIDTH_FIELD);
        int start = (int) Math.ceil(prefixWidth);
        return Bitmap.createBitmap(bitmap, start, 0, 320 - start, bitmap.getHeight());
    }
}
