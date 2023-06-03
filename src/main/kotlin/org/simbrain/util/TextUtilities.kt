package org.simbrain.util

import smile.math.matrix.Matrix
import smile.nlp.tokenizer.SimpleSentenceSplitter
import javax.swing.JScrollPane
import javax.swing.JTextArea

/**
 * Sentence tokenizer: parse document into sentences and return as a list of sentences.
 *
 * Forward to Smile's sentence splitter.
 */
fun String.tokenizeSentencesFromDoc(): List<String> {
    return SimpleSentenceSplitter.getInstance().split(this).toList()
}

/**
 * Trims extra whitespace and removes newlines, returns, and tabs.
 */
fun String.removeSpecialCharacters(): String {
    return this.trim().replace("[\n\r\t]".toRegex(), " ").replace("\\s+".toRegex(), " ")
}

/**
 * https://www.techiedelight.com/remove-punctuation-from-a-string-in-kotlin/
 */
fun String.removePunctuation(): String {
    return this.replace("\\p{Punct}".toRegex(), "");
}

/**
 * Word tokenizer: parse sentence into words.
 */
fun String.tokenizeWordsFromSentence(): List<String> {
    return this.lowercase().removePunctuation().split(" ")
}

/**
 * Unique tokens: all unique tokens (contexts)
 * Converts to lowercase
 */
fun List<String>.uniqueTokensFromArray(): List<String> {
    return distinctBy { it.lowercase() }
}

/**
 * Removes stop words from a list of strings
 * Stopword source file: https://gist.github.com/larsyencken/1440509#file-stopwords-txt
 */
fun removeStopWords(words: List<String>) : List<String> {
    val stopWords = arrayListOf("a","about","above","across","after","again","against","all","almost","alone","along","already","also","although","always","among","an","and","another","any","anybody","anyone","anything","anywhere","are","area","areas","around","as","ask","asked","asking","asks","at","away","b","back","backed","backing","backs","be","became","because","become","becomes","been","before","began","behind","being","beings","best","better","between","big","both","but","by","c","came","can","cannot","case","cases","certain","certainly","clear","clearly","come","could","d","did","differ","different","differently","do","does","done","down","down","downed","downing","downs","during","e","each","early","either","end","ended","ending","ends","enough","even","evenly","ever","every","everybody","everyone","everything","everywhere","f","face","faces","fact","facts","far","felt","few","find","finds","first","for","four","from","full","fully","further","furthered","furthering","furthers","g","gave","general","generally","get","gets","give","given","gives","go","going","good","goods","got","great","greater","greatest","group","grouped","grouping","groups","h","had","has","have","having","he","her","here","herself","high","high","high","higher","highest","him","himself","his","how","however","i","if","important","in","interest","interested","interesting","interests","into","is","it","its","itself","j","just","k","keep","keeps","kind","knew","know","known","knows","l","large","largely","last","later","latest","least","less","let","lets","like","likely","long","longer","longest","m","made","make","making","man","many","may","me","member","members","men","might","more","most","mostly","mr","mrs","much","must","my","myself","n","necessary","need","needed","needing","needs","never","new","new","newer","newest","next","no","nobody","non","noone","not","nothing","now","nowhere","number","numbers","o","of","off","often","old","older","oldest","on","once","one","only","open","opened","opening","opens","or","order","ordered","ordering","orders","other","others","our","out","over","p","part","parted","parting","parts","per","perhaps","place","places","point","pointed","pointing","points","possible","present","presented","presenting","presents","problem","problems","put","puts","q","quite","r","rather","really","right","right","room","rooms","s","said","same","saw","say","says","second","seconds","see","seem","seemed","seeming","seems","sees","several","shall","she","should","show","showed","showing","shows","side","sides","since","small","smaller","smallest","so","some","somebody","someone","something","somewhere","state","states","still","still","such","sure","t","take","taken","than","that","the","their","them","then","there","therefore","these","they","thing","things","think","thinks","this","those","though","thought","thoughts","three","through","thus","to","today","together","too","took","toward","turn","turned","turning","turns","two","u","under","until","up","upon","us","use","used","uses","v","very","w","want","wanted","wanting","wants","was","way","ways","we","well","wells","went","were","what","when","where","whether","which","while","who","whole","whose","why","will","with","within","without","work","worked","working","works","would","x","y","year","years","yet","you","young","younger","youngest","your","yours","z")
    val uniqueTargets = words.distinctBy { it.lowercase() }
    var filteredTargets = listOf<String>()
    for (target in uniqueTargets) {
        if (!stopWords.contains(target)) filteredTargets += listOf<String>(target)
    }
    return filteredTargets
}


