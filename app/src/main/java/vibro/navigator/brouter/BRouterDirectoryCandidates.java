package vibro.navigator.brouter;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class BRouterDirectoryCandidates {
    private static final String PRIMARY_ROOT_ID = "primary";
    private static final String MEDIA_BROUTER_RELATIVE_DOCUMENT_PATH =
            "Android/media/btools.routingapp/brouter/";
    private static final String LEGACY_DATA_BROUTER_RELATIVE_DOCUMENT_PATH =
            "Android/data/btools.routingapp/files/brouter/";

    private BRouterDirectoryCandidates() {
    }

    @NonNull
    public static List<String> buildLikelyDocumentIdCandidates(
            @NonNull List<String> secondaryRootIds,
            @NonNull String directoryName
    ) {
        List<String> candidates = buildStandardDocumentIdCandidates(secondaryRootIds, directoryName);
        candidates.add(legacySharedPrimaryDocumentId(directoryName));
        return candidates;
    }

    @NonNull
    static String legacySharedPrimaryDocumentId(@NonNull String directoryName) {
        return directoryId(PRIMARY_ROOT_ID, "brouter/", directoryName);
    }

    @NonNull
    static List<String> buildStandardDocumentIdCandidates(
            @NonNull List<String> secondaryRootIds, @NonNull String directoryName
    ) {
        List<String> candidates = new ArrayList<>();
        List<String> rootIds = new ArrayList<>(secondaryRootIds);
        rootIds.add(PRIMARY_ROOT_ID);
        addForRoots(candidates, rootIds, MEDIA_BROUTER_RELATIVE_DOCUMENT_PATH, directoryName);
        addForRoots(candidates, rootIds, LEGACY_DATA_BROUTER_RELATIVE_DOCUMENT_PATH, directoryName);
        return candidates;
    }

    @NonNull
    static List<String> buildFallbackPickerDocumentIdCandidates(
            @NonNull List<String> secondaryRootIds,
            @NonNull String directoryName
    ) {
        List<String> candidates = new ArrayList<>();
        addDocumentIdCandidate(candidates, PRIMARY_ROOT_ID, MEDIA_BROUTER_RELATIVE_DOCUMENT_PATH, directoryName);
        addDocumentIdCandidate(candidates, PRIMARY_ROOT_ID, LEGACY_DATA_BROUTER_RELATIVE_DOCUMENT_PATH, directoryName);
        for (String rootId : secondaryRootIds) {
            addDocumentIdCandidate(candidates, rootId, MEDIA_BROUTER_RELATIVE_DOCUMENT_PATH, directoryName);
        }
        for (String rootId : secondaryRootIds) {
            addDocumentIdCandidate(candidates, rootId, LEGACY_DATA_BROUTER_RELATIVE_DOCUMENT_PATH, directoryName);
        }
        return candidates;
    }

    private static void addForRoots(
            @NonNull List<String> candidates,
            @NonNull List<String> rootIds,
            @NonNull String brouterRelativePath,
            @NonNull String directoryName
    ) {
        for (String rootId : rootIds) {
            addDocumentIdCandidate(candidates, rootId, brouterRelativePath, directoryName);
        }
    }

    private static void addDocumentIdCandidate(
            @NonNull List<String> candidates,
            @NonNull String rootId,
            @NonNull String brouterRelativePath,
            @NonNull String directoryName
    ) {
        String documentId = directoryId(rootId, brouterRelativePath, directoryName);
        if (!candidates.contains(documentId)) {
            candidates.add(documentId);
        }
    }

    private static String directoryId(String rootId, String basePath, String directoryName) {
        String path = directoryName.equals("brouter") ? basePath.substring(0, basePath.length() - 1)
                : basePath + directoryName;
        return rootId + ":" + path;
    }
}
