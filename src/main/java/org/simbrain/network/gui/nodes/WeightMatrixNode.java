package org.simbrain.network.gui.nodes;

import org.nd4j.linalg.factory.Nd4j;
import org.piccolo2d.PNode;
import org.piccolo2d.nodes.PImage;
import org.piccolo2d.nodes.PPath;
import org.simbrain.network.core.ArrayConnectable;
import org.simbrain.network.core.WeightMatrix;
import org.simbrain.util.math.SimbrainMath;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.*;

/**
 * A visual representation of a weight matrix
 */
public class WeightMatrixNode extends PNode {

    /**
     * The weight matrix this node represents
     */
    private WeightMatrix connection;

    /**
     * The line model connecting the source and the target
     */
    private Line2D line2D;

    /**
     * The visual component of the {@link #line2D}.
     */
    private PPath line;

    /**
     * An image showing individuals connection strength
     */
    private PImage weightsImage = new PImage();

    /**
     * Width of the {@link #weightsImage}
     */
    private int imageWidth = 100;

    /**
     * Height of the {@link #weightsImage}
     */
    private int imageHeight = 100;

    /**
     * A box around the {@link #weightsImage}
     */
    private PPath box;

    public WeightMatrixNode(WeightMatrix connection) {
        box = PPath.createRectangle(-1, -1, imageHeight + 2, imageWidth + 2);
        box.setStroke(new BasicStroke(2));
        addChild(box);

        this.connection = connection;
        ArrayConnectable source = connection.getSource();
        ArrayConnectable target = connection.getTarget();
        line2D = new Line2D.Double(source.getLocation(), target.getLocation());
        line = new PPath.Double(line2D);
        source.onLocationChange(() -> {
            updateImageBoxLocation();
            updateLine();
        });
        target.onLocationChange(() -> {
            updateImageBoxLocation();
            updateLine();
        });
        updateLine();

        renderMatrixToImage();
        addChild(weightsImage);
        updateImageBoxLocation();

    }

    /**
     * Redraw the {@link #line} based on the new source and target location.
     */
    private void updateLine() {
        ArrayConnectable source = connection.getSource();
        ArrayConnectable target = connection.getTarget();
        removeChild(line);
        line2D.setLine(source.getLocation(), target.getLocation());
        line = new PPath.Double(line2D);
        line.setStroke(new BasicStroke(3.0f));
        addChild(line);
        line.lowerToBottom();
    }

    /**
     * Update the {@link #weightsImage} and its {@link #box} to the updated location (center of the {@link #line}.
     */
    private void updateImageBoxLocation() {
        weightsImage.setOffset(
                line.getBounds().getCenterX() - 50,
                line.getBounds().getCenterY() - 50
        );
        box.setOffset(
                line.getBounds().getCenterX() - 50,
                line.getBounds().getCenterY() - 50
        );
    }

    /**
     * Render the weight matrix to the {@link #weightsImage}.
     */
    private void renderMatrixToImage() {

        if (!connection.isEnableRendering()) {
            // TODO: Remove existing
            return;
        }

        ColorModel colorModel = new DirectColorModel(24, 0xff << 16, 0xff << 8, 0xff);
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(connection.getWeightMatrix().columns(),
                connection.getWeightMatrix().rows());

        float[] activations = Nd4j.toFlattened(connection.getWeightMatrix()).toFloatVector();

        int[] raster = new int[(int) connection.getWeightMatrix().length()];

        // TODO: Use standardized Simbrain library for these color scalings
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
            this.weightsImage.setImage(img);
            this.weightsImage.setBounds(0, 0, imageWidth, imageHeight);
        });
    }
}
