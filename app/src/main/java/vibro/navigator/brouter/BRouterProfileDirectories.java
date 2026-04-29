package vibro.navigator.brouter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.util.AppLogger;

import java.util.ArrayList;
import java.util.List;

final class BRouterProfileDirectories {
    private static final String EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY = "com.android.externalstorage.documents";
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
            Uri treeDocumentUri = toTreeDocumentUri(profilesTreeUri);
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
            if (!EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY.equals(documentUri.getAuthority())) {
                return null;
            }
            String documentId = DocumentsContract.getDocumentId(documentUri);
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
    private Uri toTreeDocumentUri(@Nullable Uri treeUri) {
        if (treeUri == null) {
            return null;
        }
        try {
            String treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri);
            return DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to derive document URI from tree uri=" + treeUri, e);
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
        return DocumentsContract.buildDocumentUri(
                EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY,
                documentId
        );
    }

    @NonNull
    private Uri buildExternalStorageTreeUri(@NonNull String documentId) {
        return DocumentsContract.buildTreeDocumentUri(
                EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY,
                documentId
        );
    }

    private boolean documentExists(@NonNull Context context, @NonNull String documentId) {
        Uri treeUri = DocumentsContract.buildTreeDocumentUri(EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY, documentId);
        Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
        try (Cursor cursor = context.getContentResolver().query(
                documentUri,
                new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID},
                null,
                null,
                null
        )) {
            return cursor != null && cursor.moveToFirst();
        } catch (Exception e) {
            AppLogger.d(TAG, "Profiles path not accessible documentId=" + documentId
                    + " error=" + e.getClass().getSimpleName());
            return false;
        }
    }
}
