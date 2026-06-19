package vibro.navigator.android.theme;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import vibro.navigator.R;

public final class AndroidThemedButtons {
    private AndroidThemedButtons() {
    }

    public static void applyIconButtonBackground(@NonNull ImageButton button) {
        applyBorderlessSelectableBackground(button);
    }

    public static void applyBorderedIconButtonBackground(@NonNull ImageButton button) {
        applyDrawableBackground(button, R.drawable.bg_button_icon);
    }

    public static void applySecondaryButtonBackground(@NonNull TextView button) {
        applyDrawableBackground(button, R.drawable.bg_button_secondary);
    }

    private static void applyDrawableBackground(@NonNull View view, int backgroundResId) {
        view.setBackgroundResource(backgroundResId);
        view.setBackgroundTintList(null);
    }

    private static void applyBorderlessSelectableBackground(@NonNull View view) {
        view.setBackgroundResource(borderlessSelectableBackground(view.getContext()));
        view.setBackgroundTintList(null);
    }

    private static int borderlessSelectableBackground(@NonNull Context context) {
        TypedValue value = new TypedValue();
        if (!context.getTheme().resolveAttribute(
                android.R.attr.selectableItemBackgroundBorderless,
                value,
                true
        ) || value.resourceId == 0) {
            throw new IllegalStateException("Borderless selectable background is unavailable");
        }
        return value.resourceId;
    }
}
