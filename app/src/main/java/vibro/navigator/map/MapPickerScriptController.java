package vibro.navigator.map;

import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.poi.Poi;

import java.util.Locale;

final class MapPickerScriptController {
    @Nullable
    private WebView mapWebView;
    private boolean pageLoaded;

    void attach(@NonNull WebView mapWebView) {
        this.mapWebView = mapWebView;
    }

    void detach() {
        mapWebView = null;
        pageLoaded = false;
    }

    void onPageLoaded() {
        pageLoaded = true;
    }

    void zoomIn() {
        run("window.mapPicker.zoomIn();");
    }

    void zoomOut() {
        run("window.mapPicker.zoomOut();");
    }

    void initialize(
            double centerLat,
            double centerLon,
            int zoom,
            @NonNull String selectedLat,
            @NonNull String selectedLon
    ) {
        run(String.format(
                Locale.US,
                "window.mapPicker.initialize(%s,%s,%d,%s,%s);",
                formatJsDouble(centerLat),
                formatJsDouble(centerLon),
                zoom,
                selectedLat,
                selectedLon
        ));
    }

    void centerOn(@NonNull Poi poi, int zoom, boolean selectPoint) {
        run(String.format(
                Locale.US,
                "window.mapPicker.centerOn(%s,%s,%d,%s);",
                formatJsDouble(poi.lat),
                formatJsDouble(poi.lon),
                zoom,
                selectPoint ? "true" : "false"
        ));
    }

    @NonNull
    static String formatJsDouble(double value) {
        return String.format(Locale.US, "%.8f", value);
    }

    private void run(@NonNull String script) {
        WebView view = mapWebView;
        if (!pageLoaded || view == null) {
            return;
        }
        view.evaluateJavascript(script, null);
    }
}
