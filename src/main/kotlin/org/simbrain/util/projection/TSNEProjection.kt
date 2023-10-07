package org.simbrain.util.projection

import org.simbrain.util.UserParameter
import smile.manifold.TSNE

class TSNEProjection: ProjectionMethod(), IterableProjectionMethod  {

    // TODO: Re-init when setting these parameters

    @UserParameter(label = "Perplexity")
    var perplexity: Double = 20.0

    @UserParameter(label = "Learning Rate")
    var eta: Double = 200.0

    val downstairsInitializationMethod = CoordinateProjection()
    val downstairsInitializationMethod2 = TriangulateProjection()

    // TODO: Option for PCA initialization

    var tsne: TSNE? = null

    override fun init(dataset: Dataset) {
        tsne = TSNE(dataset.computeUpstairsArray(), 2, perplexity, eta, 1000).also {
            dataset.setDownstairsData(it.coordinates)
        }
    }

    override fun addPoint(dataset: Dataset, point: DataPoint) {
        synchronized(dataset) {
            if (dataset.kdTree.size < 15) {
                downstairsInitializationMethod.addPoint(dataset, point)
            } else {
                downstairsInitializationMethod2.addPoint(dataset, point)
            }
        }
    }

    override var error: Double = 0.0

    override fun iterate(dataset: Dataset) {
        tsne?.let {
            it.update(1000)
            dataset.setDownstairsData(it.coordinates)
        }
        // TODO: Cost function?
    }

    override fun copy() = TSNEProjection()

    override val name = "TSNE"

    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ProjectionMethod.getTypes()
        }
    }

}