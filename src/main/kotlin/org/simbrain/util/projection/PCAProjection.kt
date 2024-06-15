package org.simbrain.util.projection

import org.simbrain.util.UserParameter
import smile.feature.extraction.PCA

class PCAProjection: ProjectionMethod() {

    @UserParameter(label = "Freeze space", description = "If true, project to existing components each update. If " +
            "false, refit PCA components each update")
    var freeze: Boolean = false

    val initialProjectionMethod = CoordinateProjection()

    @Transient
    var pca: PCA? = null

    /**
     * This re-fits PCA.
     */
    override fun init(dataset: Dataset) {
        // The first few datapoinst can be projected using coordinate projection
        if (dataset.kdTree.size < 3) {
            initialProjectionMethod.init(dataset)
            return
        }
        reFitPCA(dataset)
    }

    override fun addPoint(dataset: Dataset, point: DataPoint) {
        if (dataset.kdTree.size < 3) {
            initialProjectionMethod.addPoint(dataset, point)
            return
        }
        if (!freeze) {
            reFitPCA(dataset)
        }
        // Project the new point onto PCA components
        point.setDownstairs(pca!!.apply(point.upstairsPoint))
    }

    private fun reFitPCA(dataset: Dataset) {
        val upstairs = dataset.computeUpstairsArray()
        pca = PCA.fit(upstairs).getProjection(2).also {
            dataset.setDownstairsData(it.apply(upstairs))
        }
    }

    override fun copy() = PCAProjection()

    override val name = "PCA"

}