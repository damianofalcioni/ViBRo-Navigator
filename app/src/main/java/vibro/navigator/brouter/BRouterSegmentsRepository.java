package vibro.navigator.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private final BRouterSegmentTileLookup tileLookup;
    @Nullable
    private List<Uri> detectedTrees;
    private List<Uri> grantedTrees = java.util.Collections.emptyList();
    @Nullable
    private List<String> discoveryDirectoryIds;
    private final BRouterDecodedStreetCache decodedStreetCache = new BRouterDecodedStreetCache();
    private final BRouterStreetProviderRead providerRead = new BRouterStreetProviderRead();
    private final BRouterStreetFileLoader streetFileLoader;
    @Nullable
    private String sourceRevision;

    public BRouterSegmentsRepository(@NonNull BRouterSegmentDependencies dependencies) {
        this.dependencies = dependencies;
        tileLookup = new BRouterSegmentTileLookup(dependencies.documentAccess, providerRead);
        segmentDirectories = new BRouterSegmentDirectories(
                dependencies.documentAccess,
                dependencies.storageVolumeAccess,
                providerRead
        );
        streetFileLoader = new BRouterStreetFileLoader(this, dependencies, decodedStreetCache, providerRead);
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
        providerRead.startLoad();
        refreshSourceCaches(context);
        BRouterSegmentBounds bounds = BRouterSegmentBounds.around(latitude, longitude, radiusMeters);
        List<CompassStreetSegment> segments = new ArrayList<>();
        var fileNames = BRouterSegmentTile.fileNamesForBounds(bounds);
        for (String fileName : fileNames) {
            if (segments.size() >= maxSegments) {
                break;
            }
            streetFileLoader.read(context, fileName, bounds, maxSegments, visibility, segments);
        }
        if (segments.isEmpty() && providerRead.failed()) {
            throw new SurroundingStreetRepository.ReadUnavailable();
        }
        return segments.isEmpty() ? CompassStreetOverlay.EMPTY : new CompassStreetOverlay(segments);
    }

    boolean readGrantedTile(@NonNull Context context, @NonNull String fileName, BRouterSegmentTileLookup.Reader reader) {
        providerRead.startLoad();
        refreshSourceCaches(context);
        return readTile(context, fileName, reader);
    }

    boolean readTile(Context context, String fileName, BRouterSegmentTileLookup.Reader reader) {
        return tileLookup.read(context, fileName, grantedTrees, () -> detectedTrees(context), reader);
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
        tileLookup.clear();
        streetFileLoader.clearSources();
        decodedStreetCache.clear();
    }

    @NonNull
    private List<Uri> detectedTrees(@NonNull Context context) {
        if (detectedTrees == null) {
            List<Uri> detected = segmentDirectories.resolveSegmentsDiscoveryTreeUris(context, discoveryDirectoryIds);
            detected.removeAll(grantedTrees);
            if (!providerRead.failed()) {
                detectedTrees = detected;
            }
            return detected;
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
        tileLookup.recordMissing(fileName);
        AppLogger.d(TAG, "BRouter segment file not found file=" + fileName);
    }
}
