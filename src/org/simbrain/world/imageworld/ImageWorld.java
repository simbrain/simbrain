package org.simbrain.world.imageworld;

import org.simbrain.world.imageworld.filters.ImageFilterFactory;
import org.simbrain.world.imageworld.filters.ThresholdFilterFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Sublcasses of ImageWorld contain an {@link ImageSource} and a series of
 * {@link SensorMatrix} objects. The image source is what outputs an image of
 * some kind, either a static image, or a video or a pixel display.  The sensor
 * matrices convert the image into numbers and allow the current image in an
 * image source to be in couplings and thereby communicate with other workspace
 * components.
 * <br>
 * The world contains a single {@link ImagePanel} which all {@link ImageSource}'s
 * draw to.
 * <br>
 * Currently there are two subclasses corresponding to the "Image World" ({@link
 * ImageAlbumWorld}) and "Pixel Display" ({@link PixelDisplayWorld}) in the
 * GUI.  Broadly speaking ImageAlbumWorld produces pixels to be consumed by
 * other components and PixelDisplayWorld consumes pixels produced by other
 * components.
 *
 * @author Tim Shea
 * @author Jeff Yoshimi
 */
public abstract class ImageWorld {

    /**
     * List of sensor matrices associated with this world.
     */
    private List<SensorMatrix> sensorMatrices;

    /**
     * Currently selected sensor matrix.
     */
    private SensorMatrix currentSensorMatrix;

    /**
     * Current image and sensor matrices draw to this JPanel.
     */
    protected transient ImagePanel imagePanel;

    /**
     * Clipboard for the image world.
     */
    private transient ImageClipboard clipboard;

    /**
     * List of world listeners.
     */
    private transient List<Listener> listeners;

    /**
     * Construct the image world.
     */
    public ImageWorld() {
        clipboard = new ImageClipboard(this);
        sensorMatrices = new ArrayList<SensorMatrix>();
        listeners = new ArrayList<Listener>();
    }

    /**
     * Initialize some default filters on world creation. This should be called
     * on the instantiation of a child of this class after the image source is
     * created.
     */
    void initializeDefaultSensorMatrices() {

        // Load default sensor matrices
        SensorMatrix unfiltered = new SensorMatrix(
            "Unfiltered",
            getImageSource()
        );
        getSensorMatrices().add(unfiltered);

        SensorMatrix gray100x100 = new SensorMatrix(
            "Gray 150x150",
            ImageFilterFactory.createGrayFilter(getImageSource(), 150, 150)
        );
        getSensorMatrices().add(gray100x100);

        SensorMatrix color100x100 = new SensorMatrix(
            "Color 100x100",
            ImageFilterFactory.createColorFilter(getImageSource(), 100, 100)
        );
        getSensorMatrices().add(color100x100);

        SensorMatrix threshold10x10 = new SensorMatrix(
            "Threshold 10x10",
            ThresholdFilterFactory.createThresholdFilter(
                getImageSource(),
                0.5f,
                10,
                10
            )
        );
        getSensorMatrices().add(threshold10x10);

        SensorMatrix threshold250x250 = new SensorMatrix(
            "Threshold 250x250",
            ThresholdFilterFactory.createThresholdFilter(
                getImageSource(),
                0.5f,
                250,
                250
            )
        );

        getSensorMatrices().add(threshold250x250);

        setCurrentSensorMatrix(getSensorMatrices().get(0));
    }

    /**
     * Returns a deserialized ImageWorld.
     */
    public Object readResolve() {
        imagePanel = new ImagePanel(true);
        listeners = new ArrayList<Listener>();
        currentSensorMatrix.getSource().addListener(imagePanel);
        clipboard = new ImageClipboard(this);
        getImageSource().notifyResize();
        getImageSource().notifyImageUpdate();
        return this;
    }

    /**
     * Save the current image as the specified filename.
     *
     * @param filename The filename to save to.
     */
    public void saveImage(String filename) throws IOException {
        BufferedImage image = currentSensorMatrix.getSource().getCurrentImage();
        File file = new File(filename);
        String[] split = filename.split("\\.");
        String ext = split[split.length - 1];
        ImageIO.write(image, ext, file);
    }

