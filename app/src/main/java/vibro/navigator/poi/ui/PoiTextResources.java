package vibro.navigator.poi.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

interface PoiTextResources {
    @NonNull
    String getString(@StringRes int resId, Object... formatArgs);

    @NonNull
    static PoiTextResources from(@NonNull Context context) {
        return new AndroidPoiTextResources(context);
    }

    final class AndroidPoiTextResources implements PoiTextResources {
        @NonNull
        private final Context context;

        private AndroidPoiTextResources(@NonNull Context context) {
            this.context = context;
        }

        @NonNull
        @Override
        public String getString(@StringRes int resId, Object... formatArgs) {
            return context.getString(resId, formatArgs);
        }
    }
}
