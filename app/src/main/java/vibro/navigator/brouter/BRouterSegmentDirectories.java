package vibro.navigator.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.logging.AppLogger;

final class BRouterSegmentDirectories {
    private static final String TAG = "BRouterSegments";

    @NonNull
    private final BRouterSegmentDependencies.DocumentAccess documentAccess;
    @NonNull
    private final BRouterSegmentDirectoryCandidates directoryCandidates;
    private final BRouterStreetProviderRead providerRead;

    BRouterSegmentDirectories(
            @NonNull BRouterSegmentDependencies.DocumentAccess documentAccess,
            @NonNull BRouterSegmentDependencies.StorageVolumeAccess storageVolumeAccess,
            BRouterStreetProviderRead providerRead
    ) {
        this.documentAccess = documentAccess;
        this.directoryCandidates = new BRouterSegmentDirectoryCandidates(storageVolumeAccess);
        this.providerRead = providerRead;
    }

    @NonNull
    List<Uri> resolveSegmentsDiscoveryTreeUris(
            @NonNull Context context,
            List<String> directoryIds
    ) {
        List<Uri> out = new ArrayList<>();
        for (String documentId : directoryIds) {
            Uri tree = documentAccess.buildExternalStorageTreeUri(documentId);
            if (Boolean.TRUE.equals(providerRead.run(tree,
                    () -> documentAccess.externalStorageDocumentExists(context, documentId))) && !out.contains(tree)) {
                out.add(tree);
            }
        }
        if (out.isEmpty()) {
            AppLogger.d(TAG, "No accessible BRouter segments4 path detected");
        }
        return out;
    }

    @NonNull
    List<String> getSegmentsDocumentIdCandidates(@NonNull Context context) {
        return directoryCandidates.getSegmentsDocumentIdCandidates(context);
    }

}
