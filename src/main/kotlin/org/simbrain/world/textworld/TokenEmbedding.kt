package org.simbrain.world.textworld

import org.simbrain.util.*
import org.simbrain.util.projection.DataPoint
import org.simbrain.util.projection.KDTree
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.table.BasicDataFrame
import org.simbrain.util.table.SimbrainTablePanel
import org.simbrain.util.table.createFromDoubleArray
import smile.math.matrix.Matrix
import java.io.File

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
 * @param tokens
 * @param tokenVectorMatrix Matrix whose rows correspond to vector representations of corresponding tokens.
 * @param trainingDocument Document, if any, used to train this embedding.
 */
class TokenEmbedding(
    val tokens: List<String>,
    val embeddingType: EmbeddingType<*>,
    val tokenizer: Tokenizer<*>,
    var tokenVectorMatrix: Matrix,
    var trainingDocument: String? = null,
): CopyableObject {

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
        if (tokens.size != tokenVectorMatrix.nrow()) {
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
     *
     * If the input key is larger than the embedding dimension, truncate. If it is smaller, pad with zeros
     */
    fun getClosestWord(key: DoubleArray): String {
        val keyVector = DoubleArray(treeMap.dimension) { i -> key.getOrElse(i) { 0.0 } }
        return treeMap.findClosestPoint(DataPoint(keyVector))?.label!!
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

    override fun copy(): TokenEmbedding {
        return TokenEmbedding(tokens, embeddingType, tokenizer, tokenVectorMatrix)
    }
}

sealed class EmbeddingType<T : CopyableObject>(): CopyableObject {
    
    @UserParameter(label = "Remove stopwords", order = 60 )
    var removeStopWords = false

    @UserParameter(label = "Convert all upper case to lower case", order = 70)
    var convertToLowerCase = true
    
    class OneHot: EmbeddingType<OneHot>() {
        override fun build(
            tokenizer: Tokenizer<*>,
            docString: String
        ): TokenEmbedding {
            val tokens = tokenizer.tokenize(docString).map { it.token }.uniqueTokensFromArray().let {
                if (convertToLowerCase) it.map { it.lowercase() } else it
            }
            return TokenEmbedding(tokens, this, tokenizer, Matrix.eye(tokens.size), docString)
        }

        override fun copy(): OneHot {
            return OneHot().also {
                it.removeStopWords = this.removeStopWords
                it.convertToLowerCase = this.convertToLowerCase
            }
        }
    }

    class CoOccurrence(
        @UserParameter(label = "Window size", minimumValue = 1.0, order = 20)
        var windowSize: Int = 5,

        @UserParameter(label = "Bidirectional", order = 30)
        var bidirectional: Boolean = true,

        @UserParameter(label = "Use PPMI", order = 40)
        var usePPMI: Boolean = true,
    ) : EmbeddingType<CoOccurrence>() {

        /**
         * Generates co-occurrence matrix from a provided [docString].
         *
         * Example: if [windowSize] is 2 and [bidirectional] is true, then the context for "dog" in "the quick [dog] ran fastly"
         * is ["the", "quick", "ran", "fastly"].  If [windowSize] 2 and [bidirectional] false, then the context for "dog"
         * is ["the", "quick"].
         */
        override fun build(
            tokenizer: Tokenizer<*>,
            docString: String
        ): TokenEmbedding {

            if (windowSize == 0) throw IllegalArgumentException("windowsize must be greater than 0")

            var convertedDocString = docString.lowercase().removeSpecialCharacters()

            if(removeStopWords) {
                convertedDocString = convertedDocString.removeWords(stopWords)
            }

            val words = convertedDocString.tokenize(tokenizer).map { if (convertToLowerCase) it.token.lowercase() else it.token }

            val tokens = words.uniqueTokensFromArray()

            // Set up matrix
            val matrixSize = tokens.size
            var cocMatrix = Matrix(matrixSize, matrixSize)

            for (sentenceIndex in words.indices) {
                val maxIndex = words.size - 1  // used for window range check

                val currentToken = words[sentenceIndex] // Current iterated token

                val contextLowerLimit = sentenceIndex - windowSize
                val contextUpperLimit = if (bidirectional) (sentenceIndex + windowSize) else (sentenceIndex)

                for (contextIndex in contextLowerLimit..contextUpperLimit) {
                    if (contextIndex in 0..maxIndex && contextIndex != sentenceIndex) {
                        val currentContext = words[contextIndex]
                        val tokenCoordinate = tokens.indexOf(currentToken)
                        val contextCoordinate = tokens.indexOf(currentContext)
                        // print(listOf("Current Token:", currentToken, tokenCoordinate))
                        // println(listOf("Current Context",currentContext, contextCoordinate))
                        cocMatrix[tokenCoordinate, contextCoordinate] =
                            cocMatrix[tokenCoordinate, contextCoordinate] + 1
                    }
                }
            }

            if (usePPMI) {
                cocMatrix = manualPPMI(cocMatrix, true)
            }

            return TokenEmbedding(tokens, this, tokenizer,  cocMatrix.replaceNaN(0.0), docString)
        }

        override fun copy(): CoOccurrence {
            return CoOccurrence(
                windowSize = this.windowSize,
                bidirectional = this.bidirectional,
                usePPMI = this.usePPMI
            ).also {
                it.removeStopWords = this.removeStopWords
                it.convertToLowerCase = this.convertToLowerCase
            }
        }
    }
    
    class Random(
        @UserParameter(label = "Vector length", minimumValue = 1.0, order = 20)
        var vectorLength: Int = 10,
        
        @UserParameter(label = "Distribution", order = 1)
        var distribution: ProbabilityDistribution = NormalDistribution()
    ) : EmbeddingType<Random>() {
        
        override fun build(tokenizer: Tokenizer<*>, docString: String): TokenEmbedding {
            val tokens = tokenizer.tokenize(docString)
                .map { it.token }
                .uniqueTokensFromArray()
                .let { if (convertToLowerCase) it.map { token -> token.lowercase() } else it }
            
            // Generate random vector for each token by sampling from distribution
            val randomMatrix = Matrix(tokens.size, vectorLength)
            for (i in 0 until tokens.size) {
                for (j in 0 until vectorLength) {
                    randomMatrix[i, j] = distribution.sampleDouble()
                }
            }
            
            return TokenEmbedding(tokens, this, tokenizer, randomMatrix, docString)
        }
        
        override fun copy(): Random {
            return Random(vectorLength, distribution).also {
                it.removeStopWords = this.removeStopWords
                it.convertToLowerCase = this.convertToLowerCase
            }
        }
    }
    
    class Custom: EmbeddingType<Custom>() {
        override fun build(
            tokenizer: Tokenizer<*>,
            docString: String
        ): TokenEmbedding {
            throw IllegalStateException("Custom embeddings must be manually loaded")
        }

        override fun copy(): Custom {
            return Custom().also {
                it.removeStopWords = this.removeStopWords
                it.convertToLowerCase = this.convertToLowerCase
            }
        }
    }

    abstract fun build(tokenizer: Tokenizer<*>, docString: String): TokenEmbedding
}

class TokenEmbeddingBuilder(
    @UserParameter(
        label = "Embedding type",
        description = "Method for converting text to vectors",
        order = 1
    ) var embeddingType: EmbeddingType<*> = EmbeddingType.CoOccurrence(),

    @UserParameter(
        label = "Tokenizer",
        description = "Options for tokenizing text",
        order = 70
    )
    var tokenizer: Tokenizer<*> = SimpleTokenizer()

): EditableObject {

    override val name: String
        get() = "Token Embedding Builder"

    /**
     * Extract a token embedding from the provided string.
     */
    fun build(docString: String) = embeddingType.build(tokenizer.also { it.initialize(docString) }, docString)
}

/**
 * Returns list of embedding types for the dropdown, excluding Custom which is for internal use only.
 */
fun getEmbeddingTypeListForExtraction(): List<Class<out EditableObject>> {
    return listOf(
        EmbeddingType.OneHot::class.java,
        EmbeddingType.CoOccurrence::class.java,
        EmbeddingType.Random::class.java
    )
}

class ExtractEmbeddingOptions(
    /**
     * If true, show document path selector. If false, document will be provided directly.
     */
    var showDocumentPath: Boolean = true
): EditableObject {
    
    var documentPath by GuiEditable(
        initValue = "",
        description = "Text file to extract embeddings from",
        order = 0,
        useFileChooser = true,
        fileChooserInitialDirectory = TextWorldPreferences.sampleTextsDirectory,
        conditionallyVisibleBy = ExtractEmbeddingOptions::showDocumentPath
    )
    
    var embeddingType: EmbeddingType<*> by GuiEditable(
        initValue = EmbeddingType.CoOccurrence() as EmbeddingType<*>,
        description = "Method for converting text to vectors",
        order = 1,
        typeMapProvider = ::getEmbeddingTypeListForExtraction
    )
    
    var tokenizer by GuiEditable(
        initValue = SimpleTokenizer() as Tokenizer<*>,
        description = "Options for tokenizing text",
        order = 70
    )
    
    override val name: String get() = "Extract Embedding Options"
    
    /**
     * Build embedding from file path (when showDocumentPath is true).
     */
    fun buildEmbedding(): TokenEmbedding {
        val docString = Utils.readFileContents(File(documentPath))
        return embeddingType.build(tokenizer.also { it.initialize(docString) }, docString).also {
            it.trainingDocument = docString
        }
    }
    
    /**
     * Build embedding from provided document string (when showDocumentPath is false).
     */
    fun buildEmbeddingFromText(docString: String): TokenEmbedding {
        return embeddingType.build(tokenizer.also { it.initialize(docString) }, docString).also {
            it.trainingDocument = docString
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
    val tokenizer = SimpleTokenizer()
    textworld.tokenEmbedding = TokenEmbedding(listOf("Word 1", "Word 2"), embeddingType = EmbeddingType.OneHot(), tokenizer, embeddings)
    val viewer = SimbrainTablePanel(textworld.tokenEmbedding.createTableModel())
    viewer.displayInDialog()
}
