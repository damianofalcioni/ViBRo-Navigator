package vibro.navigator.nav.route;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GeoJsonRouteSpeedLimitParser {
    private static final String DISTANCE_COLUMN = "Distance";
    private static final String WAY_TAGS_COLUMN = "WayTags";
    private static final String DEFAULT_DISTANCE_METERS = "0";
    private static final String SPEED_LIMIT_UNIT_PATTERN = "mph|km/h|kph";
    private static final String MPH_UNIT = "mph";
    private static final Pattern MAXSPEED_TAG_PATTERN = Pattern.compile(
            "(?:^|\\s)maxspeed=([^\\s]+)(?:\\s+(" + SPEED_LIMIT_UNIT_PATTERN + "))?(?:\\s|$)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern MAXSPEED_VALUE_PATTERN = Pattern.compile(
            "([0-9]{1,3})(?:\\.0+)?(?:\\s*(" + SPEED_LIMIT_UNIT_PATTERN + "))?",
            Pattern.CASE_INSENSITIVE
    );

    private GeoJsonRouteSpeedLimitParser() {
    }

    @NonNull
    static List<RouteSpeedLimitSegment> parse(@Nullable JSONArray messages) {
        if (messages == null || messages.length() < 2) {
            return new ArrayList<>();
        }
        MessageColumns columns = MessageColumns.from(messages.optJSONArray(0));
        return columns.isUsable() ? parseRows(messages, columns) : new ArrayList<>();
    }

    @NonNull
    private static List<RouteSpeedLimitSegment> parseRows(
            @NonNull JSONArray messages,
            @NonNull MessageColumns columns
    ) {
        List<RouteSpeedLimitSegment> out = new ArrayList<>();
        double cursorMeters = 0.0;
        for (int i = 1; i < messages.length(); i++) {
            cursorMeters = appendRowSegment(out, cursorMeters, messages.optJSONArray(i), columns);
        }
        return out;
    }

    private static double appendRowSegment(
            @NonNull List<RouteSpeedLimitSegment> out,
            double cursorMeters,
            @Nullable JSONArray row,
            @NonNull MessageColumns columns
    ) {
        if (row == null) {
            return cursorMeters;
        }
        double endMeters = cursorMeters + Math.max(0.0, parseDouble(row.optString(
                columns.distanceIndex,
                DEFAULT_DISTANCE_METERS
        )));
        RouteSpeedLimit speedLimit = parseSpeedLimit(row.optString(columns.wayTagsIndex, ""));
        if (speedLimit != null && endMeters > cursorMeters) {
            out.add(new RouteSpeedLimitSegment(cursorMeters, endMeters, speedLimit));
        }
        return endMeters;
    }

    @Nullable
    private static RouteSpeedLimit parseSpeedLimit(@NonNull String wayTags) {
        Matcher tagMatcher = MAXSPEED_TAG_PATTERN.matcher(wayTags);
        if (!tagMatcher.find()) {
            return null;
        }
        String normalizedValue = tagMatcher.group(1).replace('_', ' ').toLowerCase(Locale.US);
        Matcher valueMatcher = MAXSPEED_VALUE_PATTERN.matcher(normalizedValue);
        if (!valueMatcher.matches()) {
            return null;
        }
        int value = (int) parseDouble(valueMatcher.group(1));
        if (value <= 0) {
            return null;
        }
        String unit = tagMatcher.group(2) == null ? valueMatcher.group(2) : tagMatcher.group(2);
        return new RouteSpeedLimit(value, parseSpeedLimitUnit(unit));
    }

    @NonNull
    private static RouteSpeedLimit.Unit parseSpeedLimitUnit(@Nullable String unit) {
        return unit != null && MPH_UNIT.equals(unit.toLowerCase(Locale.US))
                ? RouteSpeedLimit.Unit.MILES_PER_HOUR
                : RouteSpeedLimit.Unit.KILOMETERS_PER_HOUR;
    }

    private static double parseDouble(@NonNull String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private static final class MessageColumns {
        private static final int MISSING_INDEX = -1;

        private final int distanceIndex;
        private final int wayTagsIndex;

        private MessageColumns(int distanceIndex, int wayTagsIndex) {
            this.distanceIndex = distanceIndex;
            this.wayTagsIndex = wayTagsIndex;
        }

        @NonNull
        static MessageColumns from(@Nullable JSONArray header) {
            if (header == null) {
                return new MessageColumns(MISSING_INDEX, MISSING_INDEX);
            }
            return parseHeader(header);
        }

        @NonNull
        private static MessageColumns parseHeader(@NonNull JSONArray header) {
            int distanceIndex = MISSING_INDEX;
            int wayTagsIndex = MISSING_INDEX;
            for (int i = 0; i < header.length(); i++) {
                String column = header.optString(i, "");
                if (DISTANCE_COLUMN.equals(column)) {
                    distanceIndex = i;
                } else if (WAY_TAGS_COLUMN.equals(column)) {
                    wayTagsIndex = i;
                }
            }
            return new MessageColumns(distanceIndex, wayTagsIndex);
        }

        boolean isUsable() {
            return distanceIndex >= 0 && wayTagsIndex >= 0;
        }
    }
}
