package vibro.navigator.auto;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import androidx.car.app.CarContext;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.location.NavigationGpsStatusFormatter;
import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

/** Displays the navigation fix status inside the map's stable area. */
final class ViBRoAutoGpsStatusPainter {
    private final CarContext context;
    private final ElapsedRealtimeClock clock;
    private final AndroidNavigationTextResources textResources;
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
    private StaticLayout layout;
    private int cachedMaxWidth;

    ViBRoAutoGpsStatusPainter(CarContext context, ElapsedRealtimeClock clock) {
        this.context = context;
        this.clock = clock;
        textResources = new AndroidNavigationTextResources(context);
        refreshTheme();
    }

    void refreshTheme() {
        textPaint.setColor(AndroidAppTheme.color(context, R.attr.vibroTextPrimaryColor));
        background.setColor(AndroidAppTheme.color(context, R.attr.vibroSurfaceColor));
        layout = null;
    }

    void draw(Canvas canvas, Rect area, NavGpsStatus status, float scale) {
        float inset = ViBRoAutoRenderScale.dp(context, 8f, scale);
        int maxWidth = (int) (area.width() - 4 * inset);
        if (maxWidth <= 0) {
            return;
        }
        StaticLayout text = layout(status, maxWidth, ViBRoAutoRenderScale.sp(context, 16f, scale));
        float left = area.exactCenterX() - text.getWidth() / 2f;
        float top = area.bottom - text.getHeight() - 2 * inset;
        int save = canvas.save();
        canvas.clipRect(area);
        canvas.drawRoundRect(left - inset, top - inset / 2, left + text.getWidth() + inset,
                top + text.getHeight() + inset / 2, inset, inset, background);
        canvas.translate(left, top);
        text.draw(canvas);
        canvas.restoreToCount(save);
    }

    private StaticLayout layout(NavGpsStatus status, int maxWidth, float textSize) {
        String line = NavigationGpsStatusFormatter.statusLine(textResources, status, clock.elapsedRealtimeMs());
        if (layout == null || maxWidth != cachedMaxWidth || textSize != textPaint.getTextSize()
                || !line.contentEquals(layout.getText())) {
            textPaint.setTextSize(textSize);
            cachedMaxWidth = maxWidth;
            int width = Math.max(1, Math.min(maxWidth, (int) Math.ceil(Layout.getDesiredWidth(line, textPaint))));
            layout = StaticLayout.Builder.obtain(line, 0, line.length(), textPaint, width)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build();
        }
        return layout;
    }
}
