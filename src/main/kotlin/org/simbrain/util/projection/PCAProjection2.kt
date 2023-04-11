package org.simbrain.util.projection

import org.simbrain.util.UserParameter
import smile.projection.PCA

class PCAProjection2: ProjectionMethod2() {

    @UserParameter(label = "Freeze space", description = "If true, project to existing components each update. If " +
            "false, refit PCA components each update")
    var freeze: Boolean = false

    val initialProjectionMethod = CoordinateProjection2()

    var pca: PCA? = null

    /**
     * This re-fits PCA.
     */
    override fun init(dataset: Dataset2) {
        // The first few datapoinst can be projected using coordinate projection
        if (dataset.kdTree.size < 3) {
            initialProjectionMethod.init(dataset)
            return
        }
        reFitPCA(dataset)
    }

    override fun addPoint(dataset: Dataset2, point: DataPoint2) {
        if (dataset.kdTree.size < 3) {
            initialProjectionMethod.addPoint(dataset, point)
            return
        }
        if (!freeze && pca == null) {
            reFitPCA(dataset)
        }
        // Project the new point onto PCA components
        point.setDownstairs(pca!!.project(point.upstairsPoint))
    }

    private fun reFitPCA(dataset: Dataset2) {
        val upstairs = dataset.computeUpstairsArray()
        pca = PCA.fit(upstairs).also {
            it.setProjection(2)
            dataset.setDownstairsData(it.project(upstairs))
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