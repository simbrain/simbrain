package org.simbrain.world.imageworld;

import org.simbrain.resource.ResourceManager;
import org.simbrain.world.imageworld.filters.FilteredImageSource;
import org.simbrain.world.imageworld.filters.ImageFilterFactory;
import org.simbrain.world.imageworld.filters.OffsetFilterFactory;
import org.simbrain.world.imageworld.filters.ThresholdFilterFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ImageWorld contains an {@link ImageSource} and a series of {@link SensorMatrix}
 * objects. The image source is what outputs an image of some kind, either a
 * static image or a video or a pixel display, something like that.  The sensor
 * matrices allow the current image in an image source to be in couplings and
 * thereby communicate with other workspace components.
 * that can be used to convert the image into numbers.
 *
 * @author Tim Shea
 * @author Jeff Yoshimi
 */
public class ImageWorld {

    private StaticImageSource staticSource;

    private EmitterMatrix emitterMatrix;

    /**
     * List of sensor matrices associated with this world.
     */
    private List<SensorMatrix> sensorMatrices;

    /**
     * Currently selected sensor matrix.
     */
    private SensorMatrix currentSensorMatrix;

    // Todo: Ask Tim if this is the only use of CompositeImageSource.
    /**
     * Helper so that it's easy to switch between image sources.
     */
    private CompositeImageSource compositeSource;

    public enum SourceType {
        STATIC_SOURCE,
        EMITTER_SOURCE
    }

    private final SourceType sourceType;

