package vibro.navigator.poi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PoiDetails {
    @NonNull
    private final Map<String, String> addressDetails;
    @NonNull
    private final Map<String, String> extraTags;
    @NonNull
    private final List<Entrance> entrances;
    @Nullable
    private final String parentName;
    @Nullable
    private final String entranceType;

    public PoiDetails(
            @NonNull Map<String, String> addressDetails,
            @NonNull Map<String, String> extraTags
    ) {
        this(addressDetails, extraTags, Collections.emptyList());
    }

    public PoiDetails(
            @NonNull Map<String, String> addressDetails,
            @NonNull Map<String, String> extraTags,
            @NonNull List<Entrance> entrances
    ) {
        this(addressDetails, extraTags, entrances, null, null);
    }

    public PoiDetails(
            @NonNull Map<String, String> addressDetails,
            @NonNull Map<String, String> extraTags,
            @Nullable String parentName,
            @Nullable String entranceType
    ) {
        this(addressDetails, extraTags, Collections.emptyList(), parentName, entranceType);
    }

    private PoiDetails(
            @NonNull Map<String, String> addressDetails,
            @NonNull Map<String, String> extraTags,
            @NonNull List<Entrance> entrances,
            @Nullable String parentName,
            @Nullable String entranceType
    ) {
        this.addressDetails = immutableCopy(addressDetails);
        this.extraTags = immutableCopy(extraTags);
        this.entrances = immutableEntranceCopy(entrances);
        this.parentName = parentName;
        this.entranceType = entranceType;
    }

    @NonNull
    public Map<String, String> addressDetails() {
        return addressDetails;
    }

    @NonNull
    public Map<String, String> extraTags() {
        return extraTags;
    }

    @NonNull
    public List<Entrance> entrances() {
        return entrances;
    }

    @Nullable
    public String parentName() {
        return parentName;
    }

    @Nullable
    public String entranceType() {
        return entranceType;
    }

    public boolean hasExtraTags() {
        return !extraTags.isEmpty();
    }

    public boolean hasEntrances() {
        return !entrances.isEmpty();
    }

    public boolean isEntrance() {
        return parentName != null;
    }

    @NonNull
    private static Map<String, String> immutableCopy(@NonNull Map<String, String> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    @NonNull
    private static List<Entrance> immutableEntranceCopy(@NonNull List<Entrance> source) {
        return Collections.unmodifiableList(new ArrayList<>(source));
    }

    public static final class Entrance {
        public final double lat;
        public final double lon;
        @NonNull
        private final String type;
        @NonNull
        private final Map<String, String> extraTags;

        public Entrance(
                double lat,
                double lon,
                @NonNull String type,
                @NonNull Map<String, String> extraTags
        ) {
            this.lat = lat;
            this.lon = lon;
            this.type = type;
            this.extraTags = immutableCopy(extraTags);
        }

        @NonNull
        public String type() {
            return type;
        }

        @NonNull
        public Map<String, String> extraTags() {
            return extraTags;
        }
    }
}
