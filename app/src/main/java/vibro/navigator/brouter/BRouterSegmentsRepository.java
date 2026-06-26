package vibro.navigator.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;

public final class BRouterSegmentsRepository {
    private static final String TAG = "BRouterSegments";

    @NonNull
    private final BRouterSegmentDependencies dependencies;
    @NonNull
    private final BRouterSegmentDirectories segmentDirectories;
    @NonNull
    private final Map<String, Uri> segmentFileUris = new HashMap<>();
    @NonNull
    private final Map<String, String> segmentFileDirectoryIds = new HashMap<>();
    @NonNull
    private final Set<String> missingSegmentFiles = new HashSet<>();
    @Nullable
    private List<Uri> discoveryTreeUris;
    @Nullable
    private List<String> discoveryDirectoryIds;

    public BRouterSegmentsRepository(@NonNull BRouterSegmentDependencies dependencies) {
        this.dependencies = dependencies;
        segmentDirectories = new BRouterSegmentDirectories(
                dependencies.documentAccess,
                dependencies.storageVolumeAccess
        );
    }

    @NonNull
    public CompassStreetOverlay loadSurroundingStreets(
            @NonNull Context context,
            double latitude,
            double longitude,
            double radiusMeters,
            int maxSegments
    ) {
        BRouterSegmentBounds bounds = BRouterSegmentBounds.around(latitude, longitude, radiusMeters);
        List<CompassStreetSegment> segments = new ArrayList<>();
        Set<String> fileNames = BRouterSegmentTile.fileNamesForBounds(bounds);
        for (String fileName : fileNames) {
            if (segments.size() >= maxSegments) {
                break;
            }
            readSegmentFile(context, fileName, bounds, maxSegments, segments);
        }
        return segments.isEmpty() ? CompassStreetOverlay.EMPTY : new CompassStreetOverlay(segments);
    }

    private void readSegmentFile(
            @NonNull Context context,
            @NonNull String fileName,
            @NonNull BRouterSegmentBounds bounds,
            int maxSegments,
            @NonNull List<CompassStreetSegment> out
    ) {
        Uri documentUri = resolveSegmentFileUri(context, fileName);
        if (documentUri == null) {
            readLocalSegmentFile(context, fileName, bounds, maxSegments, out);
            return;
        }
        try (BRouterSegmentReadFile readFile = dependencies.documentAccess.openReadFile(context, documentUri)) {
            if (readFile == null) {
                readLocalSegmentFile(context, fileName, bounds, maxSegments, out);
                return;
            }
            new BRouterRd5StreetReader(readFile, fileName).read(bounds, maxSegments, out);
        } catch (IOException | RuntimeException e) {
            AppLogger.w(TAG, "Failed to read BRouter segment file=" + fileName, e);
            readLocalSegmentFile(context, fileName, bounds, maxSegments, out);
        }
    }

    private void readLocalSegmentFile(
            @NonNull Context context,
            @NonNull String fileName,
            @NonNull BRouterSegmentBounds bounds,
            int maxSegments,
            @NonNull List<CompassStreetSegment> out
    ) {
        if (!dependencies.fileAccess.canReadFiles(context)) {
            return;
        }
        if (readCachedLocalSegmentFile(context, fileName, bounds, maxSegments, out)) {
            return;
        }
        for (String directoryId : discoveryDirectoryIds(context)) {
            if (readLocalSegmentFile(context, directoryId, fileName, bounds, maxSegments, out)) {
                segmentFileDirectoryIds.put(fileName, directoryId);
                return;
            }
        }
        missingSegmentFiles.add(fileName);
        AppLogger.d(TAG, "BRouter segment file not found file=" + fileName);
    }

    private boolean readCachedLocalSegmentFile(
            @NonNull Context context,
            @NonNull String fileName,
            @NonNull BRouterSegmentBounds bounds,
            int maxSegments,
            @NonNull List<CompassStreetSegment> out
    ) {
        String directoryId = segmentFileDirectoryIds.get(fileName);
        return directoryId != null
                && readLocalSegmentFile(context, directoryId, fileName, bounds, maxSegments, out);
    }

    private boolean readLocalSegmentFile(
            @NonNull Context context,
            @NonNull String directoryId,
            @NonNull String fileName,
            @NonNull BRouterSegmentBounds bounds,
            int maxSegments,
            @NonNull List<CompassStreetSegment> out
    ) {
        try (BRouterSegmentReadFile readFile = dependencies.fileAccess.openReadFile(context, directoryId, fileName)) {
            if (readFile == null) {
                return false;
            }
            new BRouterRd5StreetReader(readFile, fileName).read(bounds, maxSegments, out);
            return true;
        } catch (IOException | RuntimeException e) {
            AppLogger.w(TAG, "Failed to read BRouter segment file=" + fileName, e);
            return false;
        }
    }

    @Nullable
    private Uri resolveSegmentFileUri(@NonNull Context context, @NonNull String fileName) {
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

    @NonNull
    private List<Uri> discoveryTreeUris(@NonNull Context context) {
        if (discoveryTreeUris == null) {
            discoveryTreeUris = segmentDirectories.resolveSegmentsDiscoveryTreeUris(context);
        }
        return discoveryTreeUris;
    }

    @NonNull
    private List<String> discoveryDirectoryIds(@NonNull Context context) {
        if (discoveryDirectoryIds == null) {
            discoveryDirectoryIds = segmentDirectories.getSegmentsDocumentIdCandidates(context);
        }
        return discoveryDirectoryIds;
    }
}
