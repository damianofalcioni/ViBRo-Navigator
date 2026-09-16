package vibro.navigator.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.compass.CompassStreetSegment;
import vibro.navigator.nav.compass.CompassStreetVisibility;

/** Resolves local rd5 reads while preserving the repository's complete decoded-cell cache. */
final class BRouterStreetFileLoader {
    private static final String TAG = "BRouterSegments";

    private final BRouterSegmentsRepository repository;
    private final BRouterSegmentDependencies dependencies;
    private final BRouterDecodedStreetCache decodedStreetCache;
    private final HashMap<String, String> segmentFileDirectoryIds = new HashMap<>();

    BRouterStreetFileLoader(
            BRouterSegmentsRepository repository,
            BRouterSegmentDependencies dependencies,
            BRouterDecodedStreetCache decodedStreetCache
    ) {
        this.repository = repository;
        this.dependencies = dependencies;
        this.decodedStreetCache = decodedStreetCache;
    }

    void read(
            @NonNull Context context,
            @NonNull String fileName,
            @NonNull BRouterSegmentBounds bounds,
            int maxSegments,
            @NonNull CompassStreetVisibility visibility,
            @NonNull List<CompassStreetSegment> out
    ) {
        Uri documentUri = repository.resolveSegmentFileUri(context, fileName);
        if (documentUri == null) {
            readLocal(context, fileName, bounds, maxSegments, visibility, out);
            return;
        }
        try (BRouterSegmentReadFile readFile = dependencies.documentAccess.openReadFile(context, documentUri)) {
            if (readFile == null) {
                readLocal(context, fileName, bounds, maxSegments, visibility, out);
                return;
            }
            new BRouterRd5StreetReader(readFile, fileName, documentUri.toString(), decodedStreetCache)
                    .read(bounds, maxSegments, out, visibility);
        } catch (IOException | RuntimeException e) {
            BRouterStreetReadCancellation.check();
            AppLogger.w(TAG, "Failed to read BRouter segment file=" + fileName, e);
            readLocal(context, fileName, bounds, maxSegments, visibility, out);
        }
    }

    private void readLocal(
            Context context, String fileName, BRouterSegmentBounds bounds, int maxSegments,
            CompassStreetVisibility visibility, List<CompassStreetSegment> out
    ) {
        if (!dependencies.fileAccess.canReadFiles(context)) {
            return;
        }
        String cachedDirectoryId = segmentFileDirectoryIds.get(fileName);
        if (cachedDirectoryId != null
                && readLocal(context, cachedDirectoryId, fileName, bounds, maxSegments, visibility, out)) {
            return;
        }
        for (String directoryId : repository.discoveryDirectoryIds(context)) {
            if (readLocal(context, directoryId, fileName, bounds, maxSegments, visibility, out)) {
                segmentFileDirectoryIds.put(fileName, directoryId);
                return;
            }
        }
        repository.recordMissingSegmentFile(fileName);
    }

    private boolean readLocal(
            Context context, String directoryId, String fileName, BRouterSegmentBounds bounds,
            int maxSegments, CompassStreetVisibility visibility, List<CompassStreetSegment> out
    ) {
        try (BRouterSegmentReadFile readFile = dependencies.fileAccess.openReadFile(context, directoryId, fileName)) {
            if (readFile == null) {
                return false;
            }
            new BRouterRd5StreetReader(readFile, fileName, directoryId + "/" + fileName, decodedStreetCache)
                    .read(bounds, maxSegments, out, visibility);
            return true;
        } catch (IOException | RuntimeException e) {
            BRouterStreetReadCancellation.check();
            AppLogger.w(TAG, "Failed to read BRouter segment file=" + fileName, e);
            return false;
        }
    }
}
