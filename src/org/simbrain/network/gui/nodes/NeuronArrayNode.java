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
import org.piccolo2d.nodes.PText;
import org.piccolo2d.util.PBounds;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronArray;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.neuron.NeuronDialog;
import org.simbrain.network.neuron_update_rules.interfaces.ActivityGenerator;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;

//import org.piccolo2d.nodes.PText;

/**
 * <b>NeuronNode</b> is a Piccolo PNode corresponding to a Neuron in the neural
 * network model.
 */
@SuppressWarnings("serial")
public class NeuronArrayNode extends ScreenElement  {

    /**
     * The logical neuron array this screen element represents.
     */
    protected NeuronArray neuronArray;

    /**
     * Square shape for representing activity generators.
     */
    private PPath square = PPath.createRectangle(0 ,0,50,25);

    /**
     * Number text inside neuron.
     */
    private PText infoText;

    /**
     * Font for info text.
     */
    public static final Font INFO_FONT = new Font("Arial", Font.PLAIN, 7);


    /**
     * Create a new neuron array node.
     *
     * @param net    Reference to NetworkPanel
     * @param na reference to model neuron array
     */
    public NeuronArrayNode(final NetworkPanel net, final NeuronArray na) {
        super(net);
        this.neuronArray = na;
        this.centerFullBoundsOnPoint(na.getX(), na.getY());
        init();
    }

    /**
     * Initialize the NeuronNode.
     */
    private void init() {


        square.setPickable(true);
        addChild(square);
        PBounds bounds = square.getBounds();
        setBounds(bounds);

        infoText = new PText("rows: " + neuronArray.getRows() + "\n cols:"
            + neuronArray.getCols());
        infoText.setFont(INFO_FONT);
        addChild(infoText);
        infoText.offset(4,4);

        //addPropertyChangeListener(PROPERTY_FULL_BOUNDS, this);

    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public boolean showSelectionHandle() {
        return true;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    @Override
    protected boolean hasToolTipText() {
        return true;
    }

    @Override
    protected String getToolTipText() {
        return "" + neuronArray.getNeuronArray().sumNumber();
    }

    @Override
    protected boolean hasContextMenu() {
        return false;
    }

    @Override
    protected JPopupMenu getContextMenu() {
        return null;
    }

    @Override
    protected boolean hasPropertyDialog() {
        return false;
    }

    @Override
    protected JDialog getPropertyDialog() {
        return null;
    }

    @Override
    public void resetColors() {

    }


}
