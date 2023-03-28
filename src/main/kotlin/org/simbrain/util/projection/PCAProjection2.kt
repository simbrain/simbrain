package org.simbrain.util.projection

import smile.projection.PCA

class PCAProjection2: ProjectionMethod2() {

    val initialProjectionMethod = CoordinateProjection2()

    var pca: PCA? = null

    override fun project(dataset: Dataset2) {
        if (dataset.kdTree.size < 3) {
            initialProjectionMethod.project(dataset)
            return
        }
        val upstairs = dataset.computeUpstairsArray()
        pca = PCA.fit(upstairs).also {
            it.setProjection(2)
            dataset.setDownstairsData(it.project(upstairs))
        }
    }

    override fun initializeDownstairsPoint(dataset: Dataset2, point: DataPoint2) {
        pca.let {
            if (it == null) {
                initialProjectionMethod.initializeDownstairsPoint(dataset, point)
            } else {
                point.setDownstairs(it.project(point.upstairsPoint))
            }
        }
    }

    override fun copy() = PCAProjection2()

    override val name = "PCA"

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ProjectionMethod2.getTypes()
        }
    }
}