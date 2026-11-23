package vibro.navigator.brouter;

import android.content.Context;
import android.net.Uri;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import androidx.core.util.Supplier;

/** Tracks preferred file resolutions and avoids retrying an alias during one tile read. */
final class BRouterSegmentTileLookup {
    interface Reader {
        boolean read(Uri uri);
    }

    private final BRouterSegmentDependencies.DocumentAccess documents;
    private final BRouterStreetProviderRead providerRead;
    private final HashMap<String, Uri> files = new HashMap<>();
    private final HashSet<String> missing = new HashSet<>();

    BRouterSegmentTileLookup(BRouterSegmentDependencies.DocumentAccess documents, BRouterStreetProviderRead providerRead) {
        this.documents = documents;
        this.providerRead = providerRead;
    }

    boolean read(Context context, String name, List<Uri> granted, Supplier<List<Uri>> detected, Reader reader) {
        if (missing.contains(name)) {
            return false;
        }
        var attempted = new HashSet<String>();
        var cached = files.get(name);
        if (tryFile(cached, attempted, reader)) {
            return true;
        }
        files.remove(name);
        return readTrees(context, name, granted, reader, attempted, true)
                || readTrees(context, name, detected.get(), reader, attempted, granted.isEmpty());
    }

    private boolean readTrees(Context context, String name, List<Uri> trees, Reader reader,
                              Set<String> attempted, boolean preferFirst) {
        boolean preferred = preferFirst;
        for (Uri tree : trees) {
            Uri file = providerRead.run(tree, () -> documents.childDocumentUri(context, tree, name));
            if (tryFile(file, attempted, reader)) {
                if (preferred) {
                    files.put(name, file);
                }
                return true;
            }
            preferred = false;
        }
        return false;
    }

    private boolean tryFile(Uri file, Set<String> attempted, Reader reader) {
        return file != null && attempted.add(documents.documentIdentity(file)) && reader.read(file);
    }

    void recordMissing(String name) {
        if (!providerRead.failed()) {
            missing.add(name);
        }
    }

    void clear() {
        files.clear();
        missing.clear();
    }
}
