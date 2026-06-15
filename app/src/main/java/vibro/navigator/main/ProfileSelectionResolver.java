package vibro.navigator.main;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.brouter.BRouterProfilesRepository;

final class ProfileSelectionResolver {
    private ProfileSelectionResolver() {
    }

    @Nullable
    static ProfileSelection brouter(
            @NonNull Context context,
            @NonNull BRouterProfilesRepository profilesRepository,
            @Nullable String profileName
    ) {
        String cleanProfileName = clean(profileName);
        if (cleanProfileName == null) {
            Toast.makeText(context, R.string.msg_select_custom_profile, Toast.LENGTH_SHORT).show();
            return null;
        }
        return ProfileSelection.brouter(
                cleanProfileName,
                profilesRepository.getProfileParameterOverridesExtraParams(context, cleanProfileName)
        );
    }

    @Nullable
    private static String clean(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
