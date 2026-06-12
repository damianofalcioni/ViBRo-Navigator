package vibro.navigator.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import vibro.navigator.R;

final class BundledProfileInfo {

    private static final BundledProfileInfo[] PROFILES = new BundledProfileInfo[]{
            profile("trekking", R.string.profile_info_trekking_title,
                    R.string.profile_info_trekking_description),
            profile("trekking-ignore-cr", R.string.profile_info_trekking_ignore_cr_title,
                    R.string.profile_info_trekking_ignore_cr_description),
            profile("trekking-noferries", R.string.profile_info_trekking_noferries_title,
                    R.string.profile_info_trekking_noferries_description),
            profile("trekking-nosteps", R.string.profile_info_trekking_nosteps_title,
                    R.string.profile_info_trekking_nosteps_description),
            profile("trekking-steep", R.string.profile_info_trekking_steep_title,
                    R.string.profile_info_trekking_steep_description),
            profile("fastbike", R.string.profile_info_fastbike_title,
                    R.string.profile_info_fastbike_description),
            profile("fastbike-lowtraffic", R.string.profile_info_fastbike_lowtraffic_title,
                    R.string.profile_info_fastbike_lowtraffic_description),
            profile("fastbike-verylowtraffic", R.string.profile_info_fastbike_verylowtraffic_title,
                    R.string.profile_info_fastbike_verylowtraffic_description),
            profile("fastbike-asia-pacific", R.string.profile_info_fastbike_asia_pacific_title,
                    R.string.profile_info_fastbike_asia_pacific_description),
            profile("safety", R.string.profile_info_safety_title,
                    R.string.profile_info_safety_description),
            profile("gravel", R.string.profile_info_gravel_title,
                    R.string.profile_info_gravel_description),
            profile("mtb", R.string.profile_info_mtb_title,
                    R.string.profile_info_mtb_description),
            profile("hiking-mountain", R.string.profile_info_hiking_mountain_title,
                    R.string.profile_info_hiking_mountain_description),
            profile("skating", R.string.profile_info_skating_title,
                    R.string.profile_info_skating_description),
            profile("shortest", R.string.profile_info_shortest_title,
                    R.string.profile_info_shortest_description),
            profile("car-eco", R.string.profile_info_car_eco_title,
                    R.string.profile_info_car_eco_description),
            profile("car-eco-nocost", R.string.profile_info_car_eco_nocost_title,
                    R.string.profile_info_car_eco_nocost_description),
            profile("car-fast", R.string.profile_info_car_fast_title,
                    R.string.profile_info_car_fast_description),
            profile("car-fast-nocost", R.string.profile_info_car_fast_nocost_title,
                    R.string.profile_info_car_fast_nocost_description),
            profile("car-vario", R.string.profile_info_car_vario_title,
                    R.string.profile_info_car_vario_description),
            profile("car-vario-nocost", R.string.profile_info_car_vario_nocost_title,
                    R.string.profile_info_car_vario_nocost_description),
            profile("vm-forum-liegerad-schnell", R.string.profile_info_vm_forum_liegerad_schnell_title,
                    R.string.profile_info_vm_forum_liegerad_schnell_description),
            profile("vm-forum-velomobil-schnell", R.string.profile_info_vm_forum_velomobil_schnell_title,
                    R.string.profile_info_vm_forum_velomobil_schnell_description),
            attentionProfile("car-eco-de", R.string.profile_info_car_eco_de_title,
                    R.string.profile_info_car_eco_de_description),
            attentionProfile("moped", R.string.profile_info_moped_title,
                    R.string.profile_info_moped_description),
            attentionProfile("dummy", R.string.profile_info_dummy_title,
                    R.string.profile_info_dummy_description),
            attentionProfile("rail", R.string.profile_info_rail_title,
                    R.string.profile_info_rail_description),
            attentionProfile("river", R.string.profile_info_river_title,
                    R.string.profile_info_river_description)
    };
    private static final BundledProfileInfo STRAIGHT_LINE = profile(
            ProfileSpinnerOption.STRAIGHT_LINE_KEY,
            R.string.profile_info_straight_line_title,
            R.string.profile_info_straight_line_description
    );

    @NonNull
    private final String profileName;
    @StringRes
    private final int titleRes;
    @StringRes
    private final int descriptionRes;
    private final boolean attention;

    private BundledProfileInfo(
            @NonNull String profileName,
            @StringRes int titleRes,
            @StringRes int descriptionRes,
            boolean attention
    ) {
        this.profileName = profileName;
        this.titleRes = titleRes;
        this.descriptionRes = descriptionRes;
        this.attention = attention;
    }

    @Nullable
    static BundledProfileInfo forProfile(@Nullable String profileName) {
        if (profileName == null) {
            return null;
        }
        String normalized = profileName.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        for (BundledProfileInfo profile : PROFILES) {
            if (profile.profileName.equals(normalized)) {
                return profile;
            }
        }
        return null;
    }

    @NonNull
    static BundledProfileInfo straightLine() {
        return STRAIGHT_LINE;
    }

    @StringRes
    int titleRes() {
        return titleRes;
    }

    @StringRes
    int descriptionRes() {
        return descriptionRes;
    }

    boolean hasAttentionIcon() {
        return attention;
    }

    @NonNull
    private static BundledProfileInfo profile(
            @NonNull String profileName,
            @StringRes int titleRes,
            @StringRes int descriptionRes
    ) {
        return new BundledProfileInfo(profileName, titleRes, descriptionRes, false);
    }

    @NonNull
    private static BundledProfileInfo attentionProfile(
            @NonNull String profileName,
            @StringRes int titleRes,
            @StringRes int descriptionRes
    ) {
        return new BundledProfileInfo(profileName, titleRes, descriptionRes, true);
    }
}
