package vibro.navigator.map;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;

import vibro.navigator.logging.AppLogger;

final class MapPickerPoiScriptInjector {
    private static final String TAG = "MapPickerPoiScript";
    private static final String ASSET_NAME = "map_picker_pois.js";

    private MapPickerPoiScriptInjector() {
    }

    static void inject(@NonNull Context context, @NonNull MapPickerScriptController scriptController) {
        try {
            scriptController.injectScript(MapAssetTextReader.read(context, ASSET_NAME));
        } catch (IOException e) {
            AppLogger.w(TAG, "Failed to inject map POI layer script", e);
        }
    }
}
