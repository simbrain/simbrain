package org.simbrain.util.piccolo;

import org.piccolo2d.PNode;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.util.PBounds;
import org.simbrain.network.gui.nodes.ScreenElement;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Set;

/**
 * A node that draws an outline around a set of nodes.
 */
public class Outline extends PNode {

    /**
     * The width and height of the arc in the rounded rectangle that surrounds the outlined objects.
     */
    public static final int ARC_SIZE = 20;

    /**
     * Margin in pixels around outlined object.
     */
    private int outlinePadding = 10;

    /**
     * The actual outline object.
     */
    private PPath outline = PPath.createRoundRectangle(0, 0, 0, 0, ARC_SIZE, ARC_SIZE);

    private Collection<? extends ScreenElement> outlinedNodes = null;

    private boolean dirtyBounds = false;

    private final PropertyChangeListener markBoundsDirty = (evt) -> dirtyBounds = true;

    /**
     * Construct the outline object.
     */
    public Outline() {
        outline.setPaint(Color.gray);
        addChild(outline);
        outline.setVisible(true);
        setPickable(false);
    }

    /**
     * Use this method to force the outline to surrounded the provided nodes.  Note that outlinednodes is
     * set to null at every update. Also note that no event is fired when setting outlined nodes.
     * Update only occurs after {@link #updateBounds()} is called.
     */
    public void resetOutlinedNodes(Set<? extends ScreenElement> outlinedNodes) {
        if (this.outlinedNodes != null) {
            this.outlinedNodes.forEach(node -> node.removePropertyChangeListener(PROPERTY_FULL_BOUNDS, markBoundsDirty));
        }
        outlinedNodes.forEach(node -> node.addPropertyChangeListener(PROPERTY_FULL_BOUNDS, markBoundsDirty));
        this.outlinedNodes = outlinedNodes;
        dirtyBounds = true;
        invalidateFullBounds();
    }

    @Override
    public boolean validateFullBounds() {
        if (dirtyBounds) {
            PBounds bounds = new PBounds();
            for (ScreenElement node : outlinedNodes) {
                bounds.add(node.getFullBounds());
            }
            updateBound(bounds.x, bounds.y, bounds.width, bounds.height);
            dirtyBounds = false;
        }
        return super.validateFullBounds();
    }

    /**
     * Update the bounds of this outline object based on the locations of its {@link #outlinedNodes}.
     */
    public void updateBounds() {
        invalidateFullBounds();
    }

    /**
     * Update the outline object to specified bounds.
     */
    private void updateBound(double x, double y, double width, double height) {
        removeChild(outline);
        outline = PPath.createRoundRectangle(
                x - outlinePadding,
                y - outlinePadding,
                width + outlinePadding * 2,
                height + outlinePadding * 2,
                ARC_SIZE,
                ARC_SIZE
        );
        outline.setStrokePaint(Color.gray);
        outline.setPaint(null);
        addChild(outline);
    }

    public int getOutlinePadding() {
        return outlinePadding;
    }

    public void setOutlinePadding(int outlinePadding) {
        this.outlinePadding = outlinePadding;
    }

}
