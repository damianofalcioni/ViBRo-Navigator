package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Paint;
import android.text.StaticLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ViBRoAutoWrappedTextGplayTest {
    @Test
    public void narrowColumnShowsFullGpsStatusAndCountdownOnMultipleLines() {
        String text = "12 km/h ↑450 m • 6 m • (12) • 15 s";
        StaticLayout layout = ViBRoAutoWrappedText.create(text, new Paint(), 236f, 19f, Float.MAX_VALUE);
        int lastLine = layout.getLineCount() - 1;
        assertTrue(lastLine > 0);
        assertEquals(text.length(), layout.getLineEnd(lastLine));
        assertEquals(0, layout.getEllipsisCount(lastLine));
    }
}
