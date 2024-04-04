package org.simbrain.world.textworld

import org.simbrain.util.*
import org.simbrain.util.projection.DataPoint
import org.simbrain.util.projection.KDTree
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.table.BasicDataFrame
import org.simbrain.util.table.SimbrainTablePanel
import org.simbrain.util.table.createFromDoubleArray
import smile.math.matrix.Matrix

/**
 * Associates string tokens with vector representations. Each member of a list of String tokens is associated with
 * a row of a Matrix of doubles.
 *
 * Allows for reverse mappings from vectors back to tokens using a [KDTree].
 *
 * All tokens are converted to lower case.
 *
 * Cannot currently be mutated after creation.
 *
 * @param inputTokenList tokens prior to conversion to lower case
 * @param tokenVectorMatrix Matrix whose rows correspond to vector representations of corresponding tokens.
 * @param trainingDocument Document, if any, used to train this embedding.
 */
class TokenEmbedding(
    inputTokenList: List<String>,
    var tokenVectorMatrix: Matrix,
    var trainingDocument: String? = null
) {

    val tokens = inputTokenList.map { it.lowercase() }

    /**
     * Associates tokens with row indices of tokenVectorMatrix.
     * All tokens are converted to lower case. This map is reused in creating the tokenVectorMatrix
     * so all tokens are lower case.
     */
    var tokensMap: Map<String, Int> = tokens.mapIndexed { i, t -> t to i }.toMap()

    /**
     * Number of entries in the embedding, i.e. number of words that have associated embeddings.
     */
    val size = tokensMap.size

    /**
     * The number of dimensions in the word embedding space.
     */
    val dimension = tokenVectorMatrix.ncol()

    init {
        if (inputTokenList.size != tokenVectorMatrix.nrow()) {
            throw IllegalArgumentException("token list must be same length as token vector matrix has rows")
        }
    }

    /**
     * N-Tree (optimized to find vectors near a given vector) associating vectors with tokens.
     */
    private val treeMap = KDTree(dimension).apply {
        tokensMap.forEach { (token, i) ->
            insert(DataPoint(tokenVectorMatrix.row(i), label = token))
        }
    }

    /**
     * Return the vector associated with given string or a 0 vector if none found
     */
    fun get(token: String): DoubleArray {
        val searchToken = token.lowercase()
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

    override fun toString(): String {
        return tokens.mapIndexed{ i, t -> "$t -> ${tokenVectorMatrix.row(i).contentToString()}"  }.joinToString("\n")
    }

    /**
     * Creates a table model object for an embedding. Column headings are the same as row headings for one-hot and
     * default co-occurrence matrices.
     */
    fun createTableModel(useColumnNames: Boolean = false): BasicDataFrame {
        val table = createFromDoubleArray(tokenVectorMatrix.replaceNaN(0.0).toArray())
        table.isMutable = false
        table.rowNames = tokensMap.keys.toList()
        if (useColumnNames) {
            table.columnNames = tokensMap.keys.toList()
        }
        return table
    }
}

enum class EmbeddingType {ONE_HOT, COC, CUSTOM}

class TokenEmbeddingBuilder(): EditableObject {

    @UserParameter(label = "Embedding type", description = "Method for converting text to vectors", order = 1 )
    var embeddingType = EmbeddingType.COC

    @UserParameter(label = "Window size", minimumValue =  1.0, order = 20 )
    var windowSize = 5

    @UserParameter(label = "Bidirectional", order = 30 )
    var bidirectional = true

    @UserParameter(label = "Use PPMI", order = 40 )
    var usePPMI = true

    @UserParameter(label = "Remove stopwords", order = 60 )
    var removeStopWords = false

    /**
     * Extract a token embedding from the provided string.
     */
    fun build(docString: String) = when (embeddingType) {
        EmbeddingType.ONE_HOT -> {
            val tokens = docString.tokenizeWordsFromString().uniqueTokensFromArray()
            TokenEmbedding(tokens, Matrix.eye(tokens.size))
        }
        EmbeddingType.COC -> {
            generateCooccurrenceMatrix(docString, windowSize, bidirectional, usePPMI, removeStopWords)
        }
        else -> {
            throw IllegalStateException("Custom embeddings must be manually loaded")
        }
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
    textworld.tokenEmbedding = TokenEmbedding(listOf("Word 1", "Word 2"), embeddings)
    val viewer = SimbrainTablePanel(textworld.tokenEmbedding.createTableModel())
    viewer.displayInDialog()
}
