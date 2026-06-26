package vibro.navigator.brouter;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.List;

final class BRouterSegmentDirectoryCandidates {
    private static final String SEGMENTS_DIRECTORY = "segments4";

    @NonNull
    private final BRouterSegmentDependencies.StorageVolumeAccess storageVolumeAccess;

    BRouterSegmentDirectoryCandidates(@NonNull BRouterSegmentDependencies.StorageVolumeAccess storageVolumeAccess) {
        this.storageVolumeAccess = storageVolumeAccess;
    }

    @NonNull
    List<String> getSegmentsDocumentIdCandidates(@NonNull Context context) {
        return buildLikelyDocumentIdCandidates(storageVolumeAccess.secondaryStorageRootIds(context));
    }

    @NonNull
    static List<String> buildLikelyDocumentIdCandidates(@NonNull List<String> secondaryRootIds) {
        return BRouterDirectoryCandidates.buildLikelyDocumentIdCandidates(secondaryRootIds, SEGMENTS_DIRECTORY);
    }
}
