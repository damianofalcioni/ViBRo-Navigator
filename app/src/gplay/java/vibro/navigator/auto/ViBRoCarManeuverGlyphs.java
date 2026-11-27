package vibro.navigator.auto;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.core.graphics.drawable.IconCompat;

import java.util.HashMap;
import java.util.Map;

import vibro.navigator.nav.directions.VoiceHintMapper;
import vibro.navigator.nav.route.VoiceHint;

/** Reuses the phone's direction symbols for native Android Auto maneuver icons. */
final class ViBRoCarManeuverGlyphs {
    private static final int ICON_SIZE = 128;
    private static final float MAX_GLYPH_SIZE = 104f;

    private final Map<String, CarIcon> icons = new HashMap<>();

    CarIcon get(VoiceHint hint, int exitNumber) {
        String arrow = symbol(hint);
        String glyph = exitNumber > 0 ? arrow + exitNumber : arrow;
        CarIcon icon = icons.get(glyph);
        if (icon == null) {
            icon = draw(glyph);
            icons.put(glyph, icon);
        }
        return icon;
    }

    private static String symbol(VoiceHint hint) {
        return switch (hint.command) {
            case 13 -> "⟲";
            case 14 -> "⟳";
            default -> VoiceHintMapper.toDirection(hint).emoji.replace('⬆', '↑');
        };
    }

    private static CarIcon draw(String symbol) {
        Bitmap bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(MAX_GLYPH_SIZE);
        Rect ink = new Rect();
        paint.getTextBounds(symbol, 0, symbol.length(), ink);
        int largestDimension = Math.max(ink.width(), ink.height());
        if (largestDimension > MAX_GLYPH_SIZE) {
            paint.setTextSize(MAX_GLYPH_SIZE * MAX_GLYPH_SIZE / largestDimension);
            paint.getTextBounds(symbol, 0, symbol.length(), ink);
        }
        float center = ICON_SIZE / 2f;
        new Canvas(bitmap).drawText(symbol, center - ink.exactCenterX(), center - ink.exactCenterY(), paint);
        return new CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).setTint(CarColor.DEFAULT).build();
    }
}