// After writing, found out that SimBrain already has an outerProduct function.
fun outerProduct(vectorU: DoubleArray, vectorV: DoubleArray): Matrix {
    // u (*) v = [u1 ... ui].vertical * [v1 ... vj].horizontal
    val rows = vectorU.size
    val cols = vectorV.size
    val outerProductMatrix = Matrix(rows, cols)
    for (indexU in vectorU.indices) for (indexV in vectorV.indices) outerProductMatrix[indexU, indexV] =
        vectorU[indexU] * vectorV[indexV]
    return outerProductMatrix
}

/**
 * Positive Pointwise Mutual Information weighting
 * Adapted from: https://stackoverflow.com/questions/58701337/how-to-construct-ppmi-matrix-from-a-text-corpus
 *
 * Weights the co-occurrence values to avoid word-frequency-bias in embeddings. Words like "the" and "a" that should
 * not be considered meaningful in terms of co-occurrence are down-weighted. Less frequent words on the other, like
 * "platitude" or "espresso", that are more meaningful in terms of co-occurrences, are up-weighted.
 *
 * Generally considered better for word embeddings.
 *
 * "PPMI measures how much the probability of a target–context pair estimated in the training corpus is higher than
 * the probability we should expect if the target and the context occurred independently of one another." (Lenci, 2018)
 *
 * @param positive if true, changes negative adjusted co-occurrence values to 0
 */
fun manualPPMI(cocMatrix: Matrix, positive: Boolean = true): Matrix {
    val columnTotals = cocMatrix.colSums()
    val totalSum = columnTotals.sum()
    val rowTotals = cocMatrix.rowSums()

    val expectedValues = outerProduct(rowTotals, columnTotals) / totalSum
    val adjustedMatrix = cocMatrix.clone().div(expectedValues)

    if (positive) {
        for (indexRow in 0 until adjustedMatrix.nrow()) {
            for (indexCol in 0 until adjustedMatrix.ncol()) {
                if (adjustedMatrix[indexRow, indexCol] < 0) {
                    adjustedMatrix[indexRow, indexCol] = 0.0
                }
            }
        }
    }
    return adjustedMatrix
}

// TODO: Return a TokenVectorDictionary

/**
 * Generates co-occurrence matrix from a provided [docString].
 *
 * Example: if [windowSize] is 2 and [skipGram] is true, then the context for "dog" in "the quick dog ran fastly"
 * is ["the", "quick", "ran", "fastly"].  If [windowSize] 2 and [skipGram] false, then the context for "dog"
 * is ["the", "quick"].
 *
 * @param windowSize specifies how many words should be included in a context.
 * @param skipGram  if true, window includes this many tokens before AND after; if false the window only includes
 * previous tokens.
 * @return a symmetrical co-occurrence matrix with as many rows and columns as there are unique tokens in [docString].
 *
 */
