
package org.simbrain.network.nodes;

import java.awt.Color;
import java.awt.Paint;

import org.simbrain.network.NetworkPreferences;

import edu.umd.cs.piccolo.nodes.PPath;

/**
 * Selection marquee node.
 */
public final class SelectionMarquee
    extends PPath {

    /** Default paint. */
    private static final Paint DEFAULT_PAINT = Color.WHITE;

    /** Color of selection marquee. */
    private static Color marqueeColor = new Color(NetworkPreferences.getLassoColor());

    /** Default interior transparency. */
    private static final float DEFAULT_TRANSPARENCY = 0.6f;


    /**
     * Create a new selection marquee at the specified point
     * (<code>x</code>, <code>y</code>).
     *
     * @param x x
     * @param y y
     */
    public SelectionMarquee(final float x, final float y) {
        super();

        setPathToRectangle(x, y, 0.0f, 0.0f);

        setPaint(DEFAULT_PAINT);
        setStrokePaint(marqueeColor);
        setTransparency(DEFAULT_TRANSPARENCY);
    }


    /**
     * @return Returns the marqueeColor.
     */
    public static Color getMarqueeColor() {
        return marqueeColor;
    }


    /**
     * @param marqueeColor The marqueeColor to set.
     */
    public static void setMarqueeColor(Color marqueeColor) {
        SelectionMarquee.marqueeColor = marqueeColor;
    }
}