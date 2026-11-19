package vibro.navigator.brouter;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BRouterProfilesRepository {

    public static final String BROUTER_PACKAGE_NAME = "btools.routingapp";

    private static final String PREFS = "vibenavigator_brouter";
    private static final String KEY_CUSTOM_PROFILE_URI = "custom_profile_uri";
    private static final String KEY_CUSTOM_PROFILE_NAME = "custom_profile_name";
    static final String KEY_PROFILES_TREE_URI = "profiles_tree_uri";
    private static final String KEY_SELECTED_PROFILE = "selected_profile";
    private static final String TAG = "BRouterProfiles";

    @NonNull
    private final BRouterProfileDependencies dependencies;
    @NonNull
    private final BRouterProfileDirectories profileDirectories;
    @NonNull
    private final BRouterProfileLister profileLister;
    @NonNull
    private final BRouterProfileParameterSource parameterSource;
    @NonNull
    private final BRouterProfileParameterStore parameterStore;

    public BRouterProfilesRepository(@NonNull BRouterProfileDependencies dependencies) {
        this(
                dependencies,
                new BRouterProfileDirectories(dependencies.documentAccess, dependencies.storageVolumeAccess),
                new BRouterProfileLister(dependencies.documentAccess, dependencies.packageAccess),
                new BRouterProfileParameterSource(
                        dependencies.documentAccess,
                        dependencies.packageAccess,
                        new BRouterProfileParameterParser()
                ),
                new BRouterProfileParameterStore(PREFS)
        );
    }

    BRouterProfilesRepository(
            @NonNull BRouterProfileDependencies dependencies,
            @NonNull BRouterProfileDirectories profileDirectories,
            @NonNull BRouterProfileLister profileLister,
            @NonNull BRouterProfileParameterSource parameterSource,
            @NonNull BRouterProfileParameterStore parameterStore
    ) {
        this.dependencies = dependencies;
        this.profileDirectories = profileDirectories;
        this.profileLister = profileLister;
        this.parameterSource = parameterSource;
        this.parameterStore = parameterStore;
    }

    @Nullable
    public Uri getCustomProfileUri(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return storedUri(prefs, KEY_CUSTOM_PROFILE_URI);
    }

    @Nullable
    public String getCustomProfileName(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_CUSTOM_PROFILE_NAME, null);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return raw.trim();
    }

    public void saveCustomProfile(@NonNull Context context, @NonNull Uri documentUri, @NonNull String profileName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_CUSTOM_PROFILE_URI, documentUri.toString())
                .putString(KEY_CUSTOM_PROFILE_NAME, profileName)
                .apply();
        AppLogger.i(TAG, "Saved custom profile uri=" + documentUri + " profile=" + profileName);
    }

    @Nullable
    public Uri getProfilesTreeUri(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return storedUri(prefs, KEY_PROFILES_TREE_URI);
    }

    @Nullable
    private static Uri storedUri(SharedPreferences prefs, String key) {
        String value = prefs.getString(key, null);
        return value == null || value.trim().isEmpty() ? null : Uri.parse(value);
    }

    public void saveProfilesTreeUri(@NonNull Context context, @NonNull Uri treeUri) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_PROFILES_TREE_URI, treeUri.toString())
                .apply();
        AppLogger.i(TAG, "Saved profiles tree uri=" + treeUri);
    }

    @Nullable
    public String getSelectedProfileKey(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_SELECTED_PROFILE, null);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return raw.trim();
    }

    public void saveSelectedProfileKey(@NonNull Context context, @NonNull String selectionKey) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SELECTED_PROFILE, selectionKey).apply();
        AppLogger.i(TAG, "Saved selected profile key=" + selectionKey);
    }

    @NonNull
    public List<BRouterProfileParameter> getProfileParameters(
            @NonNull Context context,
            @Nullable String profileName
    ) {
        return getProfileParameters(context, profileName, java.util.Objects.equals(profileName, getCustomProfileName(context)));
    }

    @NonNull
    public List<BRouterProfileParameter> getProfileParameters(Context context, @Nullable String profileName, boolean custom) {
        List<BRouterProfileParameter> parameters = readResolvedProfileParameters(context, profileName, custom);
        return parameters == null ? Collections.emptyList() : parameters;
    }

    @Nullable
    public List<BRouterProfileParameter> readResolvedProfileParameters(Context context, @Nullable String profileName,
                                                                         boolean custom) {
        return parameterSource.readParameters(
                context,
                profileName,
                getCustomProfileUri(context),
                custom,
                () -> grantedProfileTrees(context),
                () -> detectedProfileTrees(context)
        );
    }

    @NonNull
    public Map<String, String> getProfileParameterValueOverrides(
            @NonNull Context context,
            @Nullable String profileName
    ) {
        String cleanName = BRouterProfileNames.clean(profileName);
        if (cleanName == null) {
            return Collections.emptyMap();
        }
        return parameterStore.getOverrides(context, cleanName);
    }

    @Nullable
    public String getProfileParameterOverridesExtraParams(
            @NonNull Context context,
            @Nullable String profileName
    ) {
        return BRouterProfileParameterValues.toExtraParams(
                getProfileParameterValueOverrides(context, profileName)
        );
    }

    public void saveProfileParameterValues(
            @NonNull Context context,
            @Nullable String profileName,
            @NonNull List<BRouterProfileParameter> parameters,
            @NonNull Map<String, String> values
    ) {
        String cleanName = BRouterProfileNames.clean(profileName);
        if (cleanName == null) {
            return;
        }
        parameterStore.saveValues(context, cleanName, parameters, values);
    }

    public void resetProfileParameterValues(@NonNull Context context, @Nullable String profileName) {
        String cleanName = BRouterProfileNames.clean(profileName);
        if (cleanName == null) {
            return;
        }
        parameterStore.reset(context, cleanName);
    }

    @Nullable
    public String getCustomProfileText(@NonNull Context context, @Nullable String profileName) {
        return BRouterCustomProfileSource.readText(
                context,
                dependencies.documentAccess,
                getCustomProfileUri(context),
                getCustomProfileName(context),
                profileName
        );
    }

    @Nullable
    public Uri getCustomProfilePickerInitialUri(@NonNull Context context) {
        Uri profilesTreeUri = getGrantedProfilesTreeUri(context);
        return profileDirectories.getCustomProfilePickerInitialUri(
                context,
                profilesTreeUri,
                getCustomProfileUri(context)
        );
    }

    @Nullable
    public Uri getProfilesTreePickerHint(@NonNull Context context, @Nullable Uri savedProfileHint) {
        Uri treeUri = getGrantedProfilesTreeUri(context);
        return profileDirectories.getProfilesTreePickerHint(
                context,
                treeUri,
                savedProfileHint
        );
    }

    public boolean hasPersistedProfilesTreeAccess(@NonNull Context context) {
        return getGrantedProfilesTreeUri(context) != null;
    }

    public boolean isBRouterInstalled(@NonNull Context context) {
        boolean installed = dependencies.packageAccess.isInstalled(context, BROUTER_PACKAGE_NAME);
        AppLogger.d(TAG, "BRouter installed=" + installed);
        return installed;
    }

    @NonNull
    public List<String> listProfiles(@NonNull Context context) {
        return listAvailableProfiles(context, listBundledProfiles(context), ignored -> { });
    }

    @NonNull
    public List<String> listBundledProfiles(Context context) {
        return BRouterProfileLister.mergeProfileNames(Collections.emptyList(), profileLister.listBundledProfiles(context));
    }

    @NonNull
    public List<String> listAvailableProfiles(Context context, List<String> bundled,
                                              androidx.core.util.Consumer<List<String>> progress) {
        List<Uri> granted = grantedProfileTrees(context);
        List<String> available = profileLister.listProfiles(context, granted, bundled, progress);
        List<Uri> detected = detectedProfileTrees(context);
        detected.removeAll(granted);
        return profileLister.listProfiles(context, detected, available, progress);
    }

    @NonNull
    private List<Uri> detectedProfileTrees(Context context) {
        return profileDirectories.resolveProfilesDiscoveryTreeUris(context, Collections.emptyList());
    }

    @NonNull
    private List<Uri> grantedProfileTrees(Context context) {
        return BRouterFolderGrant.discoveryTrees(context, getProfilesTreeUri(context), true,
                dependencies.uriPermissionAccess::hasReadPermission);
    }

    @Nullable
    public String normalizeProfileName(@Nullable String rawName) {
        return BRouterProfileLister.normalizeProfileName(rawName);
    }

    @Nullable
    private Uri getGrantedProfilesTreeUri(Context context) {
        return BRouterFolderGrant.resolve(context, getProfilesTreeUri(context), true,
                dependencies.uriPermissionAccess::hasReadPermission);
    }
}
