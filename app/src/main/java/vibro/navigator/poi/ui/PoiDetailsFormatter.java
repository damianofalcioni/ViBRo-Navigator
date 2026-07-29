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
        return format(PoiTextResources.from(context), details);
    }

    @NonNull
    static String format(@NonNull PoiTextResources textResources, @NonNull PoiDetails details) {
        StringBuilder out = new StringBuilder();
        appendEntranceSection(textResources, out, details);
        appendMapSection(textResources, out, R.string.label_poi_extra_tags, details.extraTags());
        appendMapSection(textResources, out, R.string.label_poi_address_details, details.addressDetails());
        appendEntrancesSection(textResources, out, details);
        if (out.length() == 0) {
            out.append(textResources.getString(R.string.msg_poi_details_unavailable));
        }
        appendMapCheckHint(textResources, out);
        return out.toString();
    }

    private static void appendMapCheckHint(
            @NonNull PoiTextResources textResources,
            @NonNull StringBuilder out
    ) {
        if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') {
            out.append('\n');
        }
        out.append('\n').append(textResources.getString(R.string.msg_poi_details_map_check_hint));
    }

    private static void appendEntranceSection(
            @NonNull PoiTextResources textResources,
            @NonNull StringBuilder out,
            @NonNull PoiDetails details
    ) {
        String type = details.entranceType();
        if (!details.isEntrance() || type == null || type.trim().isEmpty()) {
            return;
        }
        appendSectionHeader(textResources, out, R.string.label_poi_entrance);
        appendPair(textResources, out, textResources.getString(R.string.label_poi_entrance_type), type);
    }

    private static void appendEntrancesSection(
            @NonNull PoiTextResources textResources,
            @NonNull StringBuilder out,
            @NonNull PoiDetails details
    ) {
        if (details.entrances().isEmpty()) {
            return;
        }
        appendSectionHeader(textResources, out, R.string.label_poi_entrances);
        int index = 1;
        for (PoiDetails.Entrance entrance : details.entrances()) {
            appendEntranceDetails(textResources, out, entrance, index);
            index++;
        }
    }

    private static void appendEntranceDetails(
            @NonNull PoiTextResources textResources,
            @NonNull StringBuilder out,
            @NonNull PoiDetails.Entrance entrance,
            int index
    ) {
        if (index > 1) {
            out.append('\n');
        }
        out.append(textResources.getString(R.string.format_poi_entrance_heading, index)).append('\n');
        if (!entrance.type().trim().isEmpty()) {
            appendPair(textResources, out, textResources.getString(R.string.label_poi_entrance_type), entrance.type());
        }
        appendPair(textResources, out, textResources.getString(R.string.label_poi_coordinates),
                textResources.getString(R.string.format_coordinates, entrance.lat, entrance.lon));
        appendEntranceExtraTags(textResources, out, entrance);
    }

    private static void appendEntranceExtraTags(
            @NonNull PoiTextResources textResources,
            @NonNull StringBuilder out,
            @NonNull PoiDetails.Entrance entrance
    ) {
        for (Map.Entry<String, String> entry : new TreeMap<>(entrance.extraTags()).entrySet()) {
            appendPair(textResources, out, entry.getKey(), entry.getValue());
        }
    }

    private static void appendMapSection(
            @NonNull PoiTextResources textResources,
            @NonNull StringBuilder out,
            int titleRes,
            @NonNull Map<String, String> values
    ) {
        if (values.isEmpty()) {
            return;
        }
        appendSectionHeader(textResources, out, titleRes);
        for (Map.Entry<String, String> entry : new TreeMap<>(values).entrySet()) {
            appendPair(textResources, out, entry.getKey(), entry.getValue());
        }
    }

    private static void appendSectionHeader(
            @NonNull PoiTextResources textResources,
            @NonNull StringBuilder out,
            int titleRes
    ) {
        if (out.length() > 0) {
            out.append('\n');
        }
        out.append(textResources.getString(titleRes)).append('\n');
    }

    private static void appendPair(
            @NonNull PoiTextResources textResources,
            @NonNull StringBuilder out,
            @NonNull String key,
            @NonNull String value
    ) {
        out.append(textResources.getString(R.string.format_poi_detail_pair, key, value)).append('\n');
    }
}
