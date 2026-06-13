package vibro.navigator.main;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class MainRouteRailStopAnchors {

    @NonNull
    private final LinearLayout stopsContainer;
    @Nullable
    private final MainRouteRailView routeRailView;

    MainRouteRailStopAnchors(@NonNull LinearLayout stopsContainer, @Nullable MainRouteRailView routeRailView) {
        this.stopsContainer = stopsContainer;
        this.routeRailView = routeRailView;
    }

    void refresh() {
        if (routeRailView == null) {
            return;
        }
        routeRailView.setStopAnchors(currentStopAnchors());
    }

    void clear() {
        if (routeRailView != null) {
            routeRailView.setStopAnchors(Collections.emptyList());
        }
    }

    @NonNull
    private List<View> currentStopAnchors() {
        List<View> stopAnchors = new ArrayList<>();
        for (int i = 0; i < stopsContainer.getChildCount(); i++) {
            stopAnchors.add(stopsContainer.getChildAt(i));
        }
        return stopAnchors;
    }
}
