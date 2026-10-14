package vibro.navigator.nav.ui;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import vibro.navigator.nav.format.NavigationInstructionParts;

/** Keeps maneuver metadata fixed beside the scrolling description. */
final class NavigationInstructionRow {
    private NavigationInstructionRow() {
    }

    static void render(NavigationInstructionMetricView prefix, TextView separator, TextView instruction, String line, int color) {
        NavigationInstructionParts parts = NavigationInstructionParts.fromLine(line);
        prefix.setParts(parts);
        prefix.setVisibility(parts.distancePrefix.isEmpty() ? View.GONE : View.VISIBLE);
        separator.setVisibility(prefix.getVisibility());
        setTextIfChanged(instruction, parts.instruction);
        // Announce the full guidance once, including its fixed distance and time.
        instruction.setContentDescription(line);
        prefix.setTextColor(color);
        separator.setTextColor(color);
        instruction.setTextColor(color);
    }

    private static void setTextIfChanged(TextView view, String text) {
        if (!TextUtils.equals(view.getText(), text)) {
            view.setText(text);
        }
    }
}
