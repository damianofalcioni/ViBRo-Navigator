package vibro.navigator.auto;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.SpannableString;
import android.text.StaticLayout;
import android.text.TextPaint;

import androidx.car.app.CarContext;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.location.NavigationGpsStatusFormatter;
import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import vibro.navigator.nav.ui.NavigationGpsSpeedStyle;

/** Displays the navigation fix status inside the map's stable area. */
final class ViBRoAutoGpsStatusPainter {
    private static final int FULLSCREEN_PANEL_ALPHA = 210;
    private static final float STATUS_TEXT_SIZE_SP = 22f;
    private final CarContext context;
    private final ElapsedRealtimeClock clock;
    private final AndroidNavigationTextResources textResources;
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
    private StaticLayout layout;
    private int cachedMaxWidth;
    private boolean cachedOverLimit;

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

    float prepare(Rect area, NavGpsStatus status, float scale, boolean overLimit) {
        float inset = ViBRoAutoRenderScale.dp(context, 8f, scale);
        int maxWidth = (int) (area.width() - 4 * inset);
        if (maxWidth <= 0) {
            return 0f;
        }
        float textSize = ViBRoAutoRenderScale.sp(context, STATUS_TEXT_SIZE_SP, scale);
        StaticLayout text = layout(status, maxWidth, textSize, overLimit);
        float panelHeight = text.getHeight() + 2 * inset;
        return panelHeight < area.height() ? panelHeight : 0f;
    }

    void draw(Canvas canvas, Rect area, float scale, float panelHeight, boolean fullscreenRoute) {
        if (layout == null || panelHeight <= 0f) { return; }
        float inset = ViBRoAutoRenderScale.dp(context, 8f, scale);
        float left = area.exactCenterX() - layout.getWidth() / 2f;
        float top = area.bottom - panelHeight + inset;
        background.setAlpha(fullscreenRoute ? FULLSCREEN_PANEL_ALPHA : 255);
        int save = canvas.save();
        canvas.clipRect(area);
        canvas.drawRoundRect(left - inset, top - inset / 2, left + layout.getWidth() + inset,
                top + layout.getHeight() + inset / 2, inset, inset, background);
        canvas.translate(left, top);
        layout.draw(canvas);
        canvas.restoreToCount(save);
    }

    private StaticLayout layout(NavGpsStatus status, int maxWidth, float textSize, boolean overLimit) {
        String line = NavigationGpsStatusFormatter.statusLine(textResources, status, clock.elapsedRealtimeMs());
        if (layout == null || maxWidth != cachedMaxWidth || textSize != textPaint.getTextSize()
                || overLimit != cachedOverLimit || !line.contentEquals(layout.getText())) {
            textPaint.setTextSize(textSize);
            cachedMaxWidth = maxWidth;
            cachedOverLimit = overLimit;
            CharSequence renderedLine = line;
            if (overLimit) {
                SpannableString styled = new SpannableString(line);
                NavigationGpsSpeedStyle.apply(context, styled, status.telemetry.speedText);
                renderedLine = styled;
            }
            int width = Math.max(1, Math.min(maxWidth, (int) Math.ceil(Layout.getDesiredWidth(renderedLine, textPaint))));
            layout = StaticLayout.Builder.obtain(renderedLine, 0, renderedLine.length(), textPaint, width)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build();
        }
        return layout;
    }
}
