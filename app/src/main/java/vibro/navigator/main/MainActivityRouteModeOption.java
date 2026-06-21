package vibro.navigator.main;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import vibro.navigator.R;
import vibro.navigator.nav.model.NavigationRoutingMode;

final class MainActivityRouteModeOption {
    private static final MainActivityRouteModeOption[] OPTIONS = new MainActivityRouteModeOption[]{
            new MainActivityRouteModeOption(NavigationRoutingMode.BROUTER, R.string.label_route_mode_route),
            new MainActivityRouteModeOption(NavigationRoutingMode.ROUND_TRIP, R.string.label_route_mode_round_trip),
            new MainActivityRouteModeOption(NavigationRoutingMode.STRAIGHT_LINE, R.string.label_route_mode_straight_line)
    };

    @NonNull
    private final NavigationRoutingMode mode;
    @StringRes
    private final int labelRes;

    private MainActivityRouteModeOption(@NonNull NavigationRoutingMode mode, @StringRes int labelRes) {
        this.mode = mode;
        this.labelRes = labelRes;
    }

    @NonNull
    static String[] labels(@NonNull Context context) {
        String[] labels = new String[OPTIONS.length];
        for (int i = 0; i < OPTIONS.length; i++) {
            labels[i] = context.getString(OPTIONS[i].labelRes);
        }
        return labels;
    }

    @NonNull
    static NavigationRoutingMode modeAt(int position) {
        if (position < 0 || position >= OPTIONS.length) {
            return NavigationRoutingMode.BROUTER;
        }
        return OPTIONS[position].mode;
    }

    static boolean isEnabled(int position, boolean brouterInstalled) {
        NavigationRoutingMode mode = modeAt(position);
        return brouterInstalled || mode == NavigationRoutingMode.STRAIGHT_LINE;
    }

    static int positionOf(@NonNull NavigationRoutingMode mode) {
        for (int i = 0; i < OPTIONS.length; i++) {
            if (OPTIONS[i].mode == mode) {
                return i;
            }
        }
        return 0;
    }
}
