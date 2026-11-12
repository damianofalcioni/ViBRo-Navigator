package vibro.navigator.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;

import java.util.ArrayList;
import java.util.List;

final class BRouterProfileDirectories {
    private static final String TAG = "BRouterProfiles";

    @NonNull
    private final BRouterProfileDependencies.DocumentAccess documentAccess;
    @NonNull
    private final BRouterProfileDirectoryCandidates directoryCandidates;

    BRouterProfileDirectories(
            @NonNull BRouterProfileDependencies.DocumentAccess documentAccess,
            @NonNull BRouterProfileDependencies.StorageVolumeAccess storageVolumeAccess
    ) {
        this.documentAccess = documentAccess;
        this.directoryCandidates = new BRouterProfileDirectoryCandidates(storageVolumeAccess);
    }

    @Nullable
    Uri getCustomProfilePickerInitialUri(
            @NonNull Context context,
            @Nullable Uri profilesTreeUri,
            boolean hasProfilesTreeReadPermission,
            @Nullable Uri customProfileUri
    ) {
        if (hasProfilesTreeReadPermission) {
            return initialUriForTree(profilesTreeUri);
        }
        if (customProfileUri != null) {
            Uri parentUri = toParentDocumentUri(customProfileUri);
            return parentUri != null ? parentUri : customProfileUri;
        }
        String existing = findExistingProfilesDocumentId(context);
        return existing != null ? buildExternalStorageDocumentUri(existing) : fallbackPickerUri(context);
    }

    @Nullable
    Uri getProfilesTreePickerHint(
            @NonNull Context context,
            @Nullable Uri persistedTreeUri,
            @Nullable Uri savedProfileHint
    ) {
        if (persistedTreeUri != null) {
            return initialUriForTree(persistedTreeUri);
        }
        // A file grant may identify the folder even when probing cannot read its parent.
        if (savedProfileHint != null) {
            String parent = toParentDocumentId(savedProfileHint);
            if (parent != null && getProfilesDocumentIdCandidates(context).contains(parent)) {
                return buildExternalStorageDocumentUri(parent);
            }
        }
        return fallbackPickerUri(context);
    }

    @Nullable
    private Uri initialUriForTree(@Nullable Uri treeUri) {
        Uri documentUri = treeUri == null ? null : documentAccess.buildTreeDocumentUri(treeUri);
        return documentUri != null ? documentUri : treeUri;
    }

    @NonNull
    List<Uri> resolveProfilesDiscoveryTreeUris(
            @NonNull Context context,
            @Nullable Uri savedTreeUri,
            boolean hasSavedTreeReadPermission
    ) {
        List<Uri> out = new ArrayList<>();
        if (hasSavedTreeReadPermission && savedTreeUri != null) {
            addDiscoveryTreeUri(out, savedTreeUri);
        }
        for (String documentId : getProfilesDocumentIdCandidates(context)) {
            if (documentExists(context, documentId)) {
                addDiscoveryTreeUri(out, buildExternalStorageTreeUri(documentId));
            }
        }
        return out;
    }

    @Nullable
    private Uri toParentDocumentUri(@NonNull Uri documentUri) {
        String parentDocumentId = toParentDocumentId(documentUri);
        if (parentDocumentId == null) {
            return null;
        }
        return buildExternalStorageDocumentUri(parentDocumentId);
    }

    @Nullable
    private String toParentDocumentId(@NonNull Uri documentUri) {
        try {
            if (!documentAccess.isExternalStorageDocument(documentUri)) {
                return null;
            }
            String documentId = documentAccess.documentId(documentUri);
            if (documentId == null) {
                return null;
            }
            int slash = documentId.lastIndexOf('/');
            if (slash <= 0) {
                return null;
            }
            return documentId.substring(0, slash);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to derive parent document URI from custom profile uri=" + documentUri, e);
            return null;
        }
    }

    @NonNull
    private Uri fallbackPickerUri(@NonNull Context context) {
        List<String> fallbackCandidates = BRouterProfileDirectoryCandidates.buildFallbackPickerDocumentIdCandidates(
                directoryCandidates.getSecondaryStorageRootIds(context)
        );
        String fallback = fallbackCandidates.get(0);
        return buildExternalStorageDocumentUri(fallback);
    }

    @Nullable
    private String findExistingProfilesDocumentId(@NonNull Context context) {
        for (String documentId : getProfilesDocumentIdCandidates(context)) {
            if (documentExists(context, documentId)) {
                AppLogger.d(TAG, "Using detected profiles path documentId=" + documentId);
                return documentId;
            }
        }
        AppLogger.d(TAG, "No accessible profiles path detected");
        return null;
    }

    @NonNull
    private List<String> getProfilesDocumentIdCandidates(@NonNull Context context) {
        return directoryCandidates.getProfilesDocumentIdCandidates(context);
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

    @NonNull
    private Uri buildExternalStorageDocumentUri(@NonNull String documentId) {
        return documentAccess.buildExternalStorageDocumentUri(documentId);
    }

    @NonNull
    private Uri buildExternalStorageTreeUri(@NonNull String documentId) {
        return documentAccess.buildExternalStorageTreeUri(documentId);
    }

    private boolean documentExists(@NonNull Context context, @NonNull String documentId) {
        return documentAccess.externalStorageDocumentExists(context, documentId);
    }
}
