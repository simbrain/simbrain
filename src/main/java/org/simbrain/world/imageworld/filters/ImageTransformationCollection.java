package org.simbrain.world.imageworld.filters;

import org.simbrain.world.imageworld.ImageSource;
import org.simbrain.world.imageworld.events.FilterCollectionEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Maintains a list of {@link ImageTransformation}s that can be applied to an {@link ImageSource}.
 */
public class ImageTransformationCollection {

    /**
     * List of filters that can be applied to an image.
     */
    private List<ImageTransformation> imageTransformations = new ArrayList<>();

    /**
     * Currently selected sensor matrix.
     */
    private ImageTransformation currentImageTransformation;

    /**
     * Provides the image that is filtered.
     */
    private final ImageSource imageSource;

    /**
     * Handle FilterSelector Events.
     */
    private transient FilterCollectionEvents events = new FilterCollectionEvents();

    public ImageTransformationCollection(ImageSource imageSource) {
        this.imageSource = imageSource;
        initializeDefaultFilters();
        imageSource.getEvents().getImageUpdate().on(null, true, () -> {
            imageTransformations.forEach(ImageTransformation::applyFilter);
        });
    }

    /**
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    public Object readResolve() {
        events = new FilterCollectionEvents();
        imageSource.getEvents().getImageUpdate().on(() -> {
            imageTransformations.forEach(ImageTransformation::applyFilter);
        });
        return this;
    }

    /**
     * Initialize some default filters on world creation. This should be called
     * on the instantiation of a child of this class after the image source is
     * created.
     */
    void initializeDefaultFilters() {

        // Load default sensor matrices
        ImageTransformation unfiltered = new ImageTransformation(
                "Untransformed",
                imageSource, new IdentityOp(), imageSource.getWidth(), imageSource.getHeight()
        );
        imageSource.getEvents().getResize().on(null, true, () -> {
            unfiltered.setHeight(imageSource.getCurrentImage().getHeight());
            unfiltered.setWidth(imageSource.getCurrentImage().getWidth());
        });
        imageTransformations.add(unfiltered);

        ImageTransformation gray100x100 = new ImageTransformation(
                "Gray 100x100",
                imageSource, new GrayOp(), 100, 100);
        imageTransformations.add(gray100x100);

        ImageTransformation color100x100 = new ImageTransformation(
                "Color 100x100", imageSource, new IdentityOp(), 100, 100);
        imageTransformations.add(color100x100);

        ImageTransformation threshold10x10 = new ImageTransformation(
                "Threshold 10x10", imageSource, new ThresholdOp(.5f), 10, 10);
        imageTransformations.add(threshold10x10);

        ImageTransformation threshold250x250 = new ImageTransformation(
                "Threshold 250x250",
                imageSource, new ThresholdOp(.5f), 250, 250);
        imageTransformations.add(threshold250x250);

        currentImageTransformation = imageTransformations.get(0);
    }

    /**
     * Add a new filterContainer to the list.
     *
     * @param imageTransformation the filterContainer to add
     */
    public void addFilter(ImageTransformation imageTransformation) {
        imageTransformations.add(imageTransformation);
        events.getImageTransformationAdded().fire(imageTransformation);
    }

    /**
     * Remove the indicated sensor matrix.
     *
     * @param imageTransformation the sensor matrix to remove
     */
    public void removeFilter(ImageTransformation imageTransformation) {
        // Can't remove the "Unfiltered" option
        if (imageTransformation.getName().equalsIgnoreCase("Unfiltered")) {
            return;
        }
        imageTransformations.remove(imageTransformation);
        getEvents().getImageTransformationRemoved().fire(imageTransformation);
    }

    public ImageSource getImageSource() {
        return imageSource;
    }

    public List<ImageTransformation> getFilters() {
        return imageTransformations;
    }

    public ImageTransformation getCurrentFilter() {
        return currentImageTransformation;
    }


    public void setCurrentFilter(ImageTransformation currentImageTransformation) {
        this.currentImageTransformation = currentImageTransformation;
    }

    public FilterCollectionEvents getEvents() {
        return events;
    }

}
