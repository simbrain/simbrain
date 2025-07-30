package org.simbrain.world.imageworld.filters

import org.simbrain.util.*
import java.awt.image.BufferedImage

/**
 * Manages a collection of ImageFilters that can be applied in sequence to process images.
 * Unlike transformations (which are single-selection), multiple filters can be active simultaneously.
 */
class FilterManager {

    /**
     * List of all available filters that can be added
     */
    private val availableFilterTypes = listOf(
        GaborFilter::class.java,
        EdgeDetectionFilter::class.java
    )

    /**
     * List of currently active filters
     */
    private val activeFilters = mutableListOf<ImageFilter>()

    /**
     * Events for filter management
     */
    val events = FilterManagerEvents()

    /**
     * Get a list of all available filter types
     */
    fun getAvailableFilterTypes(): List<Class<out ImageFilter>> = availableFilterTypes

    /**
     * Get a list of currently active filters
     */
    fun getActiveFilters(): List<ImageFilter> = activeFilters.toList()

    /**
     * Add a new filter to the active list
     */
    fun addFilter(filter: ImageFilter) {
        activeFilters.add(filter)
        events.filterAdded.fire(filter)
    }

    /**
     * Remove a filter from the active list
     */
    fun removeFilter(filter: ImageFilter) {
        if (activeFilters.remove(filter)) {
            events.filterRemoved.fire(filter)
        }
    }

    /**
     * Create and add a new filter of the specified type
     */
    fun createAndAddFilter(filterType: Class<out ImageFilter>): ImageFilter {
        val filter = filterType.getDeclaredConstructor().newInstance()
        addFilter(filter)
        return filter
    }

    /**
     * Move a filter up in the processing order
     */
    fun moveFilterUp(filter: ImageFilter) {
        val index = activeFilters.indexOf(filter)
        if (index > 0) {
            activeFilters.removeAt(index)
            activeFilters.add(index - 1, filter)
            events.filterOrderChanged.fire()
        }
    }

    /**
     * Move a filter down in the processing order
     */
    fun moveFilterDown(filter: ImageFilter) {
        val index = activeFilters.indexOf(filter)
        if (index >= 0 && index < activeFilters.size - 1) {
            activeFilters.removeAt(index)
            activeFilters.add(index + 1, filter)
            events.filterOrderChanged.fire()
        }
    }

    /**
     * Apply all enabled filters in sequence to the input image
     */
    fun applyFilters(input: BufferedImage): BufferedImage {
        var result = input
        for (filter in activeFilters) {
            if (filter.enabled) {
                result = filter.apply(result)
            }
        }
        return result
    }

    /**
     * Clear all active filters
     */
    fun clearAllFilters() {
        val removedFilters = activeFilters.toList()
        activeFilters.clear()
        removedFilters.forEach { events.filterRemoved.fire(it) }
    }

    /**
     * Get number of enabled filters
     */
    fun getEnabledFilterCount(): Int = activeFilters.count { it.enabled }

    /**
     * Get number of total filters
     */
    fun getTotalFilterCount(): Int = activeFilters.size
}

/**
 * Events related to filter management
 */
class FilterManagerEvents : Events() {
    val filterAdded = OneArgEvent<ImageFilter>()
    val filterRemoved = OneArgEvent<ImageFilter>()
    val filterOrderChanged = NoArgEvent()
}