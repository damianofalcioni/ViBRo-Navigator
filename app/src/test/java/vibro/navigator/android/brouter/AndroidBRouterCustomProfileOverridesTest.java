package vibro.navigator.android.brouter;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.brouter.BRouterRouteRequest;
import vibro.navigator.geo.LatLon;

@RunWith(RobolectricTestRunner.class)
public class AndroidBRouterCustomProfileOverridesTest {
    private static final String PROFILE = "trekking";
    private static final String ALLOW_STEPS = "allow_steps";
    private static final String ALLOW_FERRIES = "allow_ferries";
    private static final String UNANNOTATED = """
            ---context:global
            assign allow_steps = true
            # assign allow_ferries = true
            ---context:way
            assign allow_ferries = true
            """;

    @Test
    public void sameNamedCustomProfileReusesMetadataAndOverridesForGlobalAssignments() throws Exception {
        Context app = ApplicationProvider.getApplicationContext();
        Path directory = Files.createTempDirectory(app.getCacheDir().toPath(), "custom-profile");
        installBundledProfile(app, directory.resolve("brouter.apk"));
        var repository = AndroidBRouterProfilesRepositoryFactory.create();
        var parameters = repository.getProfileParameters(app, PROFILE, false);
        assertEquals(2, parameters.size());
        repository.saveProfileParameterValues(app, PROFILE, parameters,
                Map.of(ALLOW_STEPS, "0", ALLOW_FERRIES, "0"));

        Path custom = Files.writeString(directory.resolve(PROFILE + ".brf"), UNANNOTATED);
        repository.saveCustomProfile(app, Uri.fromFile(custom.toFile()), PROFILE);
        var customParameters = repository.getProfileParameters(app, PROFILE, true);
        assertEquals(1, customParameters.size());
        assertEquals(ALLOW_STEPS, customParameters.get(0).name);
        assertEquals("Allow steps", customParameters.get(0).description);
        assertEquals("true", customParameters.get(0).defaultValue);

        String saved = repository.getProfileParameterOverridesExtraParams(app, PROFILE);
        var request = new BRouterRouteRequest(new LatLon(48, 16), List.of(), new LatLon(48.2, 16.2),
                PROFILE, true, saved, List.of());
        var bundle = AndroidBRouterRouteParams.build(request, repository.getCustomProfileText(app, PROFILE));
        assertEquals(UNANNOTATED, bundle.getString("remoteProfile"));
        assertEquals("allow_steps=0", bundle.getString("extraParams"));

        repository.saveProfileParameterValues(app, PROFILE, customParameters, Map.of(ALLOW_STEPS, "1"));
        assertEquals(Map.of(ALLOW_FERRIES, "0"), repository.getProfileParameterValueOverrides(app, PROFILE));
    }

    private static void installBundledProfile(Context app, Path apkPath) throws Exception {
        String profile = """
                ---context:global
                assign allow_steps = true # %allow_steps% | Allow steps | boolean
                assign allow_ferries = true # %allow_ferries% | Allow ferries | boolean
                """;
        var profilesZip = new ByteArrayOutputStream();
        try (var out = new ZipOutputStream(profilesZip)) {
            out.putNextEntry(new ZipEntry(PROFILE + ".brf"));
            out.write(profile.getBytes(StandardCharsets.UTF_8));
        }
        try (var out = new ZipOutputStream(Files.newOutputStream(apkPath))) {
            out.putNextEntry(new ZipEntry("assets/profiles2.zip"));
            out.write(profilesZip.toByteArray());
        }
        PackageInfo info = new PackageInfo();
        info.packageName = BRouterProfilesRepository.BROUTER_PACKAGE_NAME;
        info.applicationInfo = new ApplicationInfo();
        info.applicationInfo.packageName = info.packageName;
        info.applicationInfo.sourceDir = apkPath.toString();
        shadowOf(app.getPackageManager()).installPackage(info);
    }
}
