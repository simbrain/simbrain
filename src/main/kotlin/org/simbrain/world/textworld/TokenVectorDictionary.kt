package org.simbrain.world.textworld

import smile.math.matrix.Matrix

/**
 * Associate String tokens with vector representations.
 */
class TokenVectorDictionary(
    tokens: List<String>,
    tokenVectorMatrix: Matrix
) {

    /**
     * Assume indices of the token list correspond to rows of the cocMatrix
     */
    var tokensMap: Map<String, Int> = tokens.mapIndexed{i, t -> t to i}.toMap()

    /**
     * Matrix whose rows correspond to vector representations of corresponding tokens.
     */
    var tokenVectorMatrix: Matrix = tokenVectorMatrix

    val size = tokensMap.size

    init {
        if (tokens.size != tokenVectorMatrix.nrows()) {
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
            return DoubleArray(size)
        }
    }

    // TOOD: Use n-tree to get a token from a provided vector

}