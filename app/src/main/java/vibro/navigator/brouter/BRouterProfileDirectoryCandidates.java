package vibro.navigator.brouter;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.List;

final class BRouterProfileDirectoryCandidates {
    private static final String PROFILES_DIRECTORY = "profiles2";

    @NonNull
    private final BRouterProfileDependencies.StorageVolumeAccess storageVolumeAccess;

    BRouterProfileDirectoryCandidates(@NonNull BRouterProfileDependencies.StorageVolumeAccess storageVolumeAccess) {
        this.storageVolumeAccess = storageVolumeAccess;
    }

    @NonNull
    List<String> getProfilesDocumentIdCandidates(@NonNull Context context) {
        return buildLikelyPickerDocumentIdCandidates(getSecondaryStorageRootIds(context));
    }

    @NonNull
    List<String> getSecondaryStorageRootIds(@NonNull Context context) {
        return storageVolumeAccess.secondaryStorageRootIds(context);
    }

    @NonNull
    static List<String> buildLikelyPickerDocumentIdCandidates(@NonNull List<String> secondaryRootIds) {
        return BRouterDirectoryCandidates.buildLikelyDocumentIdCandidates(secondaryRootIds, PROFILES_DIRECTORY);
    }

    @NonNull
    static List<String> buildFallbackPickerDocumentIdCandidates(@NonNull List<String> secondaryRootIds) {
        return BRouterDirectoryCandidates.buildFallbackPickerDocumentIdCandidates(
                secondaryRootIds,
                PROFILES_DIRECTORY
        );
    }
}
