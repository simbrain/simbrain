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

import org.piccolo2d.nodes.PImage;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.nodes.PText;
import org.piccolo2d.util.PBounds;
import org.piccolo2d.util.PPaintContext;
import org.simbrain.network.events.LocationEvents;
import org.simbrain.network.gui.NetworkDialogsKt;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.actions.edit.CopyAction;
import org.simbrain.network.gui.actions.edit.CutAction;
import org.simbrain.network.gui.actions.edit.DeleteAction;
import org.simbrain.network.gui.actions.edit.PasteAction;
import org.simbrain.network.matrix.NeuronArray;
import org.simbrain.util.ImageKt;
import org.simbrain.util.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.table.NumericTable;
import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.util.table.SimbrainJTableScrollPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import static org.simbrain.network.gui.NetworkPanelMenusKt.createCouplingMenu;
import static org.simbrain.util.GeomKt.minus;
import static org.simbrain.util.GeomKt.plus;

/**
 * <b>NeuronNode</b> is a Piccolo PNode corresponding to a Neuron in the neural
 * network model.
 */
@SuppressWarnings("serial")
public class NeuronArrayNode extends ScreenElement {

    /**
     * The logical neuron array this screen element represents.
     */
    protected NeuronArray neuronArray;

    /**
     * Height in pixels of the pixel display showing the activations.
     */
    private double activationImageHeight = 10;

    /**
     * If true, show the image array as a grid; if false show it as a horizontal line.
     */
    private boolean gridMode = false;

    /**
     * Width of border in pixels.
     */
    private final int borderPixels = 10;

    /**
     * Height of array when in "flat" mode.
     */
    private final int flatPixelArrayHeight = 10;

    /**
     * Heavy stroke for clamped nodes.
     */
    private static final BasicStroke CLAMPED_STROKE = new BasicStroke(2f);

    /**
     * Stroke to use for bounding box. Toggles from clamped to default.
     */
    private Stroke boundaryStroke = DEFAULT_STROKE;

    /**
     * Text showing info about the array.
     */
    private PText infoText;

    /**
     * Square shape around array node.
     */
    private PPath borderBox;

    /**
     * Only update bounds when this is null.
     */
    private PBounds boundsCache = null;

    /**
     * Image to show activationImage.
     */
    private PImage activationImage = new PImage();

    /**
     * Font for info text.
     */
    public static final Font INFO_FONT = new Font("Arial", Font.PLAIN, 8);

    /**
     * Parent network panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * Create a new neuron array node.
     *
     * @param np Reference to NetworkPanel
     * @param na reference to model neuron array
     */
    public NeuronArrayNode(final NetworkPanel np, final NeuronArray na) {
        super(np);
        this.neuronArray = na;
        networkPanel = np;

        LocationEvents events = neuronArray.getEvents();
        events.onDeleted(n -> removeFromParent());
        events.onUpdated(() -> {
            renderArrayToActivationsImage();
            updateInfoText();
        });
        events.onClampChanged(this::updateClampStatus);
        events.onLocationChange(this::pullViewPositionFromModel);
        events.onLabelChange((o, n) -> {
            //interactionBox.setText((String) evt.getNewValue());
            //interactionBox.updateText();
        });

        // Set up main items
        setPickable(true);
        addChild(activationImage);
        borderBox = PPath.createRectangle(0, 0, getWidth(), getHeight());
        addChild(borderBox);

        // Info text
        infoText = new PText();
        infoText.setFont(INFO_FONT);
        addChild(infoText);
        infoText.offset(8, 8);
        updateInfoText();

        pullViewPositionFromModel();

        // Image array
        renderArrayToActivationsImage();

        updateClampStatus();

    }

    private void updateClampStatus() {
        if (neuronArray.isClamped()) {
            boundaryStroke = CLAMPED_STROKE;
        } else {
            boundaryStroke = DEFAULT_STROKE;
        }
        boundsCache = null;
        validateFullBounds();
    }

