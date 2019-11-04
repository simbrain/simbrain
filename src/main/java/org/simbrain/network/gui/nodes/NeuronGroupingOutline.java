package org.simbrain.network.gui.nodes;

import org.piccolo2d.PNode;
import org.piccolo2d.nodes.PPath;

import java.awt.*;

/**
 * A node that draws an outline around its children nodes.
 */
public class NeuronGroupingOutline extends PNode {

    /**
     * The width and height of the arc in the rounded rectangle that surrounds
     * the outlined objects.
     */
    public static final int ARC_SIZE = 20;

    /**
     * Indentation amount around drawn outline.
     */
    private int outlinePadding = 10 + NeuronNode.DIAMETER / 2;


    /**
     * Whether to draw an outline around entire set of grouped objects or not.
     * In some cases a fill is enough.
     */
    private boolean drawOutline = true;

    /**
     * What background color to use.
     */
    private Color backgroundColor = null;

    private PPath outline = PPath.createRoundRectangle(0, 0, 0, 0, ARC_SIZE, ARC_SIZE);

    public NeuronGroupingOutline() {
        outline.setPaint(Color.gray);
        addChild(outline);
        outline.setVisible(true);
        setPickable(false);
    }

    public void updateBound(double x, double y, double width, double height) {
        removeChild(outline);
        if (drawOutline) {
            outline = PPath.createRoundRectangle(x - outlinePadding,
                    y - outlinePadding,
                    width + outlinePadding * 2,
                    height + outlinePadding * 2,
                    ARC_SIZE,
                    ARC_SIZE
            );
            outline.setStrokePaint(Color.gray);
            outline.setPaint(backgroundColor);
            addChild(outline);
        }
    }

    public void setFillBackground(boolean fillBackground) {
        backgroundColor = fillBackground ? Color.WHITE : null;
    }

    public int getOutlinePadding() {
        return outlinePadding;
    }

    public void setOutlinePadding(int outlinePadding) {
        this.outlinePadding = outlinePadding;
    }

    public boolean isDrawOutline() {
        return drawOutline;
    }

    public void setDrawOutline(boolean drawOutline) {
        this.drawOutline = drawOutline;
    }
}
