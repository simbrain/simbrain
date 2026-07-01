package org.simbrain.util.piccolo;

import org.piccolo2d.extras.util.PFixedWidthStroke;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.util.PPaintContext;
import org.simbrain.util.NetworkTheme;
import org.simbrain.util.Utils;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Selection marquee node.
 */
public final class SelectionMarquee extends PPath.Float {

    /**
     * Default stroke.
     */
    private static final Stroke DEFAULT_STROKE = Utils.isMacOSX() ? new BasicStroke(1.0f) : new PFixedWidthStroke(1.0f);

    /**
     * Default interior transparency.
     */
    private static final float DEFAULT_TRANSPARENCY = 0.6f;

    /**
     * Create a new selection marquee at the specified point (<code>x</code>,
     * <code>y</code>).
     *
     * @param x x
     * @param y y
     */
    public SelectionMarquee(final float x, final float y) {
        super();

        append(new Rectangle2D.Float(x, y, 0.0f, 0.0f), false);

        setStroke(DEFAULT_STROKE);
        setTransparency(DEFAULT_TRANSPARENCY);
    }

    @Override
    protected void paint(final PPaintContext paintContext) {
        NetworkTheme.Palette palette = NetworkTheme.INSTANCE.getCurrent();
        Color stroke = palette.getMarquee();
        Color fill = blend(stroke, palette.getCanvasBackground(), 0.8f);
        Path2D path = getPathReference();
        Graphics2D g2 = paintContext.getGraphics();

        g2.setPaint(fill);
        g2.fill(path);

        g2.setPaint(stroke);
        g2.setStroke(new PFixedWidthStroke(1.5f));
        g2.draw(path);
    }

    private static Color blend(final Color a, final Color b, final float bWeight) {
        float aWeight = 1f - bWeight;
        return new Color(
            Math.round(a.getRed() * aWeight + b.getRed() * bWeight),
            Math.round(a.getGreen() * aWeight + b.getGreen() * bWeight),
            Math.round(a.getBlue() * aWeight + b.getBlue() * bWeight));
    }
}