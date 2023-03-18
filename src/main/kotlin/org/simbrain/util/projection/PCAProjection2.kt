package org.simbrain.util.projection

import smile.projection.PCA

class PCAProjection2(dimension: Int): ProjectionMethod2(dimension) {

    val initialProjectionMethod = CoordinateProjection2(dimension)

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

}