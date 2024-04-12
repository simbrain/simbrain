package org.simbrain.world.imageworld.filters;

import org.simbrain.world.imageworld.ImageSource;
import org.simbrain.world.imageworld.events.FilterCollectionEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Maintains a list of {@link Filter}s that can be applied to an {@link ImageSource}.
 */
public class FilterCollection {

    /**
     * List of filters that can be applied to an image.
     */
    private List<Filter> filters = new ArrayList<>();

    /**
     * Currently selected sensor matrix.
     */
    private Filter currentFilter;

    /**
     * Provides the image that is filtered.
     */
    private final ImageSource imageSource;

    /**
     * Handle FilterSelector Events.
     */
    private transient FilterCollectionEvents events = new FilterCollectionEvents();

    public FilterCollection(ImageSource imageSource) {
        this.imageSource = imageSource;
        initializeDefaultFilters();
        imageSource.getEvents().getResize().on(null, true, () -> {
            filters.forEach(Filter::initScaleOp);
        });
        imageSource.getEvents().getImageUpdate().on(null, true, () -> {
            filters.forEach(Filter::updateFilter);
        });
    }

    /**
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    public Object readResolve() {
        events = new FilterCollectionEvents();
        imageSource.getEvents().getResize().on(() -> {
            filters.forEach(Filter::initScaleOp);
        });
        imageSource.getEvents().getImageUpdate().on(() -> {
            filters.forEach(Filter::updateFilter);
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
        Filter unfiltered = new Filter(
                "Unfiltered",
                imageSource, new IdentityOp(), imageSource.getWidth(), imageSource.getHeight()
        );
        imageSource.getEvents().getResize().on(null, true, () -> {
            unfiltered.setHeight(imageSource.getCurrentImage().getHeight());
            unfiltered.setWidth(imageSource.getCurrentImage().getWidth());
            unfiltered.initChannels();
        });
        filters.add(unfiltered);

        Filter gray100x100 = new Filter(
                "Gray 150x150",
                imageSource, new GrayOp(), 150, 150);
        filters.add(gray100x100);

        Filter color100x100 = new Filter(
                "Color 100x100", imageSource, new IdentityOp(), 100, 100);
        filters.add(color100x100);

        Filter threshold10x10 = new Filter(
                "Threshold 10x10", imageSource, new ThresholdOp(.5f), 10, 10);
        filters.add(threshold10x10);

        Filter threshold250x250 = new Filter(
                "Threshold 250x250",
                imageSource, new ThresholdOp(.5f), 250, 250);
        filters.add(threshold250x250);

        currentFilter = filters.get(0);
    }

    /**
     * Add a new filterContainer to the list.
     *
     * @param filter the filterContainer to add
     */
    public void addFilter(Filter filter) {
        filters.add(filter);
        events.getFilterAdded().fireAndForget(filter);
    }

    /**
     * Remove the indicated sensor matrix.
     *
     * @param filter the sensor matrix to remove
     */
    public void removeFilter(Filter filter) {
        // Can't remove the "Unfiltered" option
        if (filter.getName().equalsIgnoreCase("Unfiltered")) {
            return;
        }
        filters.remove(filter);
        getEvents().getFilterRemoved().fireAndForget(filter);
    }

    public ImageSource getImageSource() {
        return imageSource;
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public Filter getCurrentFilter() {
        return currentFilter;
    }


    public void setCurrentFilter(Filter currentFilter) {
        events.getFilterChanged().fireAndForget(this.currentFilter, currentFilter);
        this.currentFilter = currentFilter;
    }

    public FilterCollectionEvents getEvents() {
        return events;
    }

}
