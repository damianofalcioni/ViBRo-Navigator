package vibro.navigator.auto;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import vibro.navigator.android.text.NavigationInstructionSymbolStyle;

import vibro.navigator.nav.format.NavigationInstructionParts;
import vibro.navigator.nav.format.NavigationInstructionMetricCycle;

/** One line of fixed maneuver metadata followed by a scrolling description. */
final class ViBRoAutoInstructionPainter {
    private static final long END_PAUSE_MS = 1200;

    private StaticLayout prefixLayout;
    private StaticLayout timeLayout;
    private StaticLayout separatorLayout;
    private StaticLayout instructionLayout;
    private NavigationInstructionParts parts = NavigationInstructionParts.fromLine("");
    private final NavigationInstructionMetricCycle metricCycle = new NavigationInstructionMetricCycle();
    private String lastLine = "";
    private String instructionKey = "";
    private float lastTextSize;
    private int lastColor;
    private float prefixWidth;
    private float width;
    private float overflow;
    private long cycleStartMs = -1;
    private boolean active;

    void prepare(String line, Paint paint, float textSize, float rowWidth) {
        width = Math.max(0f, rowWidth);
        active = width > 0f && !line.isEmpty();
        if (instructionLayout == null || !lastLine.equals(line)
                || lastTextSize != textSize || lastColor != paint.getColor()) {
            updateLayouts(line, paint, textSize);
        }
        overflow = Math.max(0f, instructionLayout.getLineWidth(0) - Math.max(0f, width - instructionStart()));
        if (!isScrolling()) {
            cycleStartMs = -1;
        }
    }

    float height() {
        return lastLine.isEmpty() ? 0f : Math.max(Math.max(prefixLayout.getHeight(), timeLayout.getHeight()), instructionLayout.getHeight());
    }

    boolean isScrolling() {
        return overflow > 0f && width > instructionStart();
    }

    void reset() {
        active = false;
        metricCycle.reset();
        cycleStartMs = -1;
        overflow = 0f;
    }

    long nextMetricRefreshDelayMs(long nowMs) {
        if (!active || !parts.alternatesMetrics()) {
            return Long.MAX_VALUE;
        }
        metricCycle.prefix(parts, nowMs);
        return metricCycle.nextSwitchDelayMs(nowMs);
    }

    void draw(Canvas canvas, float left, float top, long nowMs, float speedPixelsPerSecond) {
        if (height() == 0f) {
            return;
        }
        int save = canvas.save();
        canvas.clipRect(left, top, left + width, top + height());
        String displayedPrefix = metricCycle.prefix(parts, nowMs);
        StaticLayout metricLayout = displayedPrefix.equals(parts.timePrefix) ? timeLayout : prefixLayout;
        ViBRoAutoWrappedText.draw(canvas, metricLayout, left, top + (height() - metricLayout.getHeight()) / 2f);
        ViBRoAutoWrappedText.draw(canvas, separatorLayout, left + prefixWidth,
                top + (height() - separatorLayout.getHeight()) / 2f);
        canvas.clipRect(left + instructionStart(), top, left + width, top + height());
        ViBRoAutoWrappedText.draw(canvas, instructionLayout,
                left + instructionStart() - scrollOffset(nowMs, speedPixelsPerSecond),
                top + (height() - instructionLayout.getHeight()) / 2f);
        canvas.restoreToCount(save);
    }

    private void updateLayouts(String line, Paint paint, float textSize) {
        parts = NavigationInstructionParts.fromLine(line);
        String updatedKey = parts.symbol + parts.instruction;
        if (!instructionKey.equals(updatedKey)) {
            instructionKey = updatedKey;
            cycleStartMs = -1;
            prefixWidth = 0f;
        }
        if (lastTextSize != textSize) {
            prefixWidth = 0f;
        }
        TextPaint layoutPaint = new TextPaint(paint);
        layoutPaint.setTextSize(textSize);
        layoutPaint.setTextAlign(Paint.Align.LEFT);
        prefixLayout = singleLine(NavigationInstructionSymbolStyle.apply(parts.distancePrefix, parts.symbol), layoutPaint);
        timeLayout = singleLine(NavigationInstructionSymbolStyle.apply(parts.timePrefix, parts.symbol), layoutPaint);
        separatorLayout = singleLine(parts.distancePrefix.isEmpty() ? "" : " - ", layoutPaint);
        instructionLayout = singleLine(parts.instruction.replace('\n', ' '), layoutPaint);
        prefixWidth = Math.max(prefixWidth, Math.max(prefixLayout.getLineWidth(0), timeLayout.getLineWidth(0)));
        lastLine = line;
        lastTextSize = textSize;
        lastColor = paint.getColor();
    }

    private float instructionStart() {
        return prefixWidth + separatorLayout.getLineWidth(0);
    }

    private float scrollOffset(long nowMs, float speedPixelsPerSecond) {
        if (!isScrolling()) {
            return 0f;
        }
        if (cycleStartMs < 0) {
            cycleStartMs = nowMs;
        }
        long travelMs = Math.max(1L, Math.round(overflow * 1000f / speedPixelsPerSecond));
        long elapsed = Math.max(0L, nowMs - cycleStartMs) % (travelMs + END_PAUSE_MS * 2);
        float progress = Math.max(0f, Math.min(1f, (elapsed - END_PAUSE_MS) / (float) travelMs));
        return progress * overflow;
    }

    private static StaticLayout singleLine(CharSequence text, TextPaint paint) {
        int measuredWidth = Math.max(1, (int) Math.ceil(Layout.getDesiredWidth(text, paint)));
        return StaticLayout.Builder.obtain(text, 0, text.length(), new TextPaint(paint), measuredWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .setMaxLines(1)
                .build();
    }
}
