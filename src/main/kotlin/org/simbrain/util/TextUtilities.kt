package org.simbrain.util

import smile.nlp.tokenizer.SimpleSentenceSplitter

/**
 * Sentence tokenizer: parse document into sentences and return as a list of sentences.
 *
 * Forward to Smile's sentence splitter.
 */
fun tokenizeSentencesFromDoc(docString: String) : List<String> {
    return  SimpleSentenceSplitter.getInstance().split(docString).toList()
}

/**
 * https://www.techiedelight.com/remove-punctuation-from-a-string-in-kotlin/
 */
fun removePunctuation(str: String) : String {
    return str.replace("\\p{Punct}".toRegex(), "");
}

/**
 * Word tokenizer: parse sentence into words.
 */
fun tokenizeWordsFromSentence(sentence: String) : List<String> {
    return removePunctuation(sentence).split(" ")
}
