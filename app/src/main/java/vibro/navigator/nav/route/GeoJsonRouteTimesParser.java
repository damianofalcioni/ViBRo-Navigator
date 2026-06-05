package vibro.navigator.nav.route;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class GeoJsonRouteTimesParser {
    private GeoJsonRouteTimesParser() {
    }

    @NonNull
    static List<Double> parse(@Nullable JSONArray timesArray) {
        List<Double> out = new ArrayList<>();
        if (timesArray == null) {
            return out;
        }
        double previous = 0.0;
        for (int i = 0; i < timesArray.length(); i++) {
            double parsed = parseOptionalDouble(timesArray.opt(i));
            if (!isValidRouteTime(parsed, previous, i)) {
                return Collections.emptyList();
            }
            out.add(parsed);
            previous = parsed;
        }
        return out;
    }

    private static boolean isValidRouteTime(double value, double previous, int index) {
        return Double.isFinite(value) && value >= 0.0 && (index == 0 || value >= previous);
    }

    private static double parseOptionalDouble(@Nullable Object raw) {
        if (raw == null || JSONObject.NULL.equals(raw)) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
    }
}
