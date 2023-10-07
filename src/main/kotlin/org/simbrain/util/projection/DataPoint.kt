package org.simbrain.util.projection

import org.simbrain.util.euclideanDistance

class DataPoint @JvmOverloads constructor(val upstairsPoint: DoubleArray, downstairsDimension: Int = 2, var label: String? = null) {

    val downstairsPoint = DoubleArray(downstairsDimension)

    fun setUpstairs(data: DoubleArray) {
        System.arraycopy(data, 0, this.upstairsPoint, 0, this.upstairsPoint.size)
    }

    fun setDownstairs(data: DoubleArray) {
        System.arraycopy(data, 0, this.downstairsPoint, 0, this.downstairsPoint.size)
    }

    fun euclideanDistance(other: DataPoint) = this.upstairsPoint.euclideanDistance(other.upstairsPoint)

    override fun toString() = """
        upstairs: ${upstairsPoint.contentToString()}
        downstairs: ${downstairsPoint.contentToString()}
    """.trimIndent()
}