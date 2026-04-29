package vibro.navigator;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

final class ProfileSpinnerOptions {

    private final List<String> profiles = new ArrayList<>();
    private final List<ProfileSpinnerOption> options = new ArrayList<>();

    void replace(
            @NonNull Context context,
            @NonNull List<String> newProfiles,
            @Nullable String customProfile
    ) {
        profiles.clear();
        profiles.addAll(newProfiles);
        options.clear();
        for (String profile : profiles) {
            options.add(new ProfileSpinnerOption(profile, profile, false));
        }
        options.add(buildCustomOption(context, customProfile));
    }

    @NonNull
    List<ProfileSpinnerOption> all() {
        return options;
    }

    int size() {
        return options.size();
    }

    @NonNull
    List<String> profilesForLog() {
        return profiles;
    }

    @Nullable
    ProfileSpinnerOption optionAt(int position) {
        if (position < 0 || position >= options.size()) {
            return null;
        }
        return options.get(position);
    }

    int restoredPosition(@Nullable String selectionKey, @Nullable String customProfile) {
        int target = findPosition(selectionKey);
        if (target < 0 && profiles.isEmpty() && customProfile != null) {
            target = findCustomPosition();
        }
        return target < 0 ? 0 : target;
    }

    int firstRegularOrCustomPosition() {
        if (!profiles.isEmpty()) {
            return 0;
        }
        return findCustomPosition();
    }

    int findCustomPosition() {
        return findPosition(ProfileSpinnerOption.CUSTOM_KEY);
    }

    private int findPosition(@Nullable String selectionKey) {
        if (selectionKey == null) {
            return -1;
        }
        for (int i = 0; i < options.size(); i++) {
            if (selectionKey.equals(options.get(i).selectionKey())) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    private static ProfileSpinnerOption buildCustomOption(
            @NonNull Context context,
            @Nullable String customProfile
    ) {
        if (customProfile == null || customProfile.trim().isEmpty()) {
            return new ProfileSpinnerOption(context.getString(R.string.label_vehicle_profile_custom), null, true);
        }
        return new ProfileSpinnerOption(
                context.getString(R.string.label_vehicle_profile_custom_with_name, customProfile),
                customProfile,
                true
        );
    }
}
