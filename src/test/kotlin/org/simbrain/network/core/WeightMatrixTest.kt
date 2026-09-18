package org.simbrain.network.core

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import smile.math.matrix.Matrix

class WeightMatrixTest {

    @Test
    fun `psr matrix keeps the weight shape when a transposed matrix is assigned`() {
        val wm = WeightMatrix(NeuronArray(3), NeuronArray(2))
        wm.psrMatrix = Matrix(3, 2)
        assertEquals(2, wm.psrMatrix.nrow())
        assertEquals(3, wm.psrMatrix.ncol())
    }

    @Test
    fun `network with a transposed saved psr matrix loads and updates`() {
        val net = Network()
        val source = NeuronArray(3)
        val target = NeuronArray(2)
        net.addNetworkModelsAsync(source, target, WeightMatrix(source, target))
        // Older builds wrote the psr matrix transposed after the weight matrix dialog was committed
        val xml = getNetworkXStream().toXML(net).replace(
            Regex("(<psrMatrix[^>]*>\\s*<rows>)2(</rows>\\s*<cols>)3(</cols>)"), "$13$22$3"
        )
        val loaded = getNetworkXStream().fromXML(xml) as Network
        val wm = loaded.getModels<WeightMatrix>().first()
        assertEquals(2, wm.psrMatrix.nrow())
        assertEquals(3, wm.psrMatrix.ncol())
        runBlocking { loaded.update() }
    }
}
