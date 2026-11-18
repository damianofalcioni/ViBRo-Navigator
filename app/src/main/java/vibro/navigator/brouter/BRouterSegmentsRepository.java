package vibro.navigator.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;
import vibro.navigator.nav.compass.CompassStreetVisibility;
import vibro.navigator.nav.streets.SurroundingStreetRepository;

public final class BRouterSegmentsRepository implements SurroundingStreetRepository {
    private static final String TAG = "BRouterSegments";

    interface TileReader {
        boolean read(Uri uri);
    }

    @NonNull
    private final BRouterSegmentDependencies dependencies;
    @NonNull
    private final BRouterSegmentDirectories segmentDirectories;
    @NonNull
    private final Map<String, Uri> segmentFileUris = new HashMap<>();
    @NonNull
    private final Set<String> missingSegmentFiles = new HashSet<>();
    @Nullable
    private List<Uri> detectedTrees;
    private List<Uri> grantedTrees = java.util.Collections.emptyList();
    @Nullable
    private List<String> discoveryDirectoryIds;
    private final BRouterDecodedStreetCache decodedStreetCache = new BRouterDecodedStreetCache();
    private final BRouterStreetFileLoader streetFileLoader;
    @Nullable
    private String sourceRevision;

    public BRouterSegmentsRepository(@NonNull BRouterSegmentDependencies dependencies) {
        this.dependencies = dependencies;
        segmentDirectories = new BRouterSegmentDirectories(
                dependencies.documentAccess,
                dependencies.storageVolumeAccess
        );
        streetFileLoader = new BRouterStreetFileLoader(this, dependencies, decodedStreetCache);
    }

    @Nullable
    public Uri getSegmentsTreeUri(@NonNull Context context) {
        return BRouterSegmentSourceSettings.getTreeUri(context);
    }

    public void saveSegmentsTreeUri(@NonNull Context context, @NonNull Uri treeUri) {
        BRouterSegmentSourceSettings.save(context, treeUri);
        AppLogger.i(TAG, "Saved BRouter segments tree uri=" + treeUri);
    }

    public boolean hasPersistedSegmentsTreeAccess(@NonNull Context context) {
        return getGrantedSegmentsTreeUri(context) != null;
    }

    @Nullable
    public Uri getGrantedSegmentsTreeUri(@NonNull Context context) {
        return BRouterFolderGrant.resolve(context, getSegmentsTreeUri(context), false,
                dependencies.uriPermissionAccess::hasReadPermission);
    }

    @NonNull
    @Override
    public CompassStreetOverlay loadSurroundingStreets(
            @NonNull Context context,
            double latitude,
            double longitude,
            double radiusMeters,
            int maxSegments,
            @NonNull CompassStreetVisibility visibility
    ) {
        BRouterSegmentBounds bounds = BRouterSegmentBounds.around(latitude, longitude, radiusMeters);
        List<CompassStreetSegment> segments = new ArrayList<>();
        Set<String> fileNames = BRouterSegmentTile.fileNamesForBounds(bounds);
        for (String fileName : fileNames) {
            if (segments.size() >= maxSegments) {
                break;
            }
            streetFileLoader.read(context, fileName, bounds, maxSegments, visibility, segments);
        }
        return segments.isEmpty() ? CompassStreetOverlay.EMPTY : new CompassStreetOverlay(segments);
    }

    boolean readGrantedTile(@NonNull Context context, @NonNull String fileName, TileReader reader) {
        refreshSourceCaches(context);
        if (missingSegmentFiles.contains(fileName)) {
            return false;
        }
        Uri cached = segmentFileUris.get(fileName);
        if (cached != null && reader.read(cached)) {
            return true;
        }
        segmentFileUris.remove(fileName);
        return readTrees(context, fileName, reader, cached, grantedTrees, true)
                || readTrees(context, fileName, reader, cached, detectedTrees(context), grantedTrees.isEmpty());
    }

    private boolean readTrees(Context context, String fileName, TileReader reader, @Nullable Uri cached,
                              List<Uri> trees, boolean preferFirst) {
        boolean preferred = preferFirst;
        for (Uri tree : trees) {
            Uri document = dependencies.documentAccess.childDocumentUri(context, tree, fileName);
            if (document != null && !document.equals(cached) && reader.read(document)) {
                if (preferred) {
                    segmentFileUris.put(fileName, document);
                }
                return true;
            }
            preferred = false;
        }
        return false;
    }

    private void refreshSourceCaches(@NonNull Context context) {
        String revision = BRouterSegmentSourceSettings.getRevision(context);
        List<String> directories = segmentDirectories.getSegmentsDocumentIdCandidates(context);
        List<Uri> grants = BRouterFolderGrant.discoveryTrees(
                context, getSegmentsTreeUri(context), false, dependencies.uriPermissionAccess::hasReadPermission);
        if (Objects.equals(sourceRevision, revision) && directories.equals(discoveryDirectoryIds)
                && grants.equals(grantedTrees)) {
            return;
        }
        sourceRevision = revision;
        detectedTrees = null;
        discoveryDirectoryIds = directories;
        grantedTrees = grants;
        segmentFileUris.clear();
        missingSegmentFiles.clear();
        streetFileLoader.clearSources();
        decodedStreetCache.clear();
    }

    @NonNull
    private List<Uri> detectedTrees(@NonNull Context context) {
        if (detectedTrees == null) {
            detectedTrees = segmentDirectories.resolveSegmentsDiscoveryTreeUris(context, discoveryDirectoryIds);
            detectedTrees.removeAll(grantedTrees);
        }
        return detectedTrees;
    }

    @NonNull
    List<String> discoveryDirectoryIds(@NonNull Context context) {
        if (discoveryDirectoryIds == null) {
            discoveryDirectoryIds = segmentDirectories.getSegmentsDocumentIdCandidates(context);
        }
        return discoveryDirectoryIds;
    }

    void recordMissingSegmentFile(@NonNull String fileName) {
        missingSegmentFiles.add(fileName);
        AppLogger.d(TAG, "BRouter segment file not found file=" + fileName);
    }
}
