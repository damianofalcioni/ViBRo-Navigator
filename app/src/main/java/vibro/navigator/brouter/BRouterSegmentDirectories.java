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

    BRouterSegmentDirectories(
            @NonNull BRouterSegmentDependencies.DocumentAccess documentAccess,
            @NonNull BRouterSegmentDependencies.StorageVolumeAccess storageVolumeAccess
    ) {
        this.documentAccess = documentAccess;
        this.directoryCandidates = new BRouterSegmentDirectoryCandidates(storageVolumeAccess);
    }

    @NonNull
    List<Uri> resolveSegmentsDiscoveryTreeUris(@NonNull Context context) {
        List<Uri> out = new ArrayList<>();
        for (String documentId : getSegmentsDocumentIdCandidates(context)) {
            if (documentAccess.externalStorageDocumentExists(context, documentId)) {
                addDiscoveryTreeUri(out, documentAccess.buildExternalStorageTreeUri(documentId));
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

    private void addDiscoveryTreeUri(@NonNull List<Uri> out, @NonNull Uri treeUri) {
        String candidate = treeUri.toString();
        for (Uri existing : out) {
            if (candidate.equals(existing.toString())) {
                return;
            }
        }
        out.add(treeUri);
    }
}
