/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.gui.nodes;

import org.piccolo2d.PNode;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.nodes.PShape;
import org.piccolo2d.util.PBounds;
import org.piccolo2d.util.PPaintContext;

import java.awt.*;
import java.util.ArrayList;

/**
 * A version of {@link OutlinedObjects} that does not have {@link NeuronNode}s as children
 * but uses them to determine bounds.
 *
 * TODO: See if this can be merged to OutlinedObjects as a special case or if code can be shared.
 *
 * @author Lance Good
 * @author Jeff Yoshimi
 */
public class NeuronCollectionOutline extends PPath.Float {

    /**
     * The width and height of the arc in the rounded rectangle that surrounds the outlined objects.
     */
    public static final int ROUNDING_WIDTH_HEIGHT = 20;

    /**
     * Whether to draw an outline around entire set of grouped objects or not. In some cases a fill is enough.
     */
    private boolean drawOutline = true;

    /**
     * What outline color to use.
     */
    private Color lineColor = Color.gray;

    /**
     * What background color to use.
     */
    private Color backgroundColor = Color.white;

    /**
     * Indentation amount around drawn outline.
     */
    private int outlinePadding = 10;

    /**
     * References to neuron nodes used to determine bounds.
     */
    private ArrayList<NeuronNode> neuronNodeRefs = new ArrayList<>();

    /**
     * Construct the outlined objects group.
     */
    public NeuronCollectionOutline() {
        super();
        // This seems to be needed to initialize the paint system properly
        this.setVisible(true);
        this.setPaint(Color.gray);

        // TODO: Below just forces a repaint
        setBounds(10,10,10,10);

    }

    /**
     * Add a reference to the a neuron node for updating the outline bounds.
     */
    public void addChildRef(NeuronNode node) {
        neuronNodeRefs.add(node);
        // TODO: Outline not updated continuously as in OutlinedObjects, where the neuron nodes are
        // proper children of the PNode.  Commented out code (here and in validateFullBounds below) were efforts to fix,
        //but to no avail.
        //this.addPropertyChangeListener(PNode.PROPERTY_FULL_BOUNDS, node);
        //node.getNeuron().addPropertyChangeListener(event -> {
        //    if(event.getPropertyName().equals("moved")) {
        //        this.computeFullBounds(null);
        //        this.repaint();
        //    };
        //});
    }

    @Override
    public PBounds getFullBounds() {
        return getUnionOfBounds();
    }

    /**
     * Change the default paint to fill an expanded bounding box based on its children's bounds.
     */
    @Override
    public void paint(final PPaintContext ppc) {
        final Paint paint = getPaint();
        if (paint != null) {
            final Graphics2D g2 = ppc.getGraphics();
            final PBounds bounds = getUnionOfBounds();
            if (drawOutline) {
                g2.setPaint(lineColor);
                g2.drawRoundRect((int) bounds.getX() - outlinePadding, (int) bounds.getY() - outlinePadding, (int) bounds.getWidth() + 2 * outlinePadding, (int) bounds.getHeight() + 2 * outlinePadding, ROUNDING_WIDTH_HEIGHT, ROUNDING_WIDTH_HEIGHT);
            }
        }
    }

    /**
     * Get union of referenced neuron node bounds
     */
    private PBounds getUnionOfBounds() {
        PBounds resultBounds = new PBounds();
        for(PNode node : neuronNodeRefs) {
            resultBounds.add(node.getFullBoundsReference());
        }
        return resultBounds;
    }


    //@Override
    //public boolean validateFullBounds() {
    //    try {
    //        if (comparisonBounds == null) {
    //            System.out.println("Im null before.");
    //        }
    //        comparisonBounds = getUnionOfBounds();
    //        if (comparisonBounds == null) {
    //            System.out.println("Im null after.");
    //        }
    //    } catch (NullPointerException npe) {
    //        npe.printStackTrace();
    //    }
    //    if (!cachedChildBounds.equals(comparisonBounds)) {
    //        setPaintInvalid(true);
    //    }
    //    // TODO: We get smooth update when this returns true
    //    return super.validateFullBounds();
    //}

    public ArrayList<NeuronNode> getNeuronNodeRefs() {
        return neuronNodeRefs;
    }


}
