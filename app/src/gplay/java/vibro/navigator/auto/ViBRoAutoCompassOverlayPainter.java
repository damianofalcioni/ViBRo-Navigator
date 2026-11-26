package vibro.navigator.auto;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;

import java.util.Objects;

import vibro.navigator.R;
import vibro.navigator.nav.format.NavigationSpeedLimitFormatter;
import vibro.navigator.nav.model.NavState;

/** A speed limit is driving context permitted in the map safe area; controls use host actions. */
final class ViBRoAutoCompassOverlayPainter {
    private static final float LABEL_WIDTH_FRACTION = 30f / 44f;
    private final Drawable badge;
    private final float badgeSize;
    private final float textSize;
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect labelBounds = new Rect();

    ViBRoAutoCompassOverlayPainter(CarContext carContext) {
        badge = Objects.requireNonNull(carContext.getDrawable(R.drawable.bg_speed_limit_badge));
        badgeSize = carContext.getResources().getDimension(R.dimen.speed_limit_badge_size);
        textSize = carContext.getResources().getDimension(R.dimen.speed_limit_badge_text_size);
        text.setColor(Color.BLACK);
        text.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
    }

    void draw(@NonNull Canvas canvas, @NonNull NavState state, @NonNull RectF safeArea, float scale) {
        if (state.routeStatus.speedLimit == null) { return; }
        int size = Math.round(badgeSize * scale);
        int right = (int) Math.floor(safeArea.right);
        int top = (int) Math.ceil(safeArea.top);
        if (right - size < safeArea.left || top + size > safeArea.bottom) { return; }
        badge.setBounds(right - size, top, right, top + size);
        badge.draw(canvas);
        drawLabel(canvas, NavigationSpeedLimitFormatter.formatBadge(state.routeStatus.speedLimit), scale);
    }

    private void drawLabel(Canvas canvas, String label, float scale) {
        Rect badgeBounds = badge.getBounds();
        text.setTextSize(textSize * scale);
        float width = text.measureText(label);
        float maxWidth = badgeBounds.width() * LABEL_WIDTH_FRACTION;
        if (width > maxWidth) {
            text.setTextSize(text.getTextSize() * maxWidth / width);
        }
        text.getTextBounds(label, 0, label.length(), labelBounds);
        canvas.drawText(label, badgeBounds.exactCenterX() - labelBounds.exactCenterX(),
                badgeBounds.exactCenterY() - labelBounds.exactCenterY(), text);
    }
}
