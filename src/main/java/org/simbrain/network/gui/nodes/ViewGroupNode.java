package org.simbrain.network.gui.nodes;

import org.piccolo2d.PNode;
import org.piccolo2d.util.PBounds;
import org.simbrain.network.LocatableModel;
import org.simbrain.network.gui.NetworkPanel;

import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A group of screen elements.
 */
public class ViewGroupNode extends ScreenElement implements PropertyChangeListener {

    // Has not been used since roughly Simbrain 1.0

    /**
     * Reference to grouped objects.
     */
    private ArrayList<ScreenElement> groupedObjects = new ArrayList<ScreenElement>();

    /**
     * For computing offsets.
     */
    private Point2D.Double oldPosition = new Point2D.Double();

    /**
     * For computing offsets.
     */
    private double xOffset = 0;

    /**
     * For computing offsets.
     */
    private double yOffset = 0;

    /**
     * Construct text object at specified location.
     *
     * @param netPanel reference to networkPanel
     * @param elements the styled text
     */
    public ViewGroupNode(final NetworkPanel netPanel, final ArrayList<ScreenElement> elements) {
        super(netPanel);
        PBounds bounds = new PBounds();
        for (ScreenElement element : elements) {
            element.setPickable(false);
            groupedObjects.add(element);
            bounds.add(element.getGlobalBounds());
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

    @Override
    public boolean isDraggable() {
        return true;
    }

    @Override
    public LocatableModel getModel() {
        // TODO: if this is ever brought back to life
        return null;
    }

    @Override
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
