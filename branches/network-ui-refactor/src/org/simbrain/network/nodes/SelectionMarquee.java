
package org.simbrain.network.nodes;

import java.awt.Color;
import java.awt.Paint;

import edu.umd.cs.piccolo.nodes.PPath;

/**
 * Selection marquee node.
 */
public final class SelectionMarquee
    extends PPath {

    /** Default paint. */
    private static final Paint DEFAULT_PAINT = Color.WHITE;

    /** Default stroke paint. */
    private static final Paint DEFAULT_STROKE_PAINT = Color.BLACK;

    /** Default interior transparency. */
    private static final float DEFAULT_TRANSPARENCY = 0.6f;


    /**
     * Create a new selection marquee at the specified point
     * (<code>x</code>, <code>y</code>).
     *
     * @param x x
     * @param y y
     */
    public SelectionMarquee(final float x, final float y)
    {
        super();

        setPathToRectangle(x, y, 0.0f, 0.0f);

        setPaint(DEFAULT_PAINT);
        setStrokePaint(DEFAULT_STROKE_PAINT);
        setTransparency(DEFAULT_TRANSPARENCY);
    }
}