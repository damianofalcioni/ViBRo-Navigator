package vibro.navigator.about;

import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;

import androidx.annotation.NonNull;

final class AboutScrollTarget {
    private AboutScrollTarget() {
    }

    static void scrollToOnPreDraw(
            @NonNull ScrollView root,
            @NonNull View target
    ) {
        root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (root.getViewTreeObserver().isAlive()) {
                    root.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                root.scrollTo(0, scrollYFor(root, target));
                return true;
            }
        });
    }

    static int scrollYFor(@NonNull ScrollView root, @NonNull View target) {
        int scrollY = 0;
        View current = target;
        while (current != root) {
            scrollY += current.getTop() - current.getScrollY();
            Object parent = current.getParent();
            if (!(parent instanceof View)) {
                break;
            }
            current = (View) parent;
        }
        return scrollY - root.getPaddingTop();
    }
}
