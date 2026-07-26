package vibro.navigator.nav.ui;

import android.graphics.drawable.GradientDrawable;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

final class ForegroundPanelList {
    interface BackgroundFactory {
        @NonNull
        GradientDrawable create();
    }

    private final List<Entry> entries = new ArrayList<>();

    void add(@NonNull View view) {
        entries.add(new Entry(view));
    }

    void apply(boolean enabled, @NonNull BackgroundFactory backgroundFactory, @NonNull Padding padding) {
        for (Entry entry : entries) {
            entry.apply(enabled, backgroundFactory, padding);
        }
    }

    static final class Padding {
        final int horizontal;
        final int vertical;

        Padding(int horizontal, int vertical) {
            this.horizontal = horizontal;
            this.vertical = vertical;
        }
    }

    private static final class Entry {
        @NonNull
        private final View view;
        @NonNull
        private final OriginalPadding originalPadding;

        Entry(@NonNull View view) {
            this.view = view;
            originalPadding = OriginalPadding.from(view);
        }

        void apply(boolean enabled, @NonNull BackgroundFactory backgroundFactory, @NonNull Padding padding) {
            if (!enabled) {
                view.setBackground(null);
                originalPadding.apply(view);
                return;
            }
            view.setBackground(backgroundFactory.create());
            view.setPadding(padding.horizontal, padding.vertical, padding.horizontal, padding.vertical);
        }
    }

    private static final class OriginalPadding {
        private final int left;
        private final int top;
        private final int right;
        private final int bottom;

        private OriginalPadding(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        @NonNull
        static OriginalPadding from(@NonNull View view) {
            return new OriginalPadding(
                    view.getPaddingLeft(),
                    view.getPaddingTop(),
                    view.getPaddingRight(),
                    view.getPaddingBottom()
            );
        }

        void apply(@NonNull View view) {
            view.setPadding(left, top, right, bottom);
        }
    }
}
