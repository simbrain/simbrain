package org.simbrain.world.imageworld.filters;

import org.simbrain.world.imageworld.ImageSource;
import org.simbrain.world.imageworld.events.FilterSelectorEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Maintains a list of {@link Filter}s that can be applied to an {@link ImageSource}.
 */
public class FilterSelector {

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
    private transient FilterSelectorEvents events = new FilterSelectorEvents(this);

    public FilterSelector(ImageSource imageSource) {
        this.imageSource = imageSource;
        initializeDefaultFilters();
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
                imageSource
        );
        filters.add(unfiltered);

        Filter gray100x100 = new Filter(
                "Gray 150x150",
                ImageFilterFactory.createGrayFilter(imageSource, 150, 150)
        );
        filters.add(gray100x100);

        Filter color100x100 = new Filter(
                "Color 100x100",
                ImageFilterFactory.createColorFilter(imageSource, 100, 100)
        );
        filters.add(color100x100);

        Filter threshold10x10 = new Filter(
                "Threshold 10x10",
                ThresholdFilterFactory.createThresholdFilter(
                        imageSource,
                        0.5f,
                        10,
                        10
                )
        );
        filters.add(threshold10x10);

        Filter threshold250x250 = new Filter(
                "Threshold 250x250",
                ThresholdFilterFactory.createThresholdFilter(
                        imageSource,
                        0.5f,
                        250,
                        250
                )
        );

        filters.add(threshold250x250);

        currentFilter = filters.get(0);
    }


    /**
     * Add a new filterContainer to the list.
     *
     * @param filter the filterContainer to add
     */
    public void addFilterContainer(Filter filter) {
        filters.add(filter);
        currentFilter = filter;
        events.fireFilterAdded(filter);
    }

    /**
     * Remove the indicated sensor matrix.
     *
     * @param filter the sensor matrix to remove
     */
    public void removeFilterContainer(Filter filter) {
        // Can't remove the "Unfiltered" option
        if (filter.getName().equalsIgnoreCase("Unfiltered")) {
            return;
        }
        filters.remove(filter);
        events.onFilterAdded(filter);
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
        this.currentFilter = currentFilter;
        currentFilter.getSource().getEvents().fireImageUpdate();
    }

    public FilterSelectorEvents getEvents() {
        return events;
    }

}
