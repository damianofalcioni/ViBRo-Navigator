package vibro.navigator.nav.simulation;

import androidx.annotation.Nullable;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

public final class NavigationSimulationInput {
    private static final double METERS_PER_MILE = 1609.344;

    private NavigationSimulationInput() { }

    public static String formatSpeed(float speedMps, boolean imperial, Locale locale) {
        NumberFormat format = numberFormat(locale);
        format.setMaximumFractionDigits(2);
        return format.format(speedMps * unitsPerMeterPerSecond(imperial));
    }

    @Nullable
    public static Float parseSpeed(String text, boolean imperial, Locale locale) {
        String trimmed = text.trim();
        Number number = parseNumber(trimmed, locale);
        if (number == null) { return null; }
        float speed = (float) (number.doubleValue() / unitsPerMeterPerSecond(imperial));
        return NavigationSimulationConfig.validSpeed(speed) ? speed : null;
    }

    @Nullable
    private static Number parseNumber(String text, Locale locale) {
        ParsePosition position = new ParsePosition(0);
        Number number = numberFormat(locale).parse(text, position);
        if (number != null && position.getIndex() == text.length()) { return number; }
        if (text.matches("[+-]?\\d+(?:\\.\\d+)?")) { return Double.valueOf(text); }
        return null;
    }

    private static double unitsPerMeterPerSecond(boolean imperial) {
        return imperial ? 3600.0 / METERS_PER_MILE : 3.6;
    }

    private static NumberFormat numberFormat(Locale locale) {
        NumberFormat format = NumberFormat.getNumberInstance(locale);
        format.setGroupingUsed(false);
        return format;
    }
}
