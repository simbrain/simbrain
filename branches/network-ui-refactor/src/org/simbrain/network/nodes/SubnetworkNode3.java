
package org.simbrain.network.nodes;

import java.awt.Color;

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
    extends PNode implements PropertyChangeListener {

    /** Tab node. */
    private TabNode tab;

    /** Outline node. */
    private OutlineNode outline;


    /**
     * Create a new subnetwork node.
     */
    public SubnetworkNode3(final NetworkPanel networkPanel, final double x, final double y) {
        super();

        offset(x, y);
        setPickable(false);
        setChildrenPickable(true);

        tab = new TabNode(networkPanel, x, y);
        outline = new OutlineNode();

        super.addChild(tab);
        super.addChild(outline);

        tab.addPropertyChangeListener("fullBounds", outline);
        outline.addPropertyChangeListener(PPath.PROPERTY_PATH, tab);
    }


    /** @see PNode */
    public void addChild(final PNode child) {
        outline.addChild(child);
        child.addPropertyChangeListener("fullBounds", outline);
        addPropertyChangeListener(PROPERTY_FULL_BOUNDS, (PropertyChangeListener) child);
        addPropertyChangeListener(PROPERTY_FULL_BOUNDS, outline);
    }

    /** @see PropertyChangeListener */
    public void propertyChange(final PropertyChangeEvent event) {
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
            setOffset(0.0d, -22.0d);
            setBounds(0.0d, 0.0d, 80.0d, 22.0d);

            PText label = new PText("Subnetwork");
            label.offset(5.0f, 6.0f);

            PPath background = PPath.createRectangle(0.0f, 0.0f, 80.0f, 22.0f);
            background.setPaint(Color.LIGHT_GRAY);
            background.setStrokePaint(Color.DARK_GRAY);

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
            return false;
        }

        /** @see ScreenElement */
        protected String getToolTipText() {
            return null;
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
            /**
            PBounds bounds = outline.getBoundsReference();
            Rectangle2D rect = outline.localToParent(bounds);
            if (rect.getX() < 0) {
                setOffset(rect.getX(), 0.0d);
            }
            if (rect.getY() < 0) {
                setOffset(0.0d, rect.getY());
            }*/
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

            // presumably in local coordinates, offset from origin by parent's offset
            //setBounds(0.0f, 0.0f, 150.0f, 150.0f);
            setPathToRectangle(0.0f, 0.0f, 150.0f, 150.0f);
        }


        /** @see PropertyChangeListener */
        public void propertyChange(final PropertyChangeEvent event) {
            Object source = event.getSource();
            if (tab == source) {

                // tab node moved or was selected, need to find lower left corner
                Point2D lowerLeft = new Point2D.Double(0.0d, 22.0d);
                lowerLeft = tab.localToParent(lowerLeft);

                // move this outline such that its upper left corner matches
                //    the lower left corner of the tab node
                setOffset(lowerLeft.getX(), lowerLeft.getY());
            }
            else {

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
                //setBounds(bounds);
                setPathToRectangle((float) bounds.getX(), (float) bounds.getY(),
                                   (float) bounds.getWidth(), (float) bounds.getHeight());
            }
        }
    }
}