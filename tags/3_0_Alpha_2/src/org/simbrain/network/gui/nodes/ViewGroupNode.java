package org.simbrain.network.gui.nodes;

import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.actions.UngroupAction;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * A group of screen elements.
 */
public class ViewGroupNode extends ScreenElement implements PropertyChangeListener {

    /** Reference to grouped objects. */
    private ArrayList<ScreenElement> groupedObjects = new ArrayList<ScreenElement>();

    /** For computing offsets. */
    private Point2D.Double oldPosition = new Point2D.Double();

    /** For computing offsets. */
    private double xOffset = 0;

    /** For computing offsets. */
    private double yOffset = 0;

    /**
     * Construct text object at specified location.
     *
     * @param netPanel reference to networkPanel
     * @param ptext the styled text
     */
    public ViewGroupNode(final NetworkPanel netPanel, final ArrayList<ScreenElement> elements) {
        super(netPanel);
        PBounds bounds = new PBounds();
        for (ScreenElement element : elements) {
            if (!(element instanceof SubnetworkNode)) {
                element.setPickable(false);
                groupedObjects.add(element);
                bounds.add(element.getGlobalBounds());
            }
        }
        this.setBounds(bounds);
        addPropertyChangeListener(PROPERTY_FULL_BOUNDS, this);
    }

    /**
     * Update synapse node positions.
     */
    private void updateSynapseNodePositions() {
        for (Iterator i = getChildrenIterator(); i.hasNext(); ) {
            PNode node = (PNode) i.next();
            if (node instanceof NeuronNode) {
                NeuronNode neuronNode = (NeuronNode) node;
                neuronNode.updateSynapseNodePositions();
            }
        }
    }

    /** @Override. */
    public boolean isSelectable() {
        // TODO Auto-generated method stub
        return true;
    }

    /** @Override. */
    public boolean showSelectionHandle() {
        // TODO Auto-generated method stub
        return true;
    }

    /** @Override. */
    public boolean isDraggable() {
        // TODO Auto-generated method stub
        return true;
    }

    /** @Override. */
    protected boolean hasToolTipText() {
        // TODO Auto-generated method stub
        return false;
    }

    /** @Override. */
    protected String getToolTipText() {
        // TODO Auto-generated method stub
        return null;
    }

    /** @Override. */
    protected boolean hasContextMenu() {
        return true;
    }

    /** @Override. */ 
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        contextMenu.add(new UngroupAction(getNetworkPanel(), this));
        return contextMenu;
    }


    /** @Override. */
    protected boolean hasPropertyDialog() {
        // TODO Auto-generated method stub
        return false;
    }

    /** @Override. */
    protected JDialog getPropertyDialog() {
        // TODO Auto-generated method stub
        return null;
    }

    /** @Override. */
    public void resetColors() {
    }

    /** @See PNode. */
    public void propertyChange(PropertyChangeEvent arg0) {
        xOffset = oldPosition.getX() - this.getOffset().getX();
        yOffset = oldPosition.getY() - this.getOffset().getY();
        oldPosition = (Point2D.Double) this.getOffset();
        updateSynapseNodePositions();
        for (ScreenElement element : groupedObjects) {
            element.translate(-xOffset, -yOffset);
        }
    }

    /**
     * @return the groupedObjects
     */
    public ArrayList<ScreenElement> getGroupedObjects() {
        return groupedObjects;
    }

    /**
     * @param groupedObjects the groupedObjects to set
     */
    public void setGroupedObjects(ArrayList<ScreenElement> groupedObjects) {
        this.groupedObjects = groupedObjects;
    }

}
