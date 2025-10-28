package org.simbrain.util.projection

import org.simbrain.util.UserParameter
import smile.manifold.TSNE

class TSNEProjection: ProjectionMethod(), IterableProjectionMethod  {

    // TODO: Re-init when setting these parameters

    @UserParameter(label = "Perplexity", order = 10)
    var perplexity: Double = 20.0

    @UserParameter(label = "Learning Rate", order = 20)
    var eta: Double = 200.0

    @UserParameter(label = "Iteration per Update", order = 30)
    var iterations: Int = 100

    val downstairsInitializationMethod = CoordinateProjection()
    val downstairsInitializationMethod2 = TriangulateProjection()

    // TODO: Option for PCA initialization

    var tsne: TSNE? = null

    override fun init(dataset: Dataset) {
        if (dataset.kdTree.size < 2) return
        tsne = TSNE(dataset.computeUpstairsArray(), 2, perplexity, eta, iterations).also {
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
            if (tsne != null) {
                init(dataset)
            }
        }
    }

    override var error: Double = 0.0

    override fun iterate(dataset: Dataset) {
        synchronized(dataset) {
            if (dataset.kdTree.size < 2) return
            tsne?.let {
                it.update(iterations)
                dataset.setDownstairsData(it.coordinates)
                error = it.cost()
            }
        }
    }

    override fun copy() = TSNEProjection().also { 
        it.perplexity = perplexity
        it.eta = eta
        it.iterations = iterations
    }

    override val name = "TSNE"

}