package vibro.navigator.auto;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.car.app.CarContext;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Pane;
import androidx.car.app.model.Row;
import androidx.core.graphics.drawable.IconCompat;

import java.util.Objects;

import vibro.navigator.R;
import vibro.navigator.android.language.AndroidAppLanguage;
import vibro.navigator.settings.AppNavigationHintSettings;

/** Adds map gesture instructions to the host's idle pane. */
final class ViBRoCarGestureHints {
    private static final int SCROLL_AND_SCALE_API = 2;
    private static final int CLICK_API = 5;
    private static final int ICON_SIZE_PX = 96;

    private ViBRoCarGestureHints() { }

    static void addRows(Pane.Builder pane, CarContext context) {
        int api = context.getCarAppApiLevel();
        if (api < SCROLL_AND_SCALE_API || !AppNavigationHintSettings.isEnabled(context)) {
            return;
        }
        int switchViews = api >= CLICK_API ? R.string.auto_hint_switch_views_text : R.string.auto_hint_swipe_views_text;
        pane.addRow(row(context, R.string.auto_hint_switch_views, switchViews, R.drawable.ic_gesture_views));
        pane.addRow(row(context, R.string.auto_hint_zoom, R.string.auto_hint_zoom_text, R.drawable.ic_gesture_zoom));
        pane.addRow(row(context, R.string.auto_hint_tilt, R.string.auto_hint_tilt_text, R.drawable.ic_auto_gesture_tilt));
    }

    private static Row row(CarContext context, @StringRes int title, @StringRes int body, @DrawableRes int icon) {
        return new Row.Builder()
                .setTitle(AndroidAppLanguage.getString(context, title))
                .addText(AndroidAppLanguage.getString(context, body))
                .setImage(icon(context, icon))
                .build();
    }

    private static CarIcon icon(CarContext context, @DrawableRes int resource) {
        // Resolve the shared vector in the app's theme before sending it to the host.
        var drawable = Objects.requireNonNull(context.getDrawable(resource)).mutate();
        drawable.setTint(Color.WHITE);
        drawable.setBounds(0, 0, ICON_SIZE_PX, ICON_SIZE_PX);
        Bitmap bitmap = Bitmap.createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888);
        drawable.draw(new Canvas(bitmap));
        return new CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).setTint(CarColor.DEFAULT).build();
    }
}
