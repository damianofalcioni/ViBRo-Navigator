package vibro.navigator.brouter;

import android.content.Context;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

final class BRouterProfileDirectoryCandidates {
    private static final String PRIMARY_ROOT_ID = "primary";
    private static final String MEDIA_PROFILES_RELATIVE_DOCUMENT_PATH =
            "Android/media/btools.routingapp/brouter/profiles2";
    private static final String LEGACY_DATA_PROFILES_RELATIVE_DOCUMENT_PATH =
            "Android/data/btools.routingapp/files/brouter/profiles2";

    @NonNull
    List<String> getProfilesDocumentIdCandidates(@NonNull Context context) {
        return buildLikelyPickerDocumentIdCandidates(getSecondaryStorageRootIds(context));
    }

    @NonNull
    List<String> getSecondaryStorageRootIds(@NonNull Context context) {
        List<String> rootIds = new ArrayList<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return rootIds;
        }
        StorageManager storageManager = context.getSystemService(StorageManager.class);
        if (storageManager == null) {
            return rootIds;
        }
        for (StorageVolume volume : storageManager.getStorageVolumes()) {
            String uuid = volume.getUuid();
            if (uuid == null || uuid.trim().isEmpty()) {
                continue;
            }
            if (!rootIds.contains(uuid)) {
                rootIds.add(uuid);
            }
        }
        return rootIds;
    }

    @NonNull
    static List<String> buildLikelyPickerDocumentIdCandidates(@NonNull List<String> secondaryRootIds) {
        List<String> candidates = new ArrayList<>();
        List<String> rootIds = new ArrayList<>(secondaryRootIds);
        rootIds.add(PRIMARY_ROOT_ID);
        for (String rootId : rootIds) {
            addDocumentIdCandidate(candidates, rootId, MEDIA_PROFILES_RELATIVE_DOCUMENT_PATH);
        }
        for (String rootId : rootIds) {
            addDocumentIdCandidate(candidates, rootId, LEGACY_DATA_PROFILES_RELATIVE_DOCUMENT_PATH);
        }
        return candidates;
    }

    @NonNull
    static List<String> buildFallbackPickerDocumentIdCandidates(@NonNull List<String> secondaryRootIds) {
        List<String> candidates = new ArrayList<>();
        addDocumentIdCandidate(candidates, PRIMARY_ROOT_ID, MEDIA_PROFILES_RELATIVE_DOCUMENT_PATH);
        addDocumentIdCandidate(candidates, PRIMARY_ROOT_ID, LEGACY_DATA_PROFILES_RELATIVE_DOCUMENT_PATH);
        for (String rootId : secondaryRootIds) {
            addDocumentIdCandidate(candidates, rootId, MEDIA_PROFILES_RELATIVE_DOCUMENT_PATH);
        }
        for (String rootId : secondaryRootIds) {
            addDocumentIdCandidate(candidates, rootId, LEGACY_DATA_PROFILES_RELATIVE_DOCUMENT_PATH);
        }
        return candidates;
    }

    private static void addDocumentIdCandidate(
            @NonNull List<String> candidates,
            @NonNull String rootId,
            @NonNull String relativePath
    ) {
        String documentId = rootId + ":" + relativePath;
        if (!candidates.contains(documentId)) {
            candidates.add(documentId);
        }
    }
}
