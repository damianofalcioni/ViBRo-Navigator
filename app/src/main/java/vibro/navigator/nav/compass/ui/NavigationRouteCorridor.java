package vibro.navigator.nav.compass.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;

/** Caches bounded ground-plane shapes so old hardware path textures cannot discard the corridor. */
final class NavigationRouteCorridor {
    // Central projection can enlarge nearby ground fourfold, plus heading rotation. Keep each
    // source shape small enough that its projected mask fits even a 2048-pixel texture.
    private static final float TILE_SIZE_PX = 256f;
    private static final float EDGE_PADDING_PX = 1f;
    private final List<Tile> tiles = new ArrayList<>();
    private final Paint fillPaint = new Paint();
    private boolean prepared;

    void draw(Canvas canvas, Path centerline, Paint strokePaint) {
        if (!sameStroke(strokePaint)) {
            prepare(centerline, strokePaint);
        }
        fillPaint.set(strokePaint);
        fillPaint.setStyle(Paint.Style.FILL);
        for (Tile tile : tiles) {
            tile.draw(canvas, fillPaint);
        }
    }

    private boolean sameStroke(Paint paint) {
        return prepared && fillPaint.getStrokeWidth() == paint.getStrokeWidth()
                && fillPaint.getStrokeCap() == paint.getStrokeCap()
                && fillPaint.getStrokeJoin() == paint.getStrokeJoin()
                && fillPaint.getStrokeMiter() == paint.getStrokeMiter();
    }

    private void prepare(Path centerline, Paint strokePaint) {
        tiles.clear();
        Path outline = new Path();
        strokePaint.getFillPath(centerline, outline);
        RectF bounds = new RectF();
        outline.computeBounds(bounds, false);
        if (bounds.width() <= TILE_SIZE_PX && bounds.height() <= TILE_SIZE_PX) {
            tiles.add(new Tile(outline, null));
        } else {
            prepareTiles(outline, bounds);
        }
        prepared = true;
    }

    private void prepareTiles(Path outline, RectF bounds) {
        float firstX = (float) Math.floor(bounds.left / TILE_SIZE_PX) * TILE_SIZE_PX;
        float firstY = (float) Math.floor(bounds.top / TILE_SIZE_PX) * TILE_SIZE_PX;
        for (float y = firstY; y < bounds.bottom; y += TILE_SIZE_PX) {
            for (float x = firstX; x < bounds.right; x += TILE_SIZE_PX) {
                addTile(outline, x, y);
            }
        }
    }

    private void addTile(Path outline, float x, float y) {
        RectF clip = new RectF(x, y, x + TILE_SIZE_PX, y + TILE_SIZE_PX);
        Path shape = new Path();
        // Extend the shape past its hard clip to avoid antialias seams or doubled translucent joins.
        shape.addRect(x - EDGE_PADDING_PX, y - EDGE_PADDING_PX,
                clip.right + EDGE_PADDING_PX, clip.bottom + EDGE_PADDING_PX, Path.Direction.CW);
        if (shape.op(outline, Path.Op.INTERSECT) && !shape.isEmpty()) {
            tiles.add(new Tile(shape, clip));
        }
    }

    private static final class Tile {
        private final Path shape;
        private final RectF clip;

        private Tile(Path shape, RectF clip) {
            this.shape = shape;
            this.clip = clip;
        }

        private void draw(Canvas canvas, Paint paint) {
            int saved = canvas.save();
            try {
                if (clip != null) {
                    canvas.clipRect(clip);
                }
                canvas.drawPath(shape, paint);
            } finally {
                canvas.restoreToCount(saved);
            }
        }
    }
}
