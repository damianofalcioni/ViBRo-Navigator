package vibro.navigator.main;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import vibro.navigator.R;
import vibro.navigator.nav.model.NavigationRoutingMode;

final class MainActivityRouteModeOption {
    private static final MainActivityRouteModeOption[] OPTIONS = new MainActivityRouteModeOption[]{
            new MainActivityRouteModeOption(
                    NavigationRoutingMode.BROUTER,
                    R.string.label_route_mode_route,
                    R.string.route_mode_info_route_description
            ),
            new MainActivityRouteModeOption(
                    NavigationRoutingMode.ROUND_TRIP,
                    R.string.label_route_mode_round_trip,
                    R.string.route_mode_info_round_trip_description
            ),
            new MainActivityRouteModeOption(
                    NavigationRoutingMode.STRAIGHT_LINE,
                    R.string.label_route_mode_straight_line,
                    R.string.route_mode_info_straight_line_description
            )
    };

    @NonNull
    private final NavigationRoutingMode mode;
    @StringRes
    private final int labelRes;
    @StringRes
    private final int descriptionRes;

    private MainActivityRouteModeOption(
            @NonNull NavigationRoutingMode mode,
            @StringRes int labelRes,
            @StringRes int descriptionRes
    ) {
        this.mode = mode;
        this.labelRes = labelRes;
        this.descriptionRes = descriptionRes;
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

    @StringRes
    static int labelResAt(int position) {
        return optionAt(position).labelRes;
    }

    @StringRes
    static int descriptionResAt(int position) {
        return optionAt(position).descriptionRes;
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

    @NonNull
    private static MainActivityRouteModeOption optionAt(int position) {
        if (position < 0 || position >= OPTIONS.length) {
            return OPTIONS[0];
        }
        return OPTIONS[position];
    }
}
