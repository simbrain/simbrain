package org.simbrain.network.trainers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.util.getSimbrainXStream
import kotlin.random.Random

class TrainingDatasetConverterTest {

    @Test
    fun `TrainingDataset round-trips through XStream`() {
        val rng = Random(0)
        val rows = 7
        val inputCols = 5
        val targetCols = 3
        val ds = TrainingDataset(
            inputs = MutableList(rows) { MutableList(inputCols) { rng.nextDouble() } },
            targets = MutableList(rows) { MutableList(targetCols) { rng.nextDouble() } },
            inputRowNames = (0 until rows).map { "row_$it" },
            targetRowNames = (0 until rows).map { "row_$it" },
            inputColumnNames = (0 until inputCols).map { "in_$it" },
            targetColumnNames = (0 until targetCols).map { "out_$it" },
        )
        val xml = getSimbrainXStream().toXML(ds)
        val restored = getSimbrainXStream().fromXML(xml) as TrainingDataset

        assertEquals(ds.inputSize, restored.inputSize)
        assertEquals(ds.targetSize, restored.targetSize)
        assertEquals(ds.inputs.size, restored.inputs.size)
        for (i in 0 until ds.inputs.size) {
            assertEquals(ds.inputs[i], restored.inputs[i], "input row $i")
            assertEquals(ds.targets[i], restored.targets[i], "target row $i")
        }
        assertEquals(ds.inputRowNames, restored.inputRowNames)
        assertEquals(ds.targetRowNames, restored.targetRowNames)
        assertEquals(ds.inputColumnNames, restored.inputColumnNames)
        assertEquals(ds.targetColumnNames, restored.targetColumnNames)
    }

    @Test
    fun `new XML is compact - no per-cell double elements`() {
        val ds = TrainingDataset(
            inputs = MutableList(200) { MutableList(50) { 0.0 } },
            targets = MutableList(200) { MutableList(10) { 0.0 } },
        )
        val xml = getSimbrainXStream().toXML(ds)
        assertFalse(xml.contains("<double>"), "should not emit per-cell <double> elements")
        assertTrue(xml.contains("<rows>200</rows>"), "should record input rows")
        assertTrue(xml.contains("<cols>50</cols>"), "should record input cols")
        assertTrue(xml.contains("<cols>10</cols>"), "should record target cols")
    }

    @Test
    fun `empty dataset round-trips when sizes are provided`() {
        val ds = TrainingDataset(
            inputs = mutableListOf(),
            targets = mutableListOf(),
            inputSize = 12,
            targetSize = 4,
        )
        val xml = getSimbrainXStream().toXML(ds)
        val restored = getSimbrainXStream().fromXML(xml) as TrainingDataset
        assertEquals(12, restored.inputSize)
        assertEquals(4, restored.targetSize)
        assertEquals(0, restored.inputs.size)
        assertEquals(0, restored.targets.size)
    }

    @Test
    fun `legacy nested-list XML still loads`() {
        val legacy = """
            <org.simbrain.network.trainers.TrainingDataset>
              <inputs>
                <list>
                  <double>1.0</double>
                  <double>0.5</double>
                </list>
                <list>
                  <double>0.0</double>
                  <double>-0.25</double>
                </list>
              </inputs>
              <targets>
                <list>
                  <double>0.1</double>
                </list>
                <list>
                  <double>0.9</double>
                </list>
              </targets>
              <inputSize>2</inputSize>
              <targetSize>1</targetSize>
              <inputColumnNames>
                <string>a</string>
                <string>b</string>
              </inputColumnNames>
            </org.simbrain.network.trainers.TrainingDataset>
        """.trimIndent()
        val restored = getSimbrainXStream().fromXML(legacy) as TrainingDataset
        assertEquals(2, restored.inputSize)
        assertEquals(1, restored.targetSize)
        assertEquals(listOf(1.0, 0.5), restored.inputs[0])
        assertEquals(listOf(0.0, -0.25), restored.inputs[1])
        assertEquals(listOf(0.1), restored.targets[0])
        assertEquals(listOf(0.9), restored.targets[1])
        assertEquals(listOf("a", "b"), restored.inputColumnNames)
        assertNotNull(restored)
    }
}