    /**
     * Update the current tooltip on the jpanel.
     */
    private void updateToolTipText() {
        if (currentSensorMatrix == null) {
            //TODO
            //imagePanel.setToolTipText(compositeSource.getImageSource().getWidth() + " by " +  compositeSource.getImageSource().getHeight());
        } else {
            imagePanel.setToolTipText(currentSensorMatrix.getWidth() +
                " by " + currentSensorMatrix.getHeight());
        }
    }

    /**
     * Set an existing buffered image as the current image.
     */
    public void setImage(BufferedImage image) {
        getImageSource().setCurrentImage(image);
    }

    /**
     * Clear the current image from the composite image source.
     */
    public abstract void clearImage();


    //TODO: Move this and all emitter stuff..
    /**
     * Get whether the emitter matrix is using color.
     */
    public abstract boolean getUseColorEmitter();

    /**
     * Add a new matrix to the list.
     *
     * @param matrix the matrix to add
     */
    public void addSensorMatrix(SensorMatrix matrix) {
        sensorMatrices.add(matrix);
        setCurrentSensorMatrix(matrix);
        fireSensorMatrixAdded(matrix);
    }

    /**
     * Remove the indicated sensor matrix.
     *
     * @param sensorMatrix the sensor matrix to remove
     */
    public void removeSensorMatrix(SensorMatrix sensorMatrix) {
        // Can't remove the "Unfiltered" option
        if (sensorMatrix.getName().equalsIgnoreCase("Unfiltered")) {
            return;
        }
        int dialogResult = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete sensor panel \"" + sensorMatrix.getName() + "\" ?", "Warning", JOptionPane.YES_NO_OPTION);
        if (dialogResult == JOptionPane.YES_OPTION) {
            int index = sensorMatrices.indexOf(sensorMatrix);
            setCurrentSensorMatrix(sensorMatrices.get(index - 1));
            sensorMatrices.remove(sensorMatrix);
            // TODO: This is bad and should be handled in SensorMatrix
            // ImageSource source = sensorMatrix.getSource();
            // if (source instanceof FilteredImageSource) {
            //     compositeSource.removeListener((FilteredImageSource) source);
            // }
            sensorMatrix.getSource().removeListener(sensorMatrix);
            fireSensorMatrixRemoved(sensorMatrix);
        }
    }

    /**
     * Return the image panel.
     */
    public ImagePanel getImagePanel() {
        return imagePanel;
    }

    /**
     * Return the clipboard.
     */
    public ImageClipboard getClipboard() {
        return clipboard;
    }
    
    public SensorMatrix getCurrentSensorMatrix() {
        return currentSensorMatrix;
    }

    public void setCurrentSensorMatrix(SensorMatrix sensorMatrix) {
        if (sensorMatrix == currentSensorMatrix) {
            return;
        }
        if (currentSensorMatrix != null) {
            currentSensorMatrix.getSource().removeListener(imagePanel);
        }
        sensorMatrix.getSource().addListener(imagePanel);
        currentSensorMatrix = sensorMatrix;
        updateToolTipText();
    }

    public List<SensorMatrix> getSensorMatrices() {
        return sensorMatrices;
    }

    /**
     * Listener receives notifications when the image world is changed.
     */
    public interface Listener {

        /**
         * Called whenever a sensor matrix is added.
         */
        void sensorMatrixAdded(SensorMatrix addedMatrix);

        /**
         * Called whenever a sensor matrix is removed.
         */
        void sensorMatrixRemoved(SensorMatrix removedMatrix);
    }

    /**
     * Return the main {@link ImageSource} associated with this component.
     */
    public abstract ImageSourceAdapter getImageSource();

    /**
     * Update the image source.
     */
    public abstract void update();

    /**
     * @param listener the listener to add.
     */
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    /**
     * Notify listeners that a sensor matrix was added to the image world.
     */
    protected void fireSensorMatrixAdded(SensorMatrix matrix) {
        for (Listener listener : listeners) {
            listener.sensorMatrixAdded(matrix);
        }
    }

    /**
     * Notify listeners that a sensor matrix was removed from the image world.
     */
    protected void fireSensorMatrixRemoved(SensorMatrix matrix) {
        for (Listener listener : listeners) {
            listener.sensorMatrixRemoved(matrix);
        }
    }
}
