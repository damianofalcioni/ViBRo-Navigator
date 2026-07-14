package vibro.navigator.poi;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PoiAddressLabel {
    private static final String[] PLACE_KEYS = {
            "amenity", "shop", "tourism", "leisure", "historic", "building",
            "attraction", "office", "railway", "aeroway", "natural",
            "man_made", "craft", "emergency", "healthcare", "name"
    };
    private static final String[] ROAD_KEYS = {
            "road", "pedestrian", "footway", "path"
    };
    private static final String[] LOCALITY_KEYS = {
            "city", "town", "village", "hamlet", "municipality", "suburb", "neighbourhood"
    };
    private static final String[] ADMINISTRATIVE_KEYS = {
            "country", "country_code", "postcode", "state", "region", "province", "county"
    };

    private PoiAddressLabel() {
    }

    @NonNull
    public static String conciseLabel(
            @NonNull String displayName,
            @NonNull Map<String, String> addressDetails
    ) {
        String fallback = displayName.trim();
        if (addressDetails.isEmpty()) {
            return fallback;
        }

        List<String> parts = new ArrayList<>();
        String streetAddress = streetAddress(addressDetails);
        String primary = primaryLabel(fallback, addressDetails, streetAddress);
        addUnique(parts, primary);
        addStreet(parts, streetAddress, primary, addressDetails);
        addUnique(parts, firstAddressValue(addressDetails, LOCALITY_KEYS));
        return parts.isEmpty() ? fallback : joinParts(parts);
    }

    @NonNull
    private static String primaryLabel(
            @NonNull String fallback,
            @NonNull Map<String, String> addressDetails,
            @NonNull String streetAddress
    ) {
        String primary = firstAddressValue(addressDetails, PLACE_KEYS);
        if (primary.isEmpty()) {
            primary = firstDisplayPart(fallback);
        }
        if (isAdministrativeValue(primary, addressDetails) || isStreetOnly(primary, addressDetails)) {
            return streetAddress;
        }
        return primary;
    }

    private static void addStreet(
            @NonNull List<String> parts,
            @NonNull String streetAddress,
            @NonNull String primary,
            @NonNull Map<String, String> addressDetails
    ) {
        if (!primaryAlreadyIncludesStreet(primary, addressDetails)) {
            addUnique(parts, streetAddress);
        }
    }

    private static boolean isStreetOnly(
            @NonNull String value,
            @NonNull Map<String, String> addressDetails
    ) {
        String houseNumber = addressValue(addressDetails, "house_number");
        if (!houseNumber.isEmpty() && normalizedEquals(value, houseNumber)) {
            return true;
        }
        return normalizedEquals(value, firstAddressValue(addressDetails, ROAD_KEYS));
    }

    private static boolean primaryAlreadyIncludesStreet(
            @NonNull String primary,
            @NonNull Map<String, String> addressDetails
    ) {
        String road = firstAddressValue(addressDetails, ROAD_KEYS);
        String houseNumber = addressValue(addressDetails, "house_number");
        return containsNormalized(primary, road)
                && (houseNumber.isEmpty() || containsNormalized(primary, houseNumber));
    }

    @NonNull
    private static String streetAddress(@NonNull Map<String, String> addressDetails) {
        String road = firstAddressValue(addressDetails, ROAD_KEYS);
        String houseNumber = addressValue(addressDetails, "house_number");
        if (road.isEmpty()) {
            return houseNumber;
        }
        return houseNumber.isEmpty() ? road : road + " " + houseNumber;
    }

    @NonNull
    private static String firstAddressValue(
            @NonNull Map<String, String> addressDetails,
            @NonNull String[] keys
    ) {
        for (String key : keys) {
            String value = addressValue(addressDetails, key);
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    @NonNull
    private static String addressValue(@NonNull Map<String, String> addressDetails, @NonNull String key) {
        String value = addressDetails.get(key);
        return value == null ? "" : value.trim();
    }

    @NonNull
    private static String firstDisplayPart(@NonNull String displayName) {
        int comma = displayName.indexOf(',');
        String part = comma >= 0 ? displayName.substring(0, comma) : displayName;
        return part.trim();
    }

    private static boolean isAdministrativeValue(
            @NonNull String value,
            @NonNull Map<String, String> addressDetails
    ) {
        for (String key : ADMINISTRATIVE_KEYS) {
            if (normalizedEquals(value, addressValue(addressDetails, key))) {
                return true;
            }
        }
        return false;
    }

    private static void addUnique(@NonNull List<String> parts, @NonNull String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty() || containsPart(parts, trimmed)) {
            return;
        }
        parts.add(trimmed);
    }

    private static boolean containsPart(@NonNull List<String> parts, @NonNull String value) {
        for (String part : parts) {
            if (normalizedEquals(part, value)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private static String joinParts(@NonNull List<String> parts) {
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(part);
        }
        return out.toString();
    }

    private static boolean normalizedEquals(@NonNull String left, @NonNull String right) {
        return normalize(left).equals(normalize(right));
    }

    private static boolean containsNormalized(@NonNull String outer, @NonNull String inner) {
        String normalizedInner = normalize(inner);
        return !normalizedInner.isEmpty() && normalize(outer).contains(normalizedInner);
    }

    @NonNull
    private static String normalize(@NonNull String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
