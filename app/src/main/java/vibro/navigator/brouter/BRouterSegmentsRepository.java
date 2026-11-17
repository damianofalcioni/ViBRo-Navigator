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

    @NonNull
    private final BRouterSegmentDependencies dependencies;
    @NonNull
    private final BRouterSegmentDirectories segmentDirectories;
    @NonNull
    private final Map<String, Uri> segmentFileUris = new HashMap<>();
    @NonNull
    private final Set<String> missingSegmentFiles = new HashSet<>();
    @Nullable
    private List<Uri> discoveryTreeUris;
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
        return BRouterFolderGrant.resolve(context, getSegmentsTreeUri(context),
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

    @Nullable
    Uri resolveSegmentFileUri(@NonNull Context context, @NonNull String fileName) {
        refreshSourceCaches(context);
        if (missingSegmentFiles.contains(fileName)) {
            return null;
        }
        if (segmentFileUris.containsKey(fileName)) {
            return segmentFileUris.get(fileName);
        }
        for (Uri treeUri : discoveryTreeUris(context)) {
            Uri documentUri = dependencies.documentAccess.childDocumentUri(context, treeUri, fileName);
            if (documentUri != null) {
                segmentFileUris.put(fileName, documentUri);
                return documentUri;
            }
        }
        return null;
    }

    private void refreshSourceCaches(@NonNull Context context) {
        String revision = BRouterSegmentSourceSettings.getRevision(context);
        if (Objects.equals(sourceRevision, revision)) {
            return;
        }
        sourceRevision = revision;
        discoveryTreeUris = null;
        discoveryDirectoryIds = null;
        segmentFileUris.clear();
        missingSegmentFiles.clear();
        streetFileLoader.clearSources();
        decodedStreetCache.clear();
    }

    @NonNull
    private List<Uri> discoveryTreeUris(@NonNull Context context) {
        if (discoveryTreeUris == null) {
            discoveryTreeUris = segmentDirectories.resolveSegmentsDiscoveryTreeUris(context, BRouterFolderGrant.discoveryTrees(
                    context, getSegmentsTreeUri(context), dependencies.uriPermissionAccess::hasReadPermission));
        }
        return discoveryTreeUris;
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
