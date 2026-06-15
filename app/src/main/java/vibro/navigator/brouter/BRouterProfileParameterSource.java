package vibro.navigator.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import vibro.navigator.logging.AppLogger;

final class BRouterProfileParameterSource {

    private static final String BROUTER_PROFILES_ZIP = "assets/profiles2.zip";
    private static final String TAG = "BRouterProfileParams";

    @NonNull
    private final BRouterProfileDependencies.DocumentAccess documentAccess;
    @NonNull
    private final BRouterProfileDependencies.PackageAccess packageAccess;
    @NonNull
    private final BRouterProfileParameterParser parser;

    BRouterProfileParameterSource(
            @NonNull BRouterProfileDependencies.DocumentAccess documentAccess,
            @NonNull BRouterProfileDependencies.PackageAccess packageAccess,
            @NonNull BRouterProfileParameterParser parser
    ) {
        this.documentAccess = documentAccess;
        this.packageAccess = packageAccess;
        this.parser = parser;
    }

    @NonNull
    List<BRouterProfileParameter> readParameters(
            @NonNull Context context,
            @NonNull String profileName,
            @Nullable Uri customProfileUri,
            @Nullable String customProfileName,
            @NonNull List<Uri> discoveryTreeUris
    ) {
        String profileFileName = profileName + ".brf";
        List<BRouterProfileParameter> custom = readCustomProfileParameters(
                context,
                profileName,
                customProfileUri,
                customProfileName
        );
        if (!custom.isEmpty()) {
            return custom;
        }
        List<BRouterProfileParameter> external = readExternalProfileParameters(
                context,
                profileFileName,
                discoveryTreeUris
        );
        if (!external.isEmpty()) {
            return external;
        }
        return readBundledProfileParameters(context, profileName);
    }

    @NonNull
    private List<BRouterProfileParameter> readCustomProfileParameters(
            @NonNull Context context,
            @NonNull String profileName,
            @Nullable Uri customProfileUri,
            @Nullable String customProfileName
    ) {
        if (customProfileUri == null || customProfileName == null || !profileName.equals(customProfileName)) {
            return Collections.emptyList();
        }
        return parseDocumentProfile(context, customProfileUri);
    }

    @NonNull
    private List<BRouterProfileParameter> readExternalProfileParameters(
            @NonNull Context context,
            @NonNull String profileFileName,
            @NonNull List<Uri> discoveryTreeUris
    ) {
        for (Uri treeUri : discoveryTreeUris) {
            Uri childUri = documentAccess.childDocumentUri(context, treeUri, profileFileName);
            if (childUri == null) {
                continue;
            }
            List<BRouterProfileParameter> parameters = parseDocumentProfile(context, childUri);
            if (!parameters.isEmpty()) {
                return parameters;
            }
        }
        return Collections.emptyList();
    }

    @NonNull
    private List<BRouterProfileParameter> parseDocumentProfile(@NonNull Context context, @NonNull Uri uri) {
        try {
            String profileText = documentAccess.readText(context, uri);
            return parser.parse(profileText);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to read profile parameters from document uri=" + uri, e);
            return Collections.emptyList();
        }
    }

    @NonNull
    private List<BRouterProfileParameter> readBundledProfileParameters(
            @NonNull Context context,
            @NonNull String profileName
    ) {
        try {
            String sourceDir = packageAccess.sourceDir(context, BRouterProfilesRepository.BROUTER_PACKAGE_NAME);
            if (sourceDir == null) {
                return Collections.emptyList();
            }
            try (ZipFile apk = new ZipFile(sourceDir)) {
                ZipEntry profilesZipEntry = apk.getEntry(BROUTER_PROFILES_ZIP);
                if (profilesZipEntry == null) {
                    return Collections.emptyList();
                }
                return readBundledProfileParameters(apk, profilesZipEntry, profileName);
            }
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to read bundled profile parameters profile=" + profileName, e);
            return Collections.emptyList();
        }
    }

    @NonNull
    private List<BRouterProfileParameter> readBundledProfileParameters(
            @NonNull ZipFile apk,
            @NonNull ZipEntry profilesZipEntry,
            @NonNull String profileName
    ) throws IOException {
        try (InputStream raw = apk.getInputStream(profilesZipEntry);
             ZipInputStream zip = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && profileName.equals(BRouterProfileLister.normalizeProfileName(entry.getName()))) {
                    return parser.parse(readCurrentZipEntry(zip));
                }
                zip.closeEntry();
            }
        }
        return Collections.emptyList();
    }

    @NonNull
    private static String readCurrentZipEntry(@NonNull InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