    public void pullViewPositionFromModel() {
        Point2D point = minus(neuronArray.getLocation(), new Point2D.Double(getWidth() / 2, getHeight() / 2));
        this.setGlobalTranslation(point);
    }

    /**
     * Update the position of the model neuron based on the global coordinates
     * of this pnode.
     */
    public void pushViewPositionToModel() {
        Point2D p = this.getGlobalTranslation();
        neuronArray.setLocation(plus(p, new Point2D.Double(getWidth() / 2, getHeight() / 2)));
    }

    @Override
    public void offset(double dx, double dy) {
        pushViewPositionToModel();
        super.offset(dx, dy);
    }

    /**
     * Render an image and set it to {@link #activationImage} to show the current activationImage.
     * <p>
     * Will not render when {@link NeuronArray#isRenderActivations()} is set to false.
     */
    private void renderArrayToActivationsImage() {

        // if (neuronArray.getPrototypeRule() instanceof SpikingNeuronUpdateRule) {
        //     // TODO: Use this to place a yellow grid over pixels for spiking components
        //     System.out.println(
        //             Arrays.toString(((DataHolder.SpikingDataHolder)
        //                     neuronArray.getDataHolder()).spikes));
        // }

        if (neuronArray.isRenderActivations()) {

            if (gridMode) {
                // "Grid" case
                double[] activations = neuronArray.getActivations();
                int len = (int) Math.sqrt(activations.length);
                BufferedImage img = ImageKt.toSimbrainColorImage(
                        activations,len,len);
                activationImage.setImage(img);
                // TODO: Adjust this to look nice
                // TODO: Magic numbers
                this.activationImage.setBounds(5, infoText.getHeight() + 10, 100, 100);

            } else {
                // "Flat" case
                double[] activations = neuronArray.getActivations();
                BufferedImage img = ImageKt.toSimbrainColorImage(activations,activations.length, 1);
                activationImage.setImage(img);
                this.activationImage.setBounds(borderPixels, infoText.getHeight() + borderPixels,
                        infoText.getWidth() - borderPixels, flatPixelArrayHeight);
            }

            // Forces the bounds to be updated
            boundsCache = null;

            // Repaint
            networkPanel.zoomToFitPage();

        }

    }

    public NeuronArray getNeuronArray() {
        return neuronArray;
    }

    /**
     * Update status text.
     */
    private void updateInfoText() {
        infoText.setText(
                "" + neuronArray.getLabel() + "    " +
                        "nodes: " + neuronArray.getActivations().length
                        + "\nmean activation: "
                        + SimbrainMath.roundDouble(Arrays.stream(neuronArray.getActivations()).average().orElse(0), 4));
    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    @Override
    public String getToolTipText() {
        return neuronArray.toString();
    }

    @Override
    public JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();

        // Edit Menu
        contextMenu.add(new CutAction(getNetworkPanel()));
        contextMenu.add(new CopyAction(getNetworkPanel()));
        contextMenu.add(new PasteAction(getNetworkPanel()));
        contextMenu.addSeparator();
        Action editArray = new AbstractAction("Edit...") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                StandardDialog dialog = getArrayDialog();
                dialog.setVisible(true);
            }
        };
        contextMenu.add(editArray);
        contextMenu.add(new DeleteAction(getNetworkPanel()));
        contextMenu.addSeparator();

        Action connectArrays = new AbstractAction("Connect arrays...") {
            {
                putValue(SHORT_DESCRIPTION, "Connect arrays with a connector");
            }

            @Override
            public void actionPerformed(final ActionEvent event) {
                NetworkDialogsKt.createConnector(networkPanel);
            }
        };
        contextMenu.add(connectArrays);
        contextMenu.addSeparator();


        // Layout style
        // TODO: Add a third "LooseNeuron" mode.  It can also be grid or line.  Only allow it for < 1K or some number
        Action switchStyle = new AbstractAction("Switch style") {
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/grid.png"));
                putValue(SHORT_DESCRIPTION, "Change to grid style");
            }

            @Override
            public void actionPerformed(final ActionEvent event) {
                // Toggle grid mode
                gridMode = !gridMode;
                renderArrayToActivationsImage();
            }
        };
        contextMenu.add(switchStyle);

        // Example of how to use radio buttons in case we end up with three designs
        JMenu switchStyleMenu = new JMenu("Switch Styles");
        ButtonGroup styles = new ButtonGroup();
        JRadioButtonMenuItem state1 = new JRadioButtonMenuItem("Style 1", true);
        styles.add(state1);
        state1.addActionListener(e -> System.out.println("State 1"));
        switchStyleMenu.add(state1);
        JRadioButtonMenuItem state2  = new JRadioButtonMenuItem("Style 2");
        styles.add(state2);
        state2.addActionListener(e -> System.out.println("State 2"));
        switchStyleMenu.add(state2);
        JRadioButtonMenuItem state3  = new JRadioButtonMenuItem("Style 3");
        state3.addActionListener(e -> System.out.println("State 3"));
        styles.add(state3);
        switchStyleMenu.add(state3);
        contextMenu.add(switchStyleMenu);
        contextMenu.addSeparator();

        // Randomize Action
        Action randomizeAction = new AbstractAction("Randomize") {
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Rand.png"));
                putValue(SHORT_DESCRIPTION, "Randomize neuro naarray");
            }

            @Override
            public void actionPerformed(final ActionEvent event) {
                neuronArray.randomize();
            }
        };
        contextMenu.add(randomizeAction);

        contextMenu.addSeparator();
        Action editComponents = new AbstractAction("Edit Components...") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                StandardDialog dialog = new StandardDialog();
                NumericTable arrayData = new NumericTable(neuronArray.getActivations());
                dialog.setContentPane(new SimbrainJTableScrollPanel(
                        SimbrainJTable.createTable(arrayData)));
                dialog.addClosingTask(() -> {
                    neuronArray.addInputs(arrayData.getVectorCurrentRow());
                    neuronArray.update();
                });
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
            }
        };
        contextMenu.add(editComponents);

        // Coupling menu
        contextMenu.addSeparator();
        JMenu couplingMenu = createCouplingMenu(networkPanel.getNetworkComponent(), neuronArray);
        if (couplingMenu != null) {
            contextMenu.add(couplingMenu);
        }

        return contextMenu;
    }

    /**
     * Returns the dialog for editing this neuron array.
     */
    private StandardDialog getArrayDialog() {
        StandardDialog dialog = new AnnotatedPropertyEditor(neuronArray).getDialog();
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.addClosingTask(NeuronArrayNode.this::updateInfoText);
        return dialog;
    }

    @Override
    public JDialog getPropertyDialog() {
        return getArrayDialog();
    }

    @Override
    public NeuronArray getModel() {
        return getNeuronArray();
    }

    @Override
    protected void paint(PPaintContext paintContext) {
        paintContext.getGraphics().setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        super.paint(paintContext);
    }

    @Override
    protected boolean validateFullBounds() {
        if (boundsCache == null) {

            PBounds bounds = new PBounds();
            bounds.add(infoText.getFullBoundsReference());
            bounds.add(activationImage.getFullBoundsReference());

            removeChild(borderBox);
            borderBox = PPath.createRectangle(bounds.x-3, bounds.y-3, bounds.width+6, bounds.height+6);
            addChild(borderBox);
            borderBox.lowerToBottom();
            borderBox.setStroke(boundaryStroke);

            // Todo: Magic variables
            var ctr = bounds.getCenter2D();
            setBounds(0, 0, bounds.width + 7, bounds.height + 30);
            centerBoundsOnPoint(ctr.getX(), ctr.getY());

            boundsCache = bounds;
        }
        return super.validateFullBounds();
    }
}
