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

import org.nd4j.linalg.dataset.api.preprocessor.MinMaxStrategy;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.piccolo2d.PNode;
import org.piccolo2d.nodes.PImage;
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
import org.simbrain.util.math.SimbrainMath;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
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
     * Image to show activations.
     */
    private PImage activations = new PImage();

    /**
     * A background to show when no {@link #activations} image is present. (So that the node will not be transparent)
     */
    private PPath background = PPath.createRectangle(0 ,0,50,25);

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
        square.setTransparency(0.2f);
        this.neuronArray = na;

        neuronArray.addPropertyChangeListener(evt -> {
            if ("updated".equals(evt.getPropertyName())) {
                renderArrayToActivationsImage();
                updateInfoText();
            }
        });

        this.centerFullBoundsOnPoint(na.getX(), na.getY());
        init();
    }

    /**
     * Render an image and set it to {@link #activations} to show the current activations.
     *
     * Will not render when {@link NeuronArray#isRenderActivations()} is set to false.
     */
    private void renderArrayToActivationsImage() {

        if (!neuronArray.isRenderActivations()) {
            return;
        }

        ColorModel colorModel = new DirectColorModel(24, 0xff << 16, 0xff << 8, 0xff);
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(neuronArray.getCols(), neuronArray.getRows());

        float[] activations = Nd4j.toFlattened(neuronArray.getNeuronArray()).toFloatVector();

        int[] raster = new int[neuronArray.getCols() * neuronArray.getRows()];

        for (int i = 0; i < activations.length; i++) {
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
            this.activations.setImage(img);
            this.activations.setBounds(getBounds());
        });
    }

    /**
     * Initialize the NeuronNode.
     */
    private void init() {

        square.setPickable(true);

        addChild(background);
        addChild(activations);
        addChild(square);

        renderArrayToActivationsImage();

        PBounds bounds = square.getBounds();
        setBounds(bounds);

        infoText = new PText("rows: " + neuronArray.getRows() + "\n cols: "
            + neuronArray.getCols());
        infoText.setFont(INFO_FONT);
        addChild(infoText);
        infoText.offset(4,4);

        //addPropertyChangeListener(PROPERTY_FULL_BOUNDS, this);

    }

    public NeuronArray getNeuronArray() {
        return neuronArray;
    }

    public void updateInfoText() {
        infoText.setText("rows: " + neuronArray.getRows() + "\n cols: "
                + neuronArray.getCols());
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
        return true;
    }

    @Override
    protected JDialog getPropertyDialog() {
        return getNetworkPanel().getNeuronArrayDialog();
    }

    @Override
    public void resetColors() {

    }

}
