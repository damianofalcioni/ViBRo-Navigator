package vibro.navigator.nav.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import vibro.navigator.R;
import vibro.navigator.nav.format.NavigationSpeedLimitFormatter;
import vibro.navigator.nav.model.NavState;

final class NavigationActivityTextScaling {
    private NavigationActivityTextScaling() {
    }

    static void configure(
            @NonNull TextView next,
            @NonNull TextView afterNext,
            @NonNull TextView gpsStatus,
            @NonNull TextView speedLimit
    ) {
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                next,
                10,
                22,
                1,
                TypedValue.COMPLEX_UNIT_SP
        );
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                afterNext,
                12,
                18,
                1,
                TypedValue.COMPLEX_UNIT_SP
        );
        gpsStatus.setMaxLines(1);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                gpsStatus,
                10,
                18,
                1,
                TypedValue.COMPLEX_UNIT_SP
        );
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                speedLimit,
                10,
                16,
                1,
                TypedValue.COMPLEX_UNIT_SP
        );
    }

    static void renderSpeedLimit(
            @NonNull Activity activity,
            @NonNull TextView speedLimit,
            @NonNull NavState state
    ) {
        if (state.routeStatus.speedLimit == null) {
            speedLimit.setVisibility(View.GONE);
            speedLimit.setText("");
            speedLimit.setContentDescription(null);
            return;
        }
        speedLimit.setText(NavigationSpeedLimitFormatter.formatBadge(state.routeStatus.speedLimit));
        speedLimit.setContentDescription(
                NavigationSpeedLimitFormatter.formatContentDescription(activity, state.routeStatus.speedLimit)
        );
        speedLimit.setVisibility(View.VISIBLE);
    }

    @NonNull
    static CharSequence styleGpsStatus(
            @NonNull Context context,
            @NonNull String statusText,
            @Nullable NavState state
    ) {
        SpannableString styledText = new SpannableString(statusText);
        if (state == null) {
            return styledText;
        }
        styleAccuracy(context, styledText, statusText, state);
        if (!NavigationSpeedLimitFormatter.isOverLimit(
                state.gpsStatus.telemetry.speedMps,
                state.routeStatus.speedLimit
        )) {
            return styledText;
        }
        int speedStart = statusText.indexOf(state.gpsStatus.telemetry.speedText);
        if (speedStart < 0) {
            return styledText;
        }
        int speedEnd = speedStart + state.gpsStatus.telemetry.speedText.length();
        styledText.setSpan(
                new StyleSpan(Typeface.BOLD),
                speedStart,
                speedEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return styledText;
    }

    private static void styleAccuracy(
            @NonNull Context context,
            @NonNull SpannableString styledText,
            @NonNull String statusText,
            @NonNull NavState state
    ) {
        int accuracyStart = accuracyStart(statusText, state);
        if (accuracyStart < 0) {
            return;
        }
        int accuracyEnd = accuracyStart + state.gpsStatus.telemetry.accuracyText.length();
        styledText.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(context, R.color.compass_accent)),
                accuracyStart,
                accuracyEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
    }

    private static int accuracyStart(@NonNull String statusText, @NonNull NavState state) {
        int compactStart = statusText.indexOf(state.gpsStatus.statusLine);
        if (compactStart < 0) {
            return -1;
        }
        int elevationStart = state.gpsStatus.statusLine.indexOf(state.gpsStatus.telemetry.elevationText);
        if (elevationStart < 0) {
            return -1;
        }
        int accuracyStart = state.gpsStatus.statusLine.indexOf(
                state.gpsStatus.telemetry.accuracyText,
                elevationStart + state.gpsStatus.telemetry.elevationText.length()
        );
        return accuracyStart < 0 ? -1 : compactStart + accuracyStart;
    }
}