    /**
     * Container for the current image or sensor view.
     */
    private transient ImagePanel imagePanel;

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
    public ImageWorld(SourceType sourceType) {
        this.sourceType = sourceType;
        // Setup ImageSources
        staticSource = new StaticImageSource();
        emitterMatrix = new EmitterMatrix();
        if (sourceType == SourceType.EMITTER_SOURCE) {
            compositeSource = new CompositeImageSource(emitterMatrix);
        } else {
            compositeSource = new CompositeImageSource(staticSource);
        }
        staticSource.loadImage(ResourceManager.getImageIcon("bobcat.jpg"));
        imagePanel = new ImagePanel();
        clipboard = new ImageClipboard(this);
        sensorMatrices = new ArrayList<SensorMatrix>();
        listeners = new ArrayList<Listener>();


        imagePanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                // For testing ImageAlbum
                //staticSource.step();
                //staticSource.notifyImageUpdate();

                // For testing image editing
                //emitterMatrix.setBrightness(e.getLocationOnScreen(), 4);
                // emitterMatrix.emitImage();

            }
        });

        // Load default sensor matrices
        SensorMatrix unfiltered = new SensorMatrix("Unfiltered", compositeSource);
        sensorMatrices.add(unfiltered);

        SensorMatrix gray100x100 = new SensorMatrix("Gray 150x150", ImageFilterFactory.createGrayFilter(compositeSource, 150, 150));
        sensorMatrices.add(gray100x100);

        SensorMatrix color100x100 = new SensorMatrix("Color 100x100", ImageFilterFactory.createColorFilter(compositeSource, 100, 100));
        sensorMatrices.add(color100x100);

        SensorMatrix threshold10x10 = new SensorMatrix("Threshold 10x10", ThresholdFilterFactory.createThresholdFilter(compositeSource, 0.5f, 10, 10));
        sensorMatrices.add(threshold10x10);

        SensorMatrix threshold250x250 = new SensorMatrix("Threshold 250x250", ThresholdFilterFactory.createThresholdFilter(compositeSource, 0.5f, 250, 250));
        sensorMatrices.add(threshold250x250);

        SensorMatrix offset100x100 = new SensorMatrix("Offset-25 100x100", OffsetFilterFactory.createOffsetFilter(compositeSource, 25, 25, 100, 100));
        sensorMatrices.add(offset100x100);

        setCurrentSensorMatrix(sensorMatrices.get(0));

    }

    /**
     * Returns a deserialized ImageWorld.
     */
    public Object readResolve() {
        imagePanel = new ImagePanel();
        listeners = new ArrayList<Listener>();
        currentSensorMatrix.getSource().addListener(imagePanel);
        clipboard = new ImageClipboard(this);
        compositeSource.notifyImageUpdate();
        return this;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    /**
     * Load image from specified filename.
     *
     * @param filename path to image
     * @throws IOException thrown if the requested file is not available
     */
    public void loadImage(String filename) throws IOException {
        staticSource.loadImage(filename);
        fireImageSourceChanged(staticSource);
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
        if(currentSensorMatrix == null) {
            imagePanel.setToolTipText(compositeSource.getImageSource().getWidth() + " by " +  compositeSource.getImageSource().getHeight());
        } else {
            imagePanel.setToolTipText(currentSensorMatrix.getWidth() + " by " +  currentSensorMatrix.getHeight());
        }
    }
    /**
     * Set an existing buffered image as the current image.
     */
    public void setImage(BufferedImage image) {
        staticSource.setCurrentImage(image);
    }

    /**
     * Clear the current image from the composite image source.
     */
    public void clearImage() {
        if (isEmitterMatrixSelected()) {
            emitterMatrix.clear();
            emitterMatrix.emitImage();
        } else {
            staticSource.setCurrentImage(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB));
        }
    }

    /**
     * Switch the CompositeImageSource to the static image.
     */
    public void selectStaticSource() {
        compositeSource.setImageSource(staticSource);
    }

    /**
     * Get whether the emitter matrix is using color.
     */
    public boolean getUseColorEmitter() {
        return emitterMatrix.isUsingRGBColor();
    }

    /**
     * Set the color mode of the emitter matrix.
     */
    public void setUseColorEmitter(boolean value) {
        emitterMatrix.setUsingRGBColor(value);
        fireImageSourceChanged(emitterMatrix);
    }

    /**
     * Get the width of the emitter matrix.
     */
    public int getEmitterWidth() {
        return emitterMatrix.getWidth();
    }

    /**
     * Get the height of the emitter matrix.
     */
    public int getEmitterHeight() {
        return emitterMatrix.getHeight();
    }

    /**
     * Set the size of the emitter matrix.
     */
    public void resizeEmitterMatrix(int width, int height) {
        emitterMatrix.setSize(width, height);
        fireImageSourceChanged(emitterMatrix);
    }

    /**
     * Returns whether the emitter matrix is the current source for the image world.
     */
    public boolean isEmitterMatrixSelected() {
        return compositeSource.getImageSource() == emitterMatrix;
    }

    /**
     * Switch the CompositeImageSource to the emitter matrix.
     */
    public void selectEmitterMatrix() {
        compositeSource.setImageSource(emitterMatrix);
    }

    /**
     * Update the emitter matrix image.
     */
    public void emitImage() {
        emitterMatrix.emitImage();
    }

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
            ImageSource source = sensorMatrix.getSource();
            if (source instanceof FilteredImageSource) {
                compositeSource.removeListener((FilteredImageSource) source);
            }
            sensorMatrix.getSource().removeListener(sensorMatrix);
            fireSensorMatrixRemoved(sensorMatrix);
        }
    }

    /**
     * Return the image panel
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

    /**
     * @return Returns a CompositeImageSource which allows sensors to seamlessly switch between
     * available ImageSources
     */
    public ImageSource getCompositeImageSource() {
        return compositeSource;
    }

    public List<ImageSource> getImageSources() {
        List<ImageSource> sources = new ArrayList<ImageSource>();
        sources.addAll(Arrays.asList(staticSource, emitterMatrix));
        for (SensorMatrix sensorMatrix : sensorMatrices) {
            // Add Composite (unfiltered) and ImageFilters
            sources.add(sensorMatrix.getSource());
        }
        return sources;
    }

    public ImageSource getCurrentImageSource() {
        return compositeSource.getImageSource();
    }

    /**
     * @return the currentSensorPanel
     */
    public SensorMatrix getCurrentSensorMatrix() {
        return currentSensorMatrix;
    }

    /**
     * @param sensorMatrix the currentSensorMatrix to set
     */
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

    /**
     * @return a list of sensor matrices
     */
    public List<SensorMatrix> getSensorMatrices() {
        return sensorMatrices;
    }

    /**
     * Listener receives notifications when the image world is changed.
     */
    public interface Listener {

        /**
         * Called whenever an image source is changed.
         */
        void imageSourceChanged(ImageSource changedSource);

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
     * @param listener the listener to add.
     */
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    /**
     * Notify listeners that an image source was changed.
     */
    protected void fireImageSourceChanged(ImageSource source) {
        for (Listener listener : listeners) {
            listener.imageSourceChanged(source);
        }
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
