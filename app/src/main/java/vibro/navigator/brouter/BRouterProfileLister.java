package vibro.navigator.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.android.packageinfo.AndroidPackages;
import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.logging.AppLogger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

class BRouterProfileLister {

    private static final String BROUTER_PROFILES_ZIP = "assets/profiles2.zip";
    private static final String TAG = "BRouterProfiles";

    @NonNull
    List<String> listProfiles(@NonNull Context context, @NonNull List<Uri> discoveryTreeUris) {
        Set<String> externalProfiles = new TreeSet<>();
        for (Uri treeUri : discoveryTreeUris) {
            externalProfiles.addAll(listProfilesFromTree(context, treeUri));
        }
        Set<String> out = new TreeSet<>(externalProfiles);
        List<String> bundled = listBundledProfiles(context);
        out.addAll(bundled);
        AppLogger.i(TAG, "Listed profiles total=" + out.size()
                + " external=" + externalProfiles.size()
                + " bundled=" + bundled.size());
        return new ArrayList<>(out);
    }

    @NonNull
    List<String> listProfilesFromTree(@NonNull Context context, @NonNull Uri treeUri) {
        List<String> out = new ArrayList<>();
        for (String displayName : AndroidDocumentAccess.childDisplayNames(context, treeUri)) {
            addProfileName(out, displayName);
        }
        Collections.sort(out);
        AppLogger.i(TAG, "Listed external profiles uri=" + treeUri + " count=" + out.size());
        return out;
    }

    @NonNull
    List<String> listBundledProfiles(@NonNull Context context) {
        try {
            String sourceDir = AndroidPackages.sourceDir(context, BRouterProfilesRepository.BROUTER_PACKAGE_NAME);
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
        if (profileName != null) {
            out.add(profileName);
        }
    }

    @Nullable
    String normalizeProfileName(@Nullable String rawName) {
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
