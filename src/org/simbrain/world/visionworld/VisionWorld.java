/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.world.visionworld;

import java.awt.Color;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JToolTip;

import org.simbrain.util.JMultiLineToolTip;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;

import org.simbrain.world.visionworld.action.CreatePixelMatrixAction;
import org.simbrain.world.visionworld.action.CreateSensorMatrixAction;
import org.simbrain.world.visionworld.action.NormalViewAction;
import org.simbrain.world.visionworld.action.StackedViewAction;

import org.simbrain.world.visionworld.dialog.CreatePixelMatrixDialog;
import org.simbrain.world.visionworld.dialog.CreateSensorMatrixDialog;

import org.simbrain.world.visionworld.node.PixelMatrixImageNode;
import org.simbrain.world.visionworld.node.SensorMatrixNode;
import org.simbrain.world.visionworld.node.SensorNode;

/**
 * Vision world.
 */
public final class VisionWorld
    extends PCanvas {

    /** Model for this vision world. */
    private final VisionWorldModel model;

    /** Sensor selection model for this vision world. */
    //private final SensorSelectionModel selectionModel;

    /** Model listener. */
    private final VisionWorldModelListener modelListener;

    /** Selection listener. */
    //private final SensorSelectionListener selectionListener;

    /** Focus owner. */
    private PNode focusOwner;

    /** Pixel matrix node. */
    private PixelMatrixImageNode pixelMatrixNode;

    /** Sensor matrix node. */
    private SensorMatrixNode sensorMatrixNode;

    /** View padding. */
    private static final double VIEW_PADDING = 10.0d;


    /**
     * Create a new vision world with the specified model.
     *
     * @param model model for this vision world, must not be null
     */
    public VisionWorld(final VisionWorldModel model) {
        super();
        if (model == null) {
            throw new IllegalArgumentException("model must not be null");
        }
        this.model = model;

        setOpaque(true);
        setBackground(Color.WHITE);
        setDefaultRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        setAnimatingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        setInteractingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        removeInputEventListener(getPanEventHandler());
        removeInputEventListener(getZoomEventHandler());

        createNodes();
        modelListener = new VisionWorldModelListener() {

                /** {@inheritDoc} */
                public void pixelMatrixChanged(final VisionWorldModelEvent event) {
                    getLayer().removeChild(pixelMatrixNode);
                    pixelMatrixNode = new PixelMatrixImageNode(event.getPixelMatrix());
                    //pixelMatrixNode.addInputEventListener(new FocusHandler(pixelMatrixNode));
                    setFocusOwner(pixelMatrixNode);
                    //selectionModel.clear();
                    getLayer().addChild(pixelMatrixNode);
                }

                /** {@inheritDoc} */
                public void sensorMatrixChanged(final VisionWorldModelEvent event) {
                    getLayer().removeChild(sensorMatrixNode);
                    sensorMatrixNode = new SensorMatrixNode(event.getSensorMatrix());
                    //pixelMatrixNode.addInputEventListener(new FocusHandler(pixelMatrixNode));
                    setFocusOwner(sensorMatrixNode);
                    //selectionModel.clear();
                    getLayer().addChild(sensorMatrixNode);
                }
            };

        this.model.addModelListener(modelListener);

        //selectionModel = new SensorSelectionModel(this);
        //        selectionListener = new SensorSelectionListener()
        //    {
        //        /** {@inheritDoc} */
        //        public void selectionChanged(SensorSelectionEvent e) {
        //            updateSelection(e);
        //        }
        //    };

        //selectionModel.addSensorSelectionListener(selectionListener);

        //addInputEventListener(new SelectionEventHandler(this));
    }

    /**
     * Create nodes.
     */
    private void createNodes() {
        PLayer layer = getLayer();
        pixelMatrixNode = new PixelMatrixImageNode(model.getPixelMatrix());
        //pixelMatrixNode.addInputEventListener(new FocusHandler(pixelMatrixNode));
        layer.addChild(pixelMatrixNode);

        sensorMatrixNode = new SensorMatrixNode(model.getSensorMatrix());
        //sensorMatrixNode.addInputEventListener(new MouseoverHighlighter(sensorMatrixNode));
        layer.addChild(sensorMatrixNode);
    }

    /**
     * Update selection.
     *
     * @param event sensor selection event
     */
    private void updateSelection(SensorSelectionEvent event) {

        updateSensorNodes();
        Set<Sensor> selection = event.getSelection();
        Set<Sensor> oldSelection = event.getOldSelection();
        Set<Sensor> difference = new HashSet<Sensor>(oldSelection);
        difference.removeAll(selection);

        for (Sensor sensor : difference) {
            //sensorNodes.get(sensor).setSelected(false);
        }
        for (Sensor sensor : selection) {
            //sensorNodes.get(sensor).setSelected(true);
        }
    }

    private void updateSensorNodes() {
        //sensorNodes.clear();

        Collection allNodes = getLayer().getAllNodes();
        for (Iterator i = allNodes.iterator(); i.hasNext(); ) {
            PNode node = (PNode) i.next();
            if (node instanceof SensorNode) {
                SensorNode sensorNode = (SensorNode) node;
                //sensorNodes.put(sensorNode.getSensor(), sensorNode);
            }
        }
    }

    /**
     * Center camera.
     */
    private void centerCamera() {
        PLayer layer = getLayer();
        PCamera camera = getCamera();
        PBounds fullBounds = layer.getFullBoundsReference();
        PBounds paddedBounds = new PBounds(fullBounds.getX() - VIEW_PADDING,
                                           fullBounds.getY() - VIEW_PADDING,
                                           fullBounds.getHeight() + (2 * VIEW_PADDING),
                                           fullBounds.getWidth() + (2 * VIEW_PADDING));
        camera.animateViewToCenterBounds(paddedBounds, true, 0L);
    }

    PNode getFocusOwner() {
        return focusOwner;
    }

    void setFocusOwner(final PNode focusOwner) {
        /*
        if ((pixelMatrixNode.equals(focusOwner)) || (sensorMatrixNodes.containsValue(focusOwner))) {
            PNode oldFocusOwner = this.focusOwner;
            this.focusOwner = focusOwner;
            // also need to call setFocus(boolean) on pixelMatrixNode
            firePropertyChange("focusOwner", oldFocusOwner, this.focusOwner);
        }
        */
    }

    /*
    void focusNext() {
    }

    void focusPrevious() {
    }
    */

    /** {@inheritDoc} */
    public void repaint() {
        super.repaint();
        if (getLayer().getChildrenCount() > 0) {
            centerCamera();
        }
    }

    /** {@inheritDoc} */
    public JToolTip createToolTip() {
        return new JMultiLineToolTip();
    }

    /**
     * Return the model for this vision world.
     * The model will not be null.
     *
     * @return the model for this vision world
     */
    public VisionWorldModel getModel() {
        return model;
    }

    /**
     * Return the sensor selection model for this vision world.
     * The sensor selection model will not be null.
     *
     * @return the sensor selection model for this vision world
     */
    public SensorSelectionModel getSensorSelectionModel() {
        //return selectionModel;
        return null;
    }

    /**
     * Switch to the normal view.
     */
    public void normalView() {
        sensorMatrixNode.setOffset(0.0d, 0.0d);
    }

    /**
     * Switch to the stacked view.
     */
    public void stackedView() {
        sensorMatrixNode.setOffset(-20.0d, 20.0d);
    }

    /**
     * Mouseover highlighter.
     */
    private class MouseoverHighlighter
        extends PBasicInputEventHandler {

        /** Node for this mouseover highlighter. */
        private final SensorMatrixNode node;


        /**
        * Create a new mouseover highlighter for the specified node.
        *
        * @param node node
        */
        MouseoverHighlighter(final SensorMatrixNode node) {
            super();
            this.node = node;
        }


        /** {@inheritDoc} */
        public void mouseEntered(final PInputEvent event) {
            node.setTransparency(1.0f);
            node.moveToFront();
        }

        /** {@inheritDoc} */
        public void mouseExited(final PInputEvent event) {
            node.setTransparency(0.6f);
        }
    }

    /**
     * Focus handler.
     */
    private class FocusHandler
        extends PBasicInputEventHandler {

        /** Node for this focus handler. */
        private final PixelMatrixImageNode node;


        /**
         * Create a new focus handler for the specified node.
         *
         * @param node node
         */
        FocusHandler(final PixelMatrixImageNode node) {
            super();
            this.node = node;
            node.setOutlinePaint(Color.BLACK);
        }


        /** {@inheritDoc} */
        public void mouseEntered(final PInputEvent event) {
            node.setFocus(true);
            node.moveToFront();
            node.setOutlinePaint(Color.RED);
            node.repaint();
        }

        /** {@inheritDoc} */
        public void mouseExited(final PInputEvent event) {
            node.setFocus(false);
            node.setOutlinePaint(Color.BLACK);
            node.repaint();
        }
    }

    /**
     * Create pixel matrix.
     */
    public void createPixelMatrix() {
        CreatePixelMatrixDialog d = new CreatePixelMatrixDialog(this);
        d.setBounds(100, 100, 450, 500);
        d.setVisible(true);
    }

    /**
     * Add sensor matrix.
     */
    public void createSensorMatrix() {
        CreateSensorMatrixDialog d = new CreateSensorMatrixDialog(this);
        d.setBounds(100, 100, 450, 550);
        d.setVisible(true);
    }

    /**
     * Return a list of file menu actions for this vision world.
     *
     * @return a list of file menu actions for this vision world
     */
    public List<Action> getFileMenuActions() {
        return Arrays.asList(new Action[] {new CreatePixelMatrixAction(this), new CreateSensorMatrixAction(this)});
    }

    /**
     * Return a list of edit menu actions for this vision world.
     *
     * @return a list of edit menu actions for this vision world
     */
    public List<Action> getEditMenuActions() {
        return Collections.<Action>emptyList();
    }

    /**
     * Return a list of view menu actions for this vision world.
     *
     * @return a list of view menu actions for this vision world
     */
    public List<Action> getViewMenuActions() {
        return Arrays.asList(new Action[] { new NormalViewAction(this), new StackedViewAction(this) });
    }
}
