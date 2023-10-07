package org.simbrain.world.textworld

import org.simbrain.util.projection.DataPoint2
import org.simbrain.util.projection.KDTree
import org.simbrain.util.table.SimbrainDataModel
import org.simbrain.util.table.createFromDoubleArray
import smile.math.matrix.Matrix

/**
 * Associates string tokens with vector representations and vice-versa.
 */
class TokenVectorMap(
    tokens: List<String>,
    /**
     * Matrix whose rows correspond to vector representations of corresponding tokens.
     */
    var tokenVectorMatrix: Matrix
) {

    /**
     * Assume indices of the token list correspond to rows of the cocMatrix
     */
    var tokensMap: Map<String, Int> = tokens.mapIndexed{i, t -> t to i}.toMap()

    /**
     * Number of entries in the dictionary, i.e. number of words that have associated embeddings.
     */
    val size = tokensMap.size

    /**
     * The number of dimensions in the word embedding space. Tokens are associated with vectors with this many
     * components.
     *
     */
    var dimension = size
        // Currently because the matrices are always square the dimension just corresponds to number of rows
        get() = size

    /**
     * N-Tree (optimized to find vectors near a given vector) associating vectors with tokens.
     */
    private val treeMap = KDTree(dimension).apply {
        tokensMap.forEach { (token, i) ->
            insert(DataPoint2(tokenVectorMatrix.row(i), label = token))
        }
    }

    init {
        if (tokens.size != tokenVectorMatrix.nrow()) {
            throw IllegalArgumentException("token list must be same length as token vector matrix has rows")
        }
    }

    /**
     * Return the vector associated with given string or a 0 vector if none found
     */
    fun get(token: String): DoubleArray {
        val tokenIndex = tokensMap[token]
        if (tokenIndex != null) {
            return tokenVectorMatrix.row(tokenIndex)
        } else {
            // Zero array if no matching token is found
            return DoubleArray(dimension)
        }
    }

    /**
     * Finds the closest vector in terms of Euclidean distance, then returns the
     * String associated with it.
     */
    fun getClosestWord(key: DoubleArray): String {
        // TODO: Add a default minimum distance and if above that, return null or zero vector
        return treeMap.findClosestPoint(DataPoint2(key))?.label!!
    }

    fun createTableModel(): SimbrainDataModel {
        val table = createFromDoubleArray(tokenVectorMatrix.replaceNaN(0.0).toArray())
        table.setColumnNames(tokensMap.keys.toList())
        table.rowNames = tokensMap.keys.toList()
        return table
    }

}