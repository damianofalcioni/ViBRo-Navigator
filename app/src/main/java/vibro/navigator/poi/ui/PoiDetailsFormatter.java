package vibro.navigator.poi.ui;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.TreeMap;

import vibro.navigator.R;
import vibro.navigator.poi.PoiDetails;

final class PoiDetailsFormatter {
    private PoiDetailsFormatter() {
    }

    @NonNull
    static String format(@NonNull Context context, @NonNull PoiDetails details) {
        StringBuilder out = new StringBuilder();
        appendEntranceSection(context, out, details);
        appendMapSection(context, out, R.string.label_poi_extra_tags, details.extraTags());
        appendMapSection(context, out, R.string.label_poi_address_details, details.addressDetails());
        appendEntrancesSection(context, out, details);
        if (out.length() == 0) {
            out.append(context.getString(R.string.msg_poi_details_unavailable));
        }
        appendMapCheckHint(context, out);
        return out.toString();
    }

    private static void appendMapCheckHint(
            @NonNull Context context,
            @NonNull StringBuilder out
    ) {
        if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') {
            out.append('\n');
        }
        out.append('\n').append(context.getString(R.string.msg_poi_details_map_check_hint));
    }

    private static void appendEntranceSection(
            @NonNull Context context,
            @NonNull StringBuilder out,
            @NonNull PoiDetails details
    ) {
        String type = details.entranceType();
        if (!details.isEntrance() || type == null || type.trim().isEmpty()) {
            return;
        }
        appendSectionHeader(context, out, R.string.label_poi_entrance);
        appendPair(context, out, context.getString(R.string.label_poi_entrance_type), type);
    }

    private static void appendEntrancesSection(
            @NonNull Context context,
            @NonNull StringBuilder out,
            @NonNull PoiDetails details
    ) {
        if (details.entrances().isEmpty()) {
            return;
        }
        appendSectionHeader(context, out, R.string.label_poi_entrances);
        int index = 1;
        for (PoiDetails.Entrance entrance : details.entrances()) {
            appendEntranceDetails(context, out, entrance, index);
            index++;
        }
    }

    private static void appendEntranceDetails(
            @NonNull Context context,
            @NonNull StringBuilder out,
            @NonNull PoiDetails.Entrance entrance,
            int index
    ) {
        if (index > 1) {
            out.append('\n');
        }
        out.append(context.getString(R.string.format_poi_entrance_heading, index)).append('\n');
        if (!entrance.type().trim().isEmpty()) {
            appendPair(context, out, context.getString(R.string.label_poi_entrance_type), entrance.type());
        }
        appendPair(context, out, context.getString(R.string.label_poi_coordinates),
                context.getString(R.string.format_coordinates, entrance.lat, entrance.lon));
        appendEntranceExtraTags(context, out, entrance);
    }

    private static void appendEntranceExtraTags(
            @NonNull Context context,
            @NonNull StringBuilder out,
            @NonNull PoiDetails.Entrance entrance
    ) {
        for (Map.Entry<String, String> entry : new TreeMap<>(entrance.extraTags()).entrySet()) {
            appendPair(context, out, entry.getKey(), entry.getValue());
        }
    }

    private static void appendMapSection(
            @NonNull Context context,
            @NonNull StringBuilder out,
            int titleRes,
            @NonNull Map<String, String> values
    ) {
        if (values.isEmpty()) {
            return;
        }
        appendSectionHeader(context, out, titleRes);
        for (Map.Entry<String, String> entry : new TreeMap<>(values).entrySet()) {
            appendPair(context, out, entry.getKey(), entry.getValue());
        }
    }

    private static void appendSectionHeader(
            @NonNull Context context,
            @NonNull StringBuilder out,
            int titleRes
    ) {
        if (out.length() > 0) {
            out.append('\n');
        }
        out.append(context.getString(titleRes)).append('\n');
    }

    private static void appendPair(
            @NonNull Context context,
            @NonNull StringBuilder out,
            @NonNull String key,
            @NonNull String value
    ) {
        out.append(context.getString(R.string.format_poi_detail_pair, key, value)).append('\n');
    }
}
