package org.simbrain.world.imageworld;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

import org.simbrain.resource.ResourceManager;
import org.simbrain.world.imageworld.filters.ImageFilter;
import org.simbrain.world.imageworld.filters.ImageFilterFactory;
import org.simbrain.world.imageworld.filters.ThresholdFilterFactory;

/**
 * ImageWorld contains the "logical" contents of this component, the image, and
 * a series of sensor matrices that can be used to convert the image into
 * numbers.
 */
public class ImageWorld {

    /**
     * WorldListener receives notifications when the list of sensor matrices is
     * changed.
     */
    public interface WorldListener {
        /** Called when list of sensor matrices changed. */
        void sensorMatricesUpdated();
    }

    private StaticImageSource staticSource;

    private EmitterMatrix emitterMatrix;

    /** Helper so that it's easy to switch between images sources. */
    private CompositeImageSource compositeSource;

    /** List of sensor matrices associated with this world. */
    private List<SensorMatrix> sensorMatrices = new ArrayList<SensorMatrix>();

    /** Currently selected sensor matrix. */
    private SensorMatrix currentSensorMatrix;

    /** GUI container for the current image or sensor view. */
    private ImagePanel imagePanel;

    /** List of world listener. */
    private transient List<WorldListener> listeners = new ArrayList<WorldListener>();

    /**
     * Construct the image world.
     */
    public ImageWorld() {
        // Setup ImageSources
        staticSource = new StaticImageSource();
        emitterMatrix = new EmitterMatrix();
        compositeSource = new CompositeImageSource(staticSource);
        staticSource.loadImage(ResourceManager.getImageIcon("bobcat.jpg"));
        imagePanel = new ImagePanel();

        // Load default sensor matrices
        SensorMatrix unfiltered = new SensorMatrix("Unfiltered", compositeSource);
        sensorMatrices.add(unfiltered);

        SensorMatrix gray75x75 = new SensorMatrix("Color 25x25",
                ImageFilterFactory.createColorFilter(compositeSource, 25, 25));
        sensorMatrices.add(gray75x75);

        SensorMatrix gray200x200 = new SensorMatrix("Gray 200x200",
                ImageFilterFactory.createGrayFilter(compositeSource, 200, 200));
        sensorMatrices.add(gray200x200);

        SensorMatrix threshold10x10 = new SensorMatrix("Threshold 10x10",
                ThresholdFilterFactory.createThresholdFilter(compositeSource, 0.5f, 10, 10));
        sensorMatrices.add(threshold10x10);

        SensorMatrix threshold100x100 = new SensorMatrix("Threshold 100x100",
                ThresholdFilterFactory.createThresholdFilter(compositeSource, 0.5f, 100, 100));
        sensorMatrices.add(threshold100x100);

        setCurrentSensorMatrix(sensorMatrices.get(0));
    }

    /**
     * Load image from specified filename.
     * @param filename path to image
     * @throws IOException thrown if the requested file is not available
     */
    public void loadImage(String filename) throws IOException {
        staticSource.loadImage(filename);
    }

    /** Switch the CompositeImageSource to the static image. */
    public void selectStaticSource() {
        compositeSource.selectSource(staticSource);
    }

    /** Set the size of the emitter matrix. */
    public void resizeEmitterMatrix(int width, int height) {
        emitterMatrix.setSize(width, height);
    }

    /** Switch the CompositeImageSource to the emitter matrix. */
    public void selectEmitterMatrix() {
        compositeSource.selectSource(emitterMatrix);
    }

    /**
     * Add a new matrix to the list.
     *
     * @param matrix the matrix to add
     */
    public void addSensorMatrix(SensorMatrix matrix) {
        sensorMatrices.add(matrix);
        setCurrentSensorMatrix(matrix);
        fireSensorMatricesUpdated();
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
        int dialogResult = JOptionPane.showConfirmDialog(
                null, "Are you sure you want to delete sensor panel \""
                        + sensorMatrix.getName() + "\" ?",
                "Warning", JOptionPane.YES_NO_OPTION);
        if (dialogResult == JOptionPane.YES_OPTION) {
            int index = sensorMatrices.indexOf(sensorMatrix);
            setCurrentSensorMatrix(sensorMatrices.get(index - 1));
            sensorMatrices.remove(sensorMatrix);
            // TODO: This is bad and should be handled in SensorMatrix
            ImageSource source = sensorMatrix.getSource();
            if (source instanceof ImageFilter) {
                compositeSource.removeListener((ImageFilter) source);
            }
            sensorMatrix.getSource().removeListener(sensorMatrix);
            fireSensorMatricesUpdated();
        }
    }

    /** @return the image panel */
    public ImagePanel getImagePanel() {
        return imagePanel;
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

    /** @return the currentSensorPanel */
    public SensorMatrix getCurrentSensorMatrix() {
        return currentSensorMatrix;
    }

    /** @param sensorMatrix the currentSensorMatrix to set */
    public void setCurrentSensorMatrix(SensorMatrix sensorMatrix) {
        if (sensorMatrix == currentSensorMatrix) {
            return;
        }
        if (currentSensorMatrix != null) {
            currentSensorMatrix.getSource().removeListener(imagePanel);
        }
        sensorMatrix.getSource().addListener(imagePanel);
        currentSensorMatrix = sensorMatrix;
    }

    /** @return a list of sensor matrices */
    public List<SensorMatrix> getSensorMatrices() {
        return sensorMatrices;
    }

    /** @param listener the listener to add. */
    public void addListener(WorldListener listener) {
        listeners.add(listener);
    }

    /** Fire sensor matrices update event. */
    public void fireSensorMatricesUpdated() {
        for (WorldListener listener : listeners) {
            listener.sensorMatricesUpdated();
        }
    }
}
