package vibro.navigator.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import androidx.core.util.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

final class BRouterProfileLister {

    private static final String BROUTER_PROFILES_ZIP = "assets/profiles2.zip";
    private static final String TAG = "BRouterProfiles";

    @NonNull
    private final BRouterProfileDependencies dependencies;
    @NonNull
    private final BRouterProfileDirectories directories;

    BRouterProfileLister(
            @NonNull BRouterProfileDependencies dependencies,
            @NonNull BRouterProfileDirectories directories
    ) {
        this.dependencies = dependencies;
        this.directories = directories;
    }

    @NonNull
    List<String> listProfiles(@NonNull Context context, @Nullable Uri legacyGrant,
                              @NonNull List<String> bundled, Consumer<List<String>> progress) {
        ListedGrant grant = listFirstPopulatedGrant(context, legacyGrant);
        if (grant != null) {
            List<String> profiles = mergeProfileNames(grant.profiles(), bundled);
            progress.accept(profiles);
            return profiles;
        }
        return listAutodiscoveredProfiles(context, bundled, progress);
    }

    @NonNull
    List<Uri> namedProfileTrees(Context context, @Nullable Uri legacyGrant) {
        ListedGrant grant = listFirstPopulatedGrant(context, legacyGrant);
        return grant == null ? autodiscoveredTrees(context) : Collections.singletonList(grant.tree());
    }

    @Nullable
    private ListedGrant listFirstPopulatedGrant(Context context, @Nullable Uri legacyGrant) {
        // BRouter resolves a named profile in one configured folder.
        for (Uri tree : BRouterFolderGrant.sourceCandidates(context, legacyGrant, true)) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            if (!dependencies.uriPermissionAccess.hasReadPermission(context, tree)) {
                continue;
            }
            List<String> profiles = listProfilesFromTree(context, tree);
            if (!profiles.isEmpty()) {
                return new ListedGrant(tree, profiles);
            }
        }
        return null;
    }

    private List<String> listAutodiscoveredProfiles(Context context, List<String> bundled,
                                                     Consumer<List<String>> progress) {
        List<String> externalProfiles = new ArrayList<>();
        for (Uri treeUri : autodiscoveredTrees(context)) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            externalProfiles.addAll(listProfilesFromTree(context, treeUri));
            progress.accept(mergeProfileNames(externalProfiles, bundled));
        }
        List<String> out = mergeProfileNames(externalProfiles, bundled);
        AppLogger.i(TAG, "Listed profiles total=" + out.size()
                + " external=" + new TreeSet<>(externalProfiles).size()
                + " bundled=" + bundled.size());
        return out;
    }

    private List<Uri> autodiscoveredTrees(Context context) {
        return directories.resolveProfilesDiscoveryTreeUris(context, Collections.emptyList());
    }

    private record ListedGrant(Uri tree, List<String> profiles) { }

    @NonNull
    static List<String> mergeProfileNames(
            @NonNull List<String> externalProfiles,
            @NonNull List<String> bundledProfiles
    ) {
        Set<String> out = new TreeSet<>(externalProfiles);
        out.addAll(bundledProfiles);
        for (Iterator<String> profiles = out.iterator(); profiles.hasNext();) {
            if (isReservedProfile(profiles.next())) {
                profiles.remove();
            }
        }
        return new ArrayList<>(out);
    }

    @NonNull
    private List<String> listProfilesFromTree(@NonNull Context context, @NonNull Uri treeUri) {
        List<String> out = new ArrayList<>();
        for (String displayName : dependencies.documentAccess.childDisplayNames(context, treeUri)) {
            addProfileName(out, displayName);
        }
        Collections.sort(out);
        AppLogger.i(TAG, "Listed external profiles uri=" + treeUri + " count=" + out.size());
        return out;
    }

    private static boolean isReservedProfile(String name) {
        return "remote".equalsIgnoreCase(name);
    }

    @NonNull
    List<String> listBundledProfiles(@NonNull Context context) {
        try {
            String sourceDir = dependencies.packageAccess.sourceDir(context, BRouterProfilesRepository.BROUTER_PACKAGE_NAME);
            if (sourceDir == null) {
                AppLogger.w(TAG, "BRouter APK source path unavailable");
                return Collections.emptyList();
            }
            try (ZipFile apk = new ZipFile(sourceDir)) {
                ZipEntry profilesZipEntry = apk.getEntry(BROUTER_PROFILES_ZIP);
                if (profilesZipEntry == null) {
                    AppLogger.w(TAG, "Bundled profiles zip missing inside BRouter APK");
                    return Collections.emptyList();
                }
                return readBundledProfiles(apk, profilesZipEntry);
            }
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to read bundled BRouter profiles", e);
            return Collections.emptyList();
        }
    }

    @NonNull
    private List<String> readBundledProfiles(
            @NonNull ZipFile apk,
            @NonNull ZipEntry profilesZipEntry
    ) throws java.io.IOException {
        List<String> out = new ArrayList<>();
        try (InputStream raw = apk.getInputStream(profilesZipEntry);
             ZipInputStream zip = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    addProfileName(out, entry.getName());
                }
                zip.closeEntry();
            }
        }
        Collections.sort(out);
        AppLogger.i(TAG, "Listed bundled BRouter profiles count=" + out.size());
        return out;
    }

    private void addProfileName(@NonNull List<String> out, @Nullable String rawName) {
        String profileName = normalizeProfileName(rawName);
        if (profileName != null && !isReservedProfile(profileName)) {
            out.add(profileName);
        }
    }

    @Nullable
    static String normalizeProfileName(@Nullable String rawName) {
        if (rawName == null) {
            return null;
        }
        String name = rawName.trim();
        if (name.isEmpty()) {
            return null;
        }
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0 && slash + 1 < name.length()) {
            name = name.substring(slash + 1);
        }
        if (!name.toLowerCase(Locale.ROOT).endsWith(".brf")) {
            return null;
        }
        String base = name.substring(0, name.length() - 4).trim();
        return base.isEmpty() ? null : base;
    }
}
