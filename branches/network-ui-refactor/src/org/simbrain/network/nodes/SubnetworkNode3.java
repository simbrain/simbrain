
package org.simbrain.network.nodes;

import java.awt.Color;
import java.awt.Paint;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.util.Iterator;

import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import edu.umd.cs.piccolo.PNode;

import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;

import edu.umd.cs.piccolo.util.PBounds;

import org.simbrain.network.NetworkPanel;

/**
 * Subnetwork node, third take.
 *
 * <p>
 * Node composition:
 * <pre>
 * SubnetworkNode extends PNode
 *   |
 *   + -- TabNode extends ScreenElement
 *   |      |
 *   |      + -- PText, for label
 *   |      |
 *   |      + -- PPath, for outline and background
 *   |
 *   + -- OutlineNode extends PPath
 *          |
 *          + -- ... (child nodes)
 * </pre>
 * </p>
 */
public final class SubnetworkNode3
    extends PNode {

    /** Tab height. */
    private static final double TAB_HEIGHT = 22.0d;

    /** Tab width. */
    private static final double TAB_WIDTH = 80.0d;

    /** Outline inset or border height. */
    private static final double OUTLINE_INSET_HEIGHT = 12.0d;

    /** Outline inset or border width. */
    private static final double OUTLINE_INSET_WIDTH = 12.0d;

    /** Default outline height. */
    private static final double DEFAULT_OUTLINE_HEIGHT = 150.0d;

    /** Default outline width. */
    private static final double DEFAULT_OUTLINE_WIDTH = 150.0d;

    /** Default tab paint. */
    private static final Paint DEFAULT_TAB_PAINT = Color.LIGHT_GRAY;

    /** Default tab stroke paint. */
    private static final Paint DEFAULT_TAB_STROKE_PAINT = Color.DARK_GRAY;

    /** Default outline stroke paint. */
    private static final Paint DEFAULT_OUTLINE_STROKE_PAINT = Color.LIGHT_GRAY;

    /** Default label. */
    private static final String DEFAULT_LABEL = "Subnetwork";

    /** Tab node. */
    private TabNode tab;

    /** Outline node. */
    private OutlineNode outline;

    /** The tab paint for this subnetwork node. */
    private Paint tabPaint;

    /** The tab stroke paint for this subnetwork node. */
    private Paint tabStrokePaint;

    /** The outline stroke paint for this subnetwork node. */
    private Paint outlineStrokePaint;

    /** The label for this subnetwork node. */
    private String label;

    /** True if this subnetwork node is to show its outline. */
    private boolean showOutline;


    /**
     * Create a new subnetwork node.
     */
    public SubnetworkNode3(final NetworkPanel networkPanel, final double x, final double y) {
        super();

        offset(x, y);
        setPickable(false);
        setChildrenPickable(true);

        tabPaint = DEFAULT_TAB_PAINT;
        tabStrokePaint = DEFAULT_TAB_STROKE_PAINT;
        outlineStrokePaint = DEFAULT_OUTLINE_STROKE_PAINT;
        label = DEFAULT_LABEL;
        showOutline = true;

        tab = new TabNode(networkPanel, x, y);
        outline = new OutlineNode();

        super.addChild(outline);
        super.addChild(tab);

        outline.addPropertyChangeListener("bounds", tab);
    }


    /** @see PNode */
    protected void layoutChildren() {
        // attach the outline to the lower left corner of the tab
        Point2D lowerLeft = new Point2D.Double(0.0d, TAB_HEIGHT);
        lowerLeft = tab.localToParent(lowerLeft);
        outline.setOffset(lowerLeft.getX(), lowerLeft.getY());
    }

    /** @see PNode */
    public void addChild(final PNode child) {
        outline.addChild(child);
        child.addPropertyChangeListener("fullBounds", outline);
    }

    //
    // bound properties

    /**
     * Return the tab paint for this subnetwork node.
     * The tab paint will not be null.
     *
     * @return the tab paint for this subnetwork node
     */
    public final Paint getTabPaint() {
        return tabPaint;
    }

    /**
     * Set the tab paint for this subnetwork node to <code>tabPaint</code>.
     *
     * <p>This is a bound property.</p>
     *
     * @param tabPaint tab paint for this subnetwork node, must not be null
     */
    public final void setTabPaint(final Paint tabPaint) {
        if (tabPaint == null) {
            throw new IllegalArgumentException("tabPaint must not be null");
        }

        Paint oldTabPaint = this.tabPaint;
        this.tabPaint = tabPaint;
        // TODO:  update tab paint, directly or via property change listener
        firePropertyChange("tabPaint", oldTabPaint, this.tabPaint);
    }

    /**
     * Return the tab stroke paint for this subnetwork node.
     * The tab stroke paint will not be null.
     *
     * @return the tab stroke paint for this subnetwork node
     */
    public final Paint getTabStrokePaint() {
        return tabStrokePaint;
    }

    /**
     * Set the tab stroke paint for this subnetwork node to <code>tabStrokePaint</code>.
     *
     * <p>This is a bound property.</p>
     *
     * @param tabStrokePaint tab stroke paint for this subnetwork node, must not be null
     */
    public final void setTabStrokePaint(final Paint tabStrokePaint) {
        if (tabStrokePaint == null) {
            throw new IllegalArgumentException("tabStrokePaint must not be null");
        }

        Paint oldTabStrokePaint = this.tabStrokePaint;
        this.tabStrokePaint = tabStrokePaint;
        // TODO:  update tab stroke paint, directly or via property change listener
        firePropertyChange("tabStrokePaint", oldTabStrokePaint, this.tabStrokePaint);
    }

    /**
     * Return the outline stroke paint for this subnetwork node.
     * The outline stroke paint will not be null.
     *
     * @return the outline stroke paint for this subnetwork node
     */
    public final Paint getOutlineStrokePaint() {
        return outlineStrokePaint;
    }

    /**
     * Set the outline stroke paint for this subnetwork node to <code>outlineStrokePaint</code>.
     *
     * <p>This is a bound property.</p>
     *
     * @param outlineStrokePaint outline stroke paint for this subnetwork node, must not be null
     */
    public final void setOutlineStrokePaint(final Paint outlineStrokePaint) {
        if (outlineStrokePaint == null) {
            throw new IllegalArgumentException("outlineStrokePaint must not be null");
        }

        Paint oldOutlineStrokePaint = this.outlineStrokePaint;
        this.outlineStrokePaint = outlineStrokePaint;
        // TODO:  update outline stroke paint, directly or via property change listener
        firePropertyChange("outlineStrokePaint", oldOutlineStrokePaint, this.outlineStrokePaint);
    }

    /**
     * Return the label for this subnetwork node.
     * The label may be null.
     *
     * @return the label for this subnetwork node
     */
    public final String getLabel() {
        return label;
    }

    /**
     * Set the label for this subnetwork node to <code>label</code>.
     *
     * <p>This is a bound property.</p>
     *
     * @param label label for this subnetwork node
     */
    public final void setLabel(final String label) {
        String oldLabel = this.label;
        this.label = label;
        // TODO:  update tab label text, directly or via property change listener
        firePropertyChange("label", oldLabel, this.label);
    }

    /**
     * Return true if this subnetwork node is to show its outline.
     *
     * @return true if this subnetwork node is to show its outline
     */
    public boolean getShowOutline() {
        return showOutline;
    }

    /**
     * Set to true if this subnetwork node is to show its outline.
     *
     * <p>This is a bound property.</b>
     *
     * @param showOutline true if this subnetwork node is to show its outline
     */
    public void setShowOutline(final boolean showOutline) {
        boolean oldShowOutline = this.showOutline;
        this.showOutline = showOutline;
        // TODO:  set outline stroke appropriately, directly or via property change listener
        firePropertyChange("showOutline", Boolean.valueOf(oldShowOutline), Boolean.valueOf(showOutline));
    }


    /**
     * Tab node.
     */
    private class TabNode
        extends ScreenElement
        implements PropertyChangeListener {

        /**
         * Create a new tab node.
         */
        public TabNode(final NetworkPanel networkPanel, final double x, final double y) {
            super(networkPanel);

            setPickable(true);
            setChildrenPickable(false);
            setOffset(0.0d, -1 * TAB_HEIGHT);
            setBounds(0.0d, 0.0d, TAB_WIDTH, TAB_HEIGHT);

            PText label = new PText(getLabel());
            label.offset(5.0f, 6.0f);

            PPath background = PPath.createRectangle(0.0f, 0.0f, (float) TAB_WIDTH, (float) TAB_HEIGHT);
            background.setPaint(getTabPaint());
            background.setStrokePaint(getTabStrokePaint());

            addChild(background);
            addChild(label);
        }


        /** @see ScreenElement */
        public boolean isSelectable() {
            return true;
        }

        /** @see ScreenElement */
        public boolean isDraggable() {
            return true;
        }

        /** @see ScreenElement */
        protected boolean hasToolTipText() {
            return true;
        }

        /** @see ScreenElement */
        protected String getToolTipText() {
            return getLabel();
        }

        /** @see ScreenElement */
        protected boolean hasContextMenu() {
            return false;
        }

        /** @see ScreenElement */
        protected JPopupMenu getContextMenu() {
            return null;
        }

        /** @see ScreenElement */
        protected boolean hasPropertyDialog() {
            return false;
        }

        /** @see ScreenElement */
        protected JDialog getPropertyDialog() {
            return null;
        }

        /** @see ScreenElement */
        public void resetColors() {
            // empty
        }

        /** @see PropertyChangeListener */
        public void propertyChange(final PropertyChangeEvent event) {
            // TODO:
            // attach the tab to the top left corner of the outline
            // (using setOffset prevents dragging from working)
        }
    }

    /**
     * Outline node.
     */
    private class OutlineNode
        extends PPath
        implements PropertyChangeListener {

        /**
         * Create a new outline node.
         */
        public OutlineNode() {
            super();

            setPickable(false);
            setChildrenPickable(true);
            setBounds(0.0d, 0.0d, DEFAULT_OUTLINE_WIDTH, DEFAULT_OUTLINE_HEIGHT);
            setPathToRectangle(0.0f, 0.0f, (float) DEFAULT_OUTLINE_WIDTH, (float) DEFAULT_OUTLINE_HEIGHT);

            setStrokePaint(DEFAULT_OUTLINE_STROKE_PAINT);
        }


        /** @see PropertyChangeListener */
        public void propertyChange(final PropertyChangeEvent event) {

            // one of the child nodes' full bounds changed
            PBounds bounds = new PBounds();
            for (Iterator i = getChildrenIterator(); i.hasNext(); ) {
                PNode child = (PNode) i.next();
                PBounds childBounds = child.getBounds();
                child.localToParent(childBounds);
                bounds.add(childBounds);
            }

            // add (0.0d, 0.0d)
            bounds.add(12.0d, 12.0d);
            // add border
            bounds.setRect(bounds.getX() - 12.0d, bounds.getY() - 12.0d,
                           bounds.getWidth() + 24.0d, bounds.getHeight() + 24.0d);

            // set outline to new bounds
            // TODO:  only update rect if it needs updating
            setBounds(bounds);
            setPathToRectangle((float) bounds.getX(), (float) bounds.getY(),
                               (float) bounds.getWidth(), (float) bounds.getHeight());
        }
    }
}