package vibro.navigator.auto;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;

import vibro.navigator.nav.format.NavigationSpeedLimitFormatter;
import vibro.navigator.nav.model.NavState;

/** A speed limit is driving context permitted in the map safe area; controls use host actions. */
final class ViBRoAutoCompassOverlayPainter {
    private static final float BADGE_SIZE_DP = 44f;
    private final CarContext carContext;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF badge = new RectF();

    ViBRoAutoCompassOverlayPainter(CarContext carContext) {
        this.carContext = carContext;
        fill.setColor(Color.WHITE);
        outline.setColor(Color.RED);
        outline.setStyle(Paint.Style.STROKE);
        text.setColor(Color.BLACK);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextAlign(Paint.Align.CENTER);
    }

    void draw(@NonNull Canvas canvas, @NonNull NavState state, @NonNull RectF safeArea, float scale) {
        if (state.routeStatus.speedLimit == null) { return; }
        float size = ViBRoAutoRenderScale.dp(carContext, BADGE_SIZE_DP, scale);
        if (safeArea.width() < size || safeArea.height() < size) { return; }
        outline.setStrokeWidth(ViBRoAutoRenderScale.dp(carContext, 5f, scale));
        badge.set(safeArea.right - size, safeArea.top, safeArea.right, safeArea.top + size);
        float halfStroke = outline.getStrokeWidth() / 2f;
        badge.inset(halfStroke, halfStroke);
        text.setTextSize(ViBRoAutoRenderScale.sp(carContext, 16f, scale));
        canvas.drawOval(badge, fill);
        canvas.drawOval(badge, outline);
        Paint.FontMetrics metrics = text.getFontMetrics();
        canvas.drawText(NavigationSpeedLimitFormatter.formatBadge(state.routeStatus.speedLimit),
                badge.centerX(), badge.centerY() - (metrics.ascent + metrics.descent) / 2f, text);
    }
}
