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
    static ProfileSelection selected(Context context, BRouterProfilesRepository repository,
                                     @Nullable ProfileSpinnerOption option, Runnable openPicker) {
        if (option == null) {
            Toast.makeText(context, R.string.msg_select_custom_profile, Toast.LENGTH_SHORT).show();
            return null;
        }
        if (!repository.isBRouterInstalled(context)) {
            Toast.makeText(context, R.string.msg_brouter_not_found, Toast.LENGTH_SHORT).show();
            return null;
        }
        if (!option.isCustom()) {
            return brouter(context, repository, option.profileName());
        }
        String customProfile = repository.getCustomProfileName(context);
        if (clean(customProfile) == null) {
            Toast.makeText(context, R.string.msg_select_custom_profile, Toast.LENGTH_SHORT).show();
            openPicker.run();
            return null;
        }
        return customBrouter(context, repository, customProfile);
    }

    @Nullable
    static ProfileSelection brouter(
            @NonNull Context context,
            @NonNull BRouterProfilesRepository profilesRepository,
            @Nullable String profileName
    ) {
        return brouter(context, profilesRepository, profileName, false);
    }

    @Nullable
    static ProfileSelection customBrouter(
            @NonNull Context context,
            @NonNull BRouterProfilesRepository profilesRepository,
            @Nullable String profileName
    ) {
        return brouter(context, profilesRepository, profileName, true);
    }

    @Nullable
    private static ProfileSelection brouter(
            @NonNull Context context,
            @NonNull BRouterProfilesRepository profilesRepository,
            @Nullable String profileName,
            boolean customProfile
    ) {
        String cleanProfileName = clean(profileName);
        if (cleanProfileName == null) {
            Toast.makeText(context, R.string.msg_select_custom_profile, Toast.LENGTH_SHORT).show();
            return null;
        }
        String profileParameters = profilesRepository.getProfileParameterOverridesExtraParams(context, cleanProfileName);
        return customProfile
                ? ProfileSelection.customBrouter(cleanProfileName, profileParameters)
                : ProfileSelection.brouter(cleanProfileName, profileParameters);
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
