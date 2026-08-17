package vibro.navigator.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    @Nullable
    Uri getSegmentsTreePickerInitialUri(
            @NonNull Context context,
            @Nullable Uri savedTreeUri,
            boolean hasSavedTreeReadPermission
    ) {
        if (hasSavedTreeReadPermission && savedTreeUri != null) {
            return savedTreeUri;
        }
        String existing = findExistingSegmentsDocumentId(context);
        if (existing != null) {
            return documentAccess.buildExternalStorageDocumentUri(existing);
        }
        List<String> candidates = getSegmentsDocumentIdCandidates(context);
        if (candidates.isEmpty()) {
            return null;
        }
        String fallback = candidates.get(0);
        AppLogger.d(TAG, "Using fallback BRouter segments4 picker path documentId=" + fallback);
        return documentAccess.buildExternalStorageDocumentUri(fallback);
    }

    @NonNull
    List<Uri> resolveSegmentsDiscoveryTreeUris(
            @NonNull Context context,
            @Nullable Uri savedTreeUri,
            boolean hasSavedTreeReadPermission
    ) {
        List<Uri> out = new ArrayList<>();
        if (hasSavedTreeReadPermission && savedTreeUri != null) {
            addDiscoveryTreeUri(out, savedTreeUri);
        }
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

    @Nullable
    private String findExistingSegmentsDocumentId(@NonNull Context context) {
        for (String documentId : getSegmentsDocumentIdCandidates(context)) {
            if (documentAccess.externalStorageDocumentExists(context, documentId)) {
                AppLogger.d(TAG, "Using detected BRouter segments4 path documentId=" + documentId);
                return documentId;
            }
        }
        AppLogger.d(TAG, "No accessible BRouter segments4 path detected");
        return null;
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
