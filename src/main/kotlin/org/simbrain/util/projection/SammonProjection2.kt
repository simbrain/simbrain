package org.simbrain.util.projection

class SammonProjection2 @JvmOverloads constructor (dimension: Int = 3): ProjectionMethod2(dimension) {

    val downstairsInitializationMethod = CoordinateProjection2(dimension)

    override fun project(dataset: Dataset2) {
    }

    override fun initializeDownstairsPoint(dataset: Dataset2, point: DataPoint2) {
        downstairsInitializationMethod.initializeDownstairsPoint(dataset, point)
    }

    fun iterate(dataset: Dataset2) {
        if (dataset.kdTree.size < 2) return
    }

    override val name = "Sammon"

    override fun copy() = SammonProjection2(dimension)

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ProjectionMethod2.getTypes()
        }
    }
}