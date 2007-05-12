package org.simbrain.network.nodes;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * A custom outline that can be drawn around a collection of nodes.
 */
public class CustomOutline extends PPath implements PropertyChangeListener {

    /** References to outlined objects. */
    private ArrayList<PNode> outlinedObjects = new ArrayList<PNode>();

    /** Inset. */
    private final double INSET = 2.0d;

    /** @see PPath. */
    public void propertyChange(final PropertyChangeEvent arg0) {
        updateBounds();
    }

    /**
     * Updated bounds of outline based on location of its outlined objects.
     */
    public void updateBounds() {
        PBounds inputBounds = new PBounds();

        for (PNode node : outlinedObjects) {
            PBounds childBounds = node.getBounds();
            node.localToParent(childBounds);
            inputBounds.add(childBounds);
        }

        inputBounds.setRect(inputBounds.getX() - INSET,
                inputBounds.getY() - INSET,
                inputBounds.getWidth() + (2 * INSET),
                inputBounds.getHeight() + (2 * INSET));

        setPathToRectangle((float) inputBounds.getX(), (float) inputBounds.getY(),
                            (float) inputBounds.getWidth(), (float) inputBounds.getHeight());

    }

    /**
     * @return the outlinedObjects
     */
    public ArrayList<PNode> getOutlinedObjects() {
        return outlinedObjects;
    }

    /**
     * @param outlinedObjects the outlinedObjects to set
     */
    public void setOutlinedObjects(final ArrayList<PNode> outlinedObjects) {
        this.outlinedObjects = outlinedObjects;
    }


    /**
     * Remove one of the outlined aboves.
     *
     * @param node objet to remove.
     */
    public void removeOutlinedObject(final PNode node) {
        outlinedObjects.remove(node);
        if (outlinedObjects.isEmpty()) {
            this.removeFromParent();
        }
    }



}
