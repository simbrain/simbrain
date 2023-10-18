package org.simbrain.world.textworld

import org.simbrain.util.displayInDialog
import org.simbrain.util.projection.DataPoint
import org.simbrain.util.projection.KDTree
import org.simbrain.util.table.BasicDataWrapper
import org.simbrain.util.table.SimbrainDataViewer
import org.simbrain.util.table.createFromDoubleArray
import smile.math.matrix.Matrix
/**
 * Associates string tokens with vector representations.
 *
 * Also allows for reverse mappings from vectors back to tokens using a [KDTree].
 */
class TokenEmbedding(
    val tokens: List<String>,
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
     * Number of entries in the embedding, i.e. number of words that have associated embeddings.
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
            insert(DataPoint(tokenVectorMatrix.row(i), label = token))
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
    fun get(token: String, lowerCase: Boolean = true): DoubleArray {
        val searchToken = if (lowerCase) token.lowercase() else token
        val tokenIndex = tokensMap[searchToken]
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
        return treeMap.findClosestPoint(DataPoint(key))?.label!!
    }

    /**
     * Creates a table model object for an embedding.  Column headings are the same as row headings for one-hot and
     * default co-occurrence matrices.
     */
    fun createTableModel(type: TextWorld.EmbeddingType): BasicDataWrapper {
        val table = createFromDoubleArray(tokenVectorMatrix.replaceNaN(0.0).toArray())
        table.rowNames = tokensMap.keys.toList()
        if (type == TextWorld.EmbeddingType.COC || type == TextWorld.EmbeddingType.ONE_HOT) {
            table.setColumnNames(tokensMap.keys.toList())
        }
        return table
    }
}

fun main() {
    val textworld = TextWorld()
    val embeddings = Matrix.of(
        arrayOf(
            doubleArrayOf(1.0, 2.0, 3.0),
            doubleArrayOf(4.0, 5.0, 6.0),
        )
    )
    textworld.loadCustomEmbedding(listOf("Word 1", "Word 2"), embeddings)
    val viewer = SimbrainDataViewer(textworld
        .tokenEmbedding.createTableModel(TextWorld.EmbeddingType.CUSTOM))
    viewer.displayInDialog()
}