fun generateCooccurrenceMatrix(docString: String, windowSize: Int = 2, skipGram: Boolean = false , usePPMI: Boolean = true):
        Pair<List<String>, Matrix> {
    // println(docString)
    val convertedDocString = docString.removeSpecialCharacters()

    if (windowSize == 0) throw IllegalArgumentException("windowsize must be greater than 0")

    // get tokens from whole document
    val tokenizedSentence = convertedDocString.tokenizeWordsFromSentence()
    val tokens = tokenizedSentence.uniqueTokensFromArray()
    val targets = removeStopWords(tokens)

    // Split document into sentences
    val sentences = convertedDocString.tokenizeSentencesFromDoc()

    // Set up matrix
    val matrixSize = tokens.size
    val cooccurrenceSmileMatrix = Matrix(matrixSize, matrixSize)

    // cooccurrenceMatrix[0][1] = 2 // cooccurrenceMatrix[target][context]

    // Loop through sentences, through words
    for (sentence in sentences) {
        // println(sentence)
        val tokenizedSentence = sentence.tokenizeWordsFromSentence()
        for (sentenceIndex in tokenizedSentence.indices) {
            val maxIndex = tokenizedSentence.size - 1  // used for window range check

            val currentToken = tokenizedSentence[sentenceIndex] // Current iterated token

            val contextLowerLimit = sentenceIndex - windowSize
            val contextUpperLimit = if (skipGram) (sentenceIndex + windowSize) else (sentenceIndex)

            for (contextIndex in contextLowerLimit..contextUpperLimit) {
                if (contextIndex in 0..maxIndex && contextIndex != sentenceIndex) {
                    val currentContext = tokenizedSentence[contextIndex]
                    val tokenCoordinate = tokens.indexOf(currentToken)
                    val contextCoordinate = tokens.indexOf(currentContext)
                    // print(listOf("Current Token:", currentToken, tokenCoordinate))
                    // println(listOf("Current Context",currentContext, contextCoordinate))
                    cooccurrenceSmileMatrix[tokenCoordinate, contextCoordinate] =
                        cooccurrenceSmileMatrix[tokenCoordinate, contextCoordinate] + 1
                }
            }


        }
    }
    if (usePPMI) {
        return Pair(tokens, manualPPMI(cooccurrenceSmileMatrix, true).replaceNaN(0.0))
    }
    return Pair(tokens, cooccurrenceSmileMatrix.replaceNaN(0.0))
}

/**
 * Get an embedding from a matrix given matrix, index, and word
 */
fun wordEmbeddingQuery(targetWord: String, tokens: List<String>, cooccurrenceMatrix: Matrix): DoubleArray {
    val targetWordIndex = tokens.indexOf(targetWord.lowercase())
    return cooccurrenceMatrix.col(targetWordIndex)
}

/**
 * Generalized embedding similarity function.
 * The parameter [useCosine] defaults to true, so it calculates cosine similarity
 * of two vectors (higher values are more similar). If changed to false, it will
 * calculate the dot product between the two vectors.
 *
 * Cosine similarity is normalized dot product.
 */
fun embeddingSimilarity(vectorA: DoubleArray, vectorB: DoubleArray, simfun: (DoubleArray, DoubleArray) -> Double =
    smile.math.MathEx::cos): Double {
    return simfun(vectorA, vectorB)
}

@JvmOverloads
fun textEntryDialog(initialString: String, title: String = "Edit Text", columns: Int = 20, rows: Int = 5, commitAction: (String) -> Unit = {}): StandardDialog {
    val dialog = StandardDialog(null, title)
    val textArea = JTextArea(initialString)
    textArea.columns = columns
    textArea.rows = rows

    val scrollPane = JScrollPane(textArea)
    scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
    dialog.contentPane = scrollPane

    textArea.requestFocusInWindow()
    dialog.addClosingTask {
        commitAction(textArea.text)
    }

    return dialog
}

// Test main
fun main() {
    val chooser = SFileChooser(".", "Text import", "txt")
    val theFile = chooser.showOpenDialog()
    if (theFile != null) {
        val text = Utils.readFileContents(theFile)
        generateCooccurrenceMatrix(text)
    }
}
