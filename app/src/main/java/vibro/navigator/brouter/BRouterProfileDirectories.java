package vibro.navigator.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.logging.AppLogger;

import java.util.ArrayList;
import java.util.List;

final class BRouterProfileDirectories {
    private static final String TAG = "BRouterProfiles";

    private final BRouterProfileDirectoryCandidates directoryCandidates =
            new BRouterProfileDirectoryCandidates();

    @Nullable
    Uri getCustomProfilePickerInitialUri(
            @NonNull Context context,
            @Nullable Uri profilesTreeUri,
            boolean hasProfilesTreeReadPermission,
            @Nullable Uri customProfileUri
    ) {
        if (hasProfilesTreeReadPermission) {
            Uri treeDocumentUri = profilesTreeUri == null ? null : AndroidDocumentAccess.buildTreeDocumentUri(profilesTreeUri);
            return treeDocumentUri != null ? treeDocumentUri : profilesTreeUri;
        }
        if (customProfileUri != null) {
            Uri parentUri = toParentDocumentUri(customProfileUri);
            return parentUri != null ? parentUri : customProfileUri;
        }
        String documentId = resolveProfilesPickerInitialDocumentId(context);
        if (documentId == null) {
            AppLogger.d(TAG, "No accessible profiles folder found for custom picker initial URI");
            return null;
        }
        return buildExternalStorageDocumentUri(documentId);
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
            if (!AndroidDocumentAccess.isExternalStorageDocument(documentUri)) {
                return null;
            }
            String documentId = AndroidDocumentAccess.documentId(documentUri);
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

    @Nullable
    private String resolveProfilesPickerInitialDocumentId(@NonNull Context context) {
        String existing = findExistingProfilesDocumentId(context);
        if (existing != null) {
            return existing;
        }
        List<String> fallbackCandidates = BRouterProfileDirectoryCandidates.buildFallbackPickerDocumentIdCandidates(
                directoryCandidates.getSecondaryStorageRootIds(context)
        );
        if (fallbackCandidates.isEmpty()) {
            return null;
        }
        String fallback = fallbackCandidates.get(0);
        AppLogger.d(TAG, "Using fallback profiles path documentId=" + fallback);
        return fallback;
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
        return AndroidDocumentAccess.buildExternalStorageDocumentUri(documentId);
    }

    @NonNull
    private Uri buildExternalStorageTreeUri(@NonNull String documentId) {
        return AndroidDocumentAccess.buildExternalStorageTreeUri(documentId);
    }

    private boolean documentExists(@NonNull Context context, @NonNull String documentId) {
        return AndroidDocumentAccess.externalStorageDocumentExists(context, documentId);
    }
}
