package org.simbrain.util.projection

import org.simbrain.util.propertyeditor.CopyableObject

/**
 * A method for projecting a set of high-dimensional or "upstairs" points in a [Dataset2] object
 * with a downstairs point. That is, a dimensionality reduction technique.
 */
abstract class ProjectionMethod2() : CopyableObject {

    /**
     * This method should be called when entire dataset should be projected. Every upstairs point in the dataset should
     * be associated with a downstairs point. This can be called when initializing a projection. Some projection
     * methods must also be periodically re-initialized.
     *
     * Methods that implement [IterableProjectionMethod2] can't be projected in one go. Instead they must be iterated,
     * which is done using a play button in the toolbar.
     */
    abstract fun project(dataset: Dataset2)

    /**
     * This can be called when adding a new point to a dataset, or when an existing point should be reinitialized.
     * One projection method can use another projection method when implementing this method.
     *
     * This is important for "online" updates, where a projection method is costly to re-run on a whole dataset. For
     * example, Sammon maps require iterating multiple times. Rather than re-iterating, triangulation can be used to
     * initialize new points.
     *
     *  Change name to updatePoint or add point?
     */
    abstract fun addPoint(dataset: Dataset2, point: DataPoint2)

    abstract override fun copy(): ProjectionMethod2

    /**
     * Note that this is used by the combo box, so be careful about changing it.
     */
    override fun toString(): String {
        return name
    }

    companion object {

        /**
         * Decay functions for drop-down list used by [org.simbrain.util.propertyeditor.ObjectTypeEditor] to set a
         * type of probability distribution.
         */
        @JvmStatic
        fun getTypes() = listOf(
            CoordinateProjection2::class.java,
            PCAProjection2::class.java,
            SammonProjection2::class.java,
            TriangulateProjection2::class.java
        )
    }

}