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

import org.nd4j.linalg.factory.Nd4j;
import org.piccolo2d.nodes.PImage;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.nodes.PText;
import org.piccolo2d.util.PBounds;
import org.piccolo2d.util.PPaintContext;
import org.simbrain.network.dl4j.NeuronArray;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.actions.edit.CopyAction;
import org.simbrain.network.gui.actions.edit.CutAction;
import org.simbrain.network.gui.actions.edit.DeleteAction;
import org.simbrain.network.gui.actions.edit.PasteAction;
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
import java.awt.image.*;

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
     * Width in pixels of the main display box for ND4J arrays.
     */
    private final float boxWidth = 150;

    /**
     * Height in pixels of the main display box for ND4J arrays.
     */
    private final float boxHeight = 50;

    /**
     * Height in pixels of the pixel display showing the activations.
     */
    private double activationImageHeight = 10;

    /**
     * Text showing info about the array.
     */
    private PText infoText;

    /**
     * Square shape around array node.
     */
    private PPath borderBox = PPath.createRectangle(0, 0, boxWidth, boxHeight);

    /**
     * Image to show activationImage.
     */
    private PImage activationImage = new PImage();

    /**
     * A backgroundImage to show when no {@link #activationImage} image is present.
     * (So that the node will not be transparent)
     */
    private PPath backgroundImage = PPath.createRectangle(0, 0, boxWidth, boxHeight);

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
     * @param np    Reference to NetworkPanel
     * @param na reference to model neuron array
     */
    public NeuronArrayNode(final NetworkPanel np, final NeuronArray na) {
        super(np);
        borderBox.setTransparency(0.2f);
        this.neuronArray = na;
        networkPanel = np;

        neuronArray.addPropertyChangeListener(evt -> {
            if ("updated".equals(evt.getPropertyName())) {
                renderArrayToActivationsImage();
                updateInfoText();
            } else if("labelChanged".equals(evt.getPropertyName())) {
                //interactionBox.setText((String) evt.getNewValue());
                //interactionBox.updateText();
            } else if("delete".equals(evt.getPropertyName())) {
                NeuronArrayNode.this.removeFromParent();
            } else if ("moved".equals(evt.getPropertyName())) {
                pullViewPositionFromModel();
            }
        });

        // Set up main items
        borderBox.setPickable(true);
        addChild(backgroundImage);
        addChild(activationImage);
        addChild(borderBox);

        // Border box determines bounds
        PBounds bounds = borderBox.getBounds();
        setBounds(bounds);

        // Info text
        infoText = new PText();
        infoText.setFont(INFO_FONT);
        addChild(infoText);
        infoText.offset(8, 8);
        updateInfoText();

        this.centerFullBoundsOnPoint(na.getCenterX(), na.getCenterY());

        // Image array
        renderArrayToActivationsImage();
        pushViewPositionToModel();
    }

    public void pullViewPositionFromModel() {
        Point2D p = new Point2D.Double(neuronArray.getCenterX() - boxWidth / 2, neuronArray.getCenterY() - boxHeight / 2);
        this.setGlobalTranslation(p);
    }

    /**
     * Update the position of the model neuron based on the global coordinates
     * of this pnode.
     */
    public void pushViewPositionToModel() {
        Point2D p = this.getGlobalTranslation();
        neuronArray.setCenterX(p.getX() + boxWidth / 2);
        neuronArray.setCenterY(p.getY() + boxHeight / 2);
    }

    @Override
    public void offset(double dx, double dy) {
        pushViewPositionToModel();
        super.offset(dx, dy);
    }

    /**
     * Render an image and set it to {@link #activationImage} to show the current activationImage.
     *
     * Will not render when {@link NeuronArray#isRenderActivations()} is set to false.
     */
    private void renderArrayToActivationsImage() {

        if (!neuronArray.isRenderActivations()) {
            // TODO: Remove existing
            return;
        }

        ColorModel colorModel = new DirectColorModel(24, 0xff << 16, 0xff << 8, 0xff);
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(neuronArray.getNumNodes(), 1);

        float[] activations = Nd4j.toFlattened(neuronArray.getNeuronArray()).toFloatVector();

        int[] raster = new int[neuronArray.getNumNodes()];

        for (int i = 0; i < activations.length; i++) {
            // Assume activations between -1 and 1.  If larger values are allowed used SimbrainMath.rescale
            float saturation = activations[i];
            saturation = SimbrainMath.clip(saturation, -1.0f, 1.0f);
            if (saturation < 0) {
                raster[i] = Color.HSBtoRGB(2/3f, -saturation, 1.0f);
            } else {
                raster[i] = Color.HSBtoRGB(0.0f, saturation, 1.0f);
            }
        }

        // ref: https://stackoverflow.com/questions/33460365/what-the-fastest-way-to-draw-pixels-buffer-in-java
        BufferedImage img = new BufferedImage(
                colorModel,
                Raster.createWritableRaster(
                        sampleModel,
                        new DataBufferInt(raster, raster.length),
                        null
                ),
                false,
                null
        );

        SwingUtilities.invokeLater(() -> {
            this.activationImage.setImage(img);
            this.activationImage.setBounds(5,.75*boxHeight - activationImageHeight /2,
                    boxWidth-10, activationImageHeight);
        });
    }

    public NeuronArray getNeuronArray() {
        return neuronArray;
    }

    /**
     * Update status text.
     */
    private void updateInfoText() {
        infoText.setText(
                ""+ neuronArray.getLabel() + "    " +
                "nodes: " + neuronArray.getNeuronArray().length()
                + "\nmean activation: "
                + SimbrainMath.roundDouble((java.lang.Double) neuronArray.getNeuronArray().meanNumber(),4));
    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public boolean showNodeHandle() {
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
        return neuronArray.toString();
    }

    @Override
    protected boolean hasContextMenu() {
        return true;
    }

    @Override
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();

        contextMenu.add(new CutAction(getNetworkPanel()));
        contextMenu.add(new CopyAction(getNetworkPanel()));
        contextMenu.add(new PasteAction(getNetworkPanel()));
        contextMenu.addSeparator();

        // Edit Submenu
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
        Action editComponents= new AbstractAction("Edit Components...") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                StandardDialog dialog = new StandardDialog();
                NumericTable arrayData = new NumericTable(neuronArray.getNeuronArray().toDoubleVector());
                dialog.setContentPane(new SimbrainJTableScrollPanel(
                        SimbrainJTable.createTable(arrayData)));
                dialog.addClosingTask(() -> {
                    neuronArray.getNeuronArray().data().setData(arrayData.getVectorCurrentRow());
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
        JMenu couplingMenu = networkPanel.getCouplingMenu(neuronArray);
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
    protected boolean hasPropertyDialog() {
        return true;
    }

    @Override
    protected JDialog getPropertyDialog() {
        return getArrayDialog();
    }



    @Override
    public void resetColors() {
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
}
