package org.simbrain.util.piccolo;

import org.piccolo2d.extras.util.PFixedWidthStroke;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.util.PPaintContext;
import org.simbrain.util.Utils;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Selection marquee node.
 */
public final class SelectionMarquee extends PPath.Float {

    /**
     * Default paint.
     */
    private static final Paint DEFAULT_PAINT = Color.WHITE;

    /**
     * Default stroke.
     */
    private static final Stroke DEFAULT_STROKE = Utils.isMacOSX() ? new BasicStroke(1.0f) : new PFixedWidthStroke(1.0f);

    /**
     * Color of selection marquee.
     */
    private static Color marqueeColor = Color.yellow;

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

        setPaint(DEFAULT_PAINT);
        setStroke(DEFAULT_STROKE);
        setStrokePaint(marqueeColor);
        setTransparency(DEFAULT_TRANSPARENCY);
    }

    @Override
    protected void paint(final PPaintContext paintContext) {
        Paint p = getPaint();
        Stroke stroke = getStroke();
        Paint strokePaint = getStrokePaint();
        Path2D path = getPathReference();
        Graphics2D g2 = paintContext.getGraphics();

        if (p != null) {
            g2.setPaint(p);
            g2.fill(path);
        }

        if (stroke != null && strokePaint != null) {
            g2.setPaint(strokePaint);
            g2.setStroke(new PFixedWidthStroke(1.5f));
            g2.draw(path);
        }
    }

    public static Color getMarqueeColor() {
        return marqueeColor;
    }

    public static void setMarqueeColor(final Color marqueeColor) {
        SelectionMarquee.marqueeColor = marqueeColor;
    }
}