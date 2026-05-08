package org.simbrain.util.nettalk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NettalkCorpusTest {

    @Test
    fun `parses a clean line`() {
        val text = "hello\thxl-o\t>0<>1\t0"
        val entries = parseNettalkCorpus(text)
        assertEquals(1, entries.size)
        val e = entries[0]
        assertEquals("hello", e.word)
        assertEquals("hxl-o", e.phonemes)
        assertEquals(">0<>1".take(5), e.stress.take(5))
        assertEquals(0, e.flag)
    }

    @Test
    fun `skips header and blank lines`() {
        val text = """


            ${'\t'}${'\t'}Some header text
            hello${'\t'}hxl-o${'\t'}>0<>1${'\t'}0
        """.trimIndent()
        val entries = parseNettalkCorpus(text)
        assertEquals(1, entries.size)
        assertEquals("hello", entries[0].word)
    }

    @Test
    fun `tolerates trailing whitespace on flag`() {
        val entries = parseNettalkCorpus("hello\thxl-o\t>0<>1\t0 ")
        assertEquals(1, entries.size)
        assertEquals(0, entries[0].flag)
    }

    @Test
    fun `loads bundled corpus`() {
        val entries = loadNettalkCorpus()
        assertTrue(entries.size in 19_900..20_010, "Expected ~20k entries, got ${entries.size}")
        val regular = entries.count { it.isRegular }
        assertTrue(regular > 19_000, "Expected most entries to be regular, got $regular")
    }

    @Test
    fun `frequency-sorted corpus has common words near the top`() {
        val entries = loadNettalkCorpus(byFrequency = true)
        assertTrue(entries.size in 19_900..20_010, "Expected ~20k entries, got ${entries.size}")
        val firstWords = entries.take(20).map { it.word }
        for (common in listOf("the", "of", "and", "to", "in")) {
            assertTrue(common in firstWords) {
                "Expected '$common' in first 20 frequency-sorted words, got: $firstWords"
            }
        }
    }
}

class NettalkPhonologyTest {

    @Test
    fun `every corpus phoneme is in the table`() {
        val seen = loadNettalkCorpus().flatMap { it.phonemes.toSet() }.toSet()
        for (p in seen) {
            assertTrue(NettalkPhonology.phonemeFeatures.containsKey(p)) {
                "Corpus uses phoneme '$p' which is missing from the phonology table"
            }
        }
    }

    @Test
    fun `each phoneme has a unique feature vector`() {
        val seenVectors = mutableMapOf<List<Int>, Char>()
        for ((p, vec) in NettalkPhonology.phonemeFeatures) {
            val key = vec.toList()
            val collision = seenVectors[key]
            assertEquals(null, collision) {
                "Phonemes '$p' and '$collision' have identical feature vectors"
            }
            seenVectors[key] = p
        }
    }

    @Test
    fun `encode then decode round-trips`() {
        for ((p, _) in NettalkPhonology.phonemeFeatures) {
            for (s in NettalkPhonology.stressNames.map { it[0] }) {
                val enc = NettalkPhonology.encodeOutput(p, s)
                val (decP, decS) = NettalkPhonology.decodeOutput(enc)
                assertEquals(p, decP)
                assertEquals(s, decS)
            }
        }
    }

    @Test
    fun `eSpeak mapping covers all spoken phonemes`() {
        val spoken = NettalkPhonology.phonemeFeatures.keys - '-'
        for (p in spoken) {
            assertTrue(NettalkPhonology.toEspeak.containsKey(p)) {
                "Phoneme '$p' has no eSpeak mapping"
            }
        }
    }

    @Test
    fun `nettalkToEspeak drops silent positions`() {
        val out = NettalkPhonology.nettalkToEspeak("hxl-o")
        assertTrue(!out.contains('-'))
        assertTrue(out.startsWith("h"))
    }
}

class NettalkEncoderTest {

    @Test
    fun `window size must be odd`() {
        try {
            NettalkEncoder(windowSize = 6)
            error("expected exception")
        } catch (_: IllegalArgumentException) { /* ok */ }
    }

    @Test
    fun `window centered on each letter has correct hot bit`() {
        val enc = NettalkEncoder(windowSize = 7)
        val word = "cat"
        for (i in word.indices) {
            val v = enc.encodeWindow(word, i)
            val centerStart = enc.halfWindow * enc.perPositionInputSize
            val centerHot = (0 until enc.perPositionInputSize).single { v[centerStart + it] == 1.0 }
            assertEquals(word[i] - 'a', centerHot)
        }
    }

    @Test
    fun `out of bounds positions are blanks`() {
        val enc = NettalkEncoder(windowSize = 7)
        val v = enc.encodeWindow("ab", 0)
        val leftMostStart = 0
        val blankIdx = 26
        assertEquals(1.0, v[leftMostStart + blankIdx])
    }

    @Test
    fun `entry encodes one row per letter position`() {
        val enc = NettalkEncoder(windowSize = 7)
        val entry = NettalkEntry("cat", "k@t", ">1<", 0)
        val rows = enc.encodeEntry(entry)
        assertEquals(3, rows.size)
        for ((input, target) in rows) {
            assertEquals(enc.inputSize, input.size)
            assertEquals(enc.outputSize, target.size)
        }
        val (_, firstTarget) = rows[0]
        val expected = NettalkPhonology.encodeOutput('k', '>')
        assertEquals(expected.toList(), firstTarget.toList())
    }

    @Test
    fun `encoder dimensions match NETtalk paper`() {
        val enc = NettalkEncoder(windowSize = 7)
        assertEquals(203, enc.inputSize)
        assertEquals(26, enc.outputSize)
    }

    @Test
    fun `repeats and shuffleSeed produce more diverse contexts`() {
        val enc = NettalkEncoder(windowSize = 7)
        val entries = listOf(
            NettalkEntry("cat", "k@t", ">1<", 0),
            NettalkEntry("dog", "dog", ">1<", 0),
            NettalkEntry("fish", "fIS-", ">1<<", 0)
        )
        val singlePass = enc.encodeAsContinuousText(entries, gap = 1)
        val threeShuffled = enc.encodeAsContinuousText(entries, gap = 1, repeats = 3, shuffleSeed = 7L)
        // Three passes contribute three copies of [singlePass] joined by 2 inter-pass gaps
        // (one gap of length `gap` between each pair of consecutive passes).
        assertEquals(singlePass.inputs.size * 3 + 2, threeShuffled.inputs.size)
        // Deterministic with the same seed.
        val again = enc.encodeAsContinuousText(entries, gap = 1, repeats = 3, shuffleSeed = 7L)
        assertEquals(threeShuffled.rowLabels, again.rowLabels)
    }

    @Test
    fun `continuous text encoding spans word boundaries`() {
        val enc = NettalkEncoder(windowSize = 7)
        val entries = listOf(
            NettalkEntry("cat", "k@t", ">1<", 0),
            NettalkEntry("dog", "dog", ">1<", 0)
        )
        val out = enc.encodeAsContinuousText(entries, gap = 1)
        // 3 letters in cat + 1 separator space + 3 letters in dog = 7 training rows.
        assertEquals(7, out.inputs.size)
        // The window for the 't' in 'cat' (index 2 in joined "cat dog") should see ' d' on the right.
        // joined positions: 0=c 1=a 2=t 3=' ' 4=d 5=o 6=g. Window at i=2: positions -1..5 → " catdo"
        // Index 4 of the window (right-of-center, position 3 in "cat dog") should be a space.
        val v = out.inputs[2]
        val rightOfCenterStart = (enc.halfWindow + 1) * enc.perPositionInputSize
        // letterIndex(' ') == 26
        assertEquals(1.0, v[rightOfCenterStart + 26])
        // The separator space at index 3 should have target phoneme `-` and stress `<`.
        val spaceTarget = out.targets[3]
        val expectedSpace = NettalkPhonology.encodeOutput('-', '<')
        assertEquals(expectedSpace.toList(), spaceTarget.toList())
    }
}
