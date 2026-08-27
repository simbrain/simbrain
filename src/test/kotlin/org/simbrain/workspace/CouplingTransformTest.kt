/**
 * Tests for coupling transform chains and nullable message semantics: same-type and type-bridging
 * operations, filters that suppress delivery by returning null, chain type validation, the `via` DSL,
 * and persistence of a transform chain through workspace serialization.
 */
package org.simbrain.workspace

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Neuron
import org.simbrain.workspace.couplings.BroadcastOperation
import org.simbrain.workspace.couplings.CoerceInOperation
import org.simbrain.workspace.couplings.ElementOperation
import org.simbrain.workspace.couplings.ArrayToMatrixOperation
import org.simbrain.workspace.couplings.MatrixToArrayOperation
import org.simbrain.workspace.couplings.MeanOperation
import org.simbrain.workspace.couplings.OnChangeOperation
import org.simbrain.workspace.couplings.ScaleOperation
import org.simbrain.workspace.couplings.ThresholdOperation
import org.simbrain.workspace.gui.inferTargetArraySize
import org.simbrain.workspace.serialization.WorkspaceSerializer
import smile.math.matrix.Matrix
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class TransformTestContainer : AttributeContainer {
    override val id = "Transform container"

    var scalar = 0.0
    var maybe: Double? = null
    var array = doubleArrayOf(1.0, 2.0, 3.0)

    var received = 0.0
    var receivedArray = doubleArrayOf()
    var deliveries = 0

    @Producible
    fun produceScalar() = scalar

    @Producible
    fun produceMaybe(): Double? = maybe

    @Producible
    fun produceArray() = array

    @Consumable
    fun consumeScalar(value: Double) {
        received = value
        deliveries++
    }

    @Consumable
    fun consumeArray(value: DoubleArray) {
        receivedArray = value
        deliveries++
    }
}

class MatrixTestContainer : AttributeContainer {
    override val id = "Matrix container"

    var matrix: Matrix = Matrix.column(doubleArrayOf(4.0, 5.0, 6.0))
    var receivedMatrix: Matrix? = null

    @Producible
    fun produceMatrix(): Matrix = matrix

    @Consumable
    fun consumeMatrix(value: Matrix) {
        receivedMatrix = value
    }
}

class AmbiguousSizeContainer : AttributeContainer {
    override val id = "Ambiguous container"

    @Producible
    fun produceSmall() = doubleArrayOf(1.0, 2.0)

    @Producible
    fun produceLarge() = doubleArrayOf(1.0, 2.0, 3.0, 4.0)

    @Consumable
    fun consumeArray(value: DoubleArray) {
    }
}

class CouplingTransformTest {

    private val workspace = Workspace()

    private val couplingManager
        get() = workspace.couplingManager

    @Test
    fun `scale transform is applied to the produced value`() {
        val container = TransformTestContainer().apply { scalar = 3.0 }
        with(couplingManager) {
            createCoupling(
                container.getProducer("produceScalar"),
                container.getConsumer("consumeScalar"),
                transforms = listOf(ScaleOperation(2.0))
            )
        }
        runBlocking { couplingManager.updateCouplings() }
        assertEquals(6.0, container.received)
    }

    @Test
    fun `chained transforms are applied in order`() {
        val container = TransformTestContainer().apply { scalar = 3.0 }
        with(couplingManager) {
            container.getProducer("produceScalar")
                .via(ScaleOperation(2.0))
                .via(CoerceInOperation(0.0, 5.0)) couple container.getConsumer("consumeScalar")
        }
        runBlocking { couplingManager.updateCouplings() }
        assertEquals(5.0, container.received)
    }

    @Test
    fun `mean bridges an array producer to a scalar consumer`() {
        val container = TransformTestContainer()
        with(couplingManager) {
            container.getProducer("produceArray") via MeanOperation() couple container.getConsumer("consumeScalar")
        }
        runBlocking { couplingManager.updateCouplings() }
        assertEquals(2.0, container.received)
    }

    @Test
    fun `broadcast bridges a scalar producer to an array consumer`() {
        val container = TransformTestContainer().apply { scalar = 1.5 }
        with(couplingManager) {
            container.getProducer("produceScalar") via BroadcastOperation(3) couple container.getConsumer("consumeArray")
        }
        runBlocking { couplingManager.updateCouplings() }
        assertArrayEquals(doubleArrayOf(1.5, 1.5, 1.5), container.receivedArray)
    }

    @Test
    fun `element pick delivers one component and suppresses out of range indices`() {
        val container = TransformTestContainer()
        with(couplingManager) {
            container.getProducer("produceArray") via ElementOperation(1) couple container.getConsumer("consumeScalar")
        }
        runBlocking { couplingManager.updateCouplings() }
        assertEquals(2.0, container.received)
        assertEquals(1, container.deliveries)

        container.array = doubleArrayOf(9.0)
        runBlocking { couplingManager.updateCouplings() }
        assertEquals(1, container.deliveries)
    }

    @Test
    fun `threshold suppresses values below it and passes values at or above it`() {
        val container = TransformTestContainer().apply { scalar = 0.5 }
        with(couplingManager) {
            container.getProducer("produceScalar") via ThresholdOperation(1.0) couple container.getConsumer("consumeScalar")
        }
        runBlocking { couplingManager.updateCouplings() }
        assertEquals(0, container.deliveries)

        container.scalar = 1.0
        runBlocking { couplingManager.updateCouplings() }
        assertEquals(1, container.deliveries)
        assertEquals(1.0, container.received)
    }

    @Test
    fun `on change delivers only when the value differs from the last delivered one`() {
        val container = TransformTestContainer().apply { scalar = 1.0 }
        with(couplingManager) {
            container.getProducer("produceScalar") via OnChangeOperation() couple container.getConsumer("consumeScalar")
        }
        runBlocking {
            couplingManager.updateCouplings()
            couplingManager.updateCouplings()
        }
        assertEquals(1, container.deliveries)

        container.scalar = 2.0
        runBlocking { couplingManager.updateCouplings() }
        assertEquals(2, container.deliveries)
    }

    @Test
    fun `null from a producer means nothing is delivered this tick`() {
        val container = TransformTestContainer().apply { maybe = null }
        with(couplingManager) {
            container.getProducer("produceMaybe") couple container.getConsumer("consumeScalar")
        }
        runBlocking { couplingManager.updateCouplings() }
        assertEquals(0, container.deliveries)

        container.maybe = 4.25
        runBlocking { couplingManager.updateCouplings() }
        assertEquals(1, container.deliveries)
        assertEquals(4.25, container.received)
    }

    @Test
    fun `a transform whose input does not match the producer is rejected`() {
        val container = TransformTestContainer()
        val exception = assertThrows<MismatchedAttributesException> {
            with(couplingManager) {
                createCoupling(
                    container.getProducer("produceScalar"),
                    container.getConsumer("consumeScalar"),
                    transforms = listOf(MeanOperation())
                )
            }
        }
        assertEquals(true, "Mean" in exception.message!!)
    }

    @Test
    fun `a chain whose output does not match the consumer is rejected`() {
        val container = TransformTestContainer()
        assertThrows<MismatchedAttributesException> {
            with(couplingManager) {
                createCoupling(
                    container.getProducer("produceArray"),
                    container.getConsumer("consumeArray"),
                    transforms = listOf(MeanOperation())
                )
            }
        }
    }

    @Test
    fun `couplings with the same endpoints but different transforms coexist`() {
        val container = TransformTestContainer()
        with(couplingManager) {
            container.getProducer("produceScalar") couple container.getConsumer("consumeScalar")
            container.getProducer("produceScalar") via ScaleOperation(2.0) couple container.getConsumer("consumeScalar")
        }
        assertEquals(2, couplingManager.couplings.size)
    }

    @Test
    fun `setTransforms replaces the chain in place and preserves update order`() {
        val container = TransformTestContainer().apply { scalar = 2.0 }
        with(couplingManager) {
            val first = container.getProducer("produceScalar") couple container.getConsumer("consumeScalar")
            val second = container.getProducer("produceArray") couple container.getConsumer("consumeArray")
            val replaced = setTransforms(first, listOf(ScaleOperation(3.0)))
            assertEquals(listOf(replaced, second), couplings)
        }
        runBlocking { couplingManager.updateCouplings() }
        assertEquals(6.0, container.received)
    }

    @Test
    fun `setTransforms rejects a chain that does not fit the endpoints`() {
        val container = TransformTestContainer().apply { scalar = 1.0 }
        val coupling = with(couplingManager) {
            container.getProducer("produceScalar") couple container.getConsumer("consumeScalar")
        }
        assertThrows<MismatchedAttributesException> {
            couplingManager.setTransforms(coupling, listOf(MeanOperation()))
        }
        runBlocking { couplingManager.updateCouplings() }
        assertEquals(1.0, container.received)
    }

    @Test
    fun `array producers can feed matrix consumers through the cast`() {
        val source = TransformTestContainer()
        val target = MatrixTestContainer()
        with(couplingManager) {
            source.getProducer("produceArray") via ArrayToMatrixOperation() couple target.getConsumer("consumeMatrix")
        }
        runBlocking { couplingManager.updateCouplings() }
        val received = target.receivedMatrix!!
        assertEquals(3, received.nrow())
        assertEquals(1, received.ncol())
        assertEquals(2.0, received.get(1, 0))
    }

    @Test
    fun `matrix producers can feed array consumers through the cast`() {
        val source = MatrixTestContainer()
        val target = TransformTestContainer()
        with(couplingManager) {
            source.getProducer("produceMatrix") via MatrixToArrayOperation() couple target.getConsumer("consumeArray")
        }
        runBlocking { couplingManager.updateCouplings() }
        assertArrayEquals(doubleArrayOf(4.0, 5.0, 6.0), target.receivedArray)
    }

    @Test
    fun `target array size is inferred from the consumer container's producers`() {
        val container = TransformTestContainer()
        val consumer = with(couplingManager) { container.getConsumer("consumeScalar") }
        assertEquals(3, inferTargetArraySize(workspace, consumer))

        val matrixContainer = MatrixTestContainer()
        val matrixConsumer = with(couplingManager) { matrixContainer.getConsumer("consumeMatrix") }
        assertEquals(3, inferTargetArraySize(workspace, matrixConsumer))

        val ambiguous = AmbiguousSizeContainer()
        val ambiguousConsumer = with(couplingManager) { ambiguous.getConsumer("consumeArray") }
        assertEquals(null, inferTargetArraySize(workspace, ambiguousConsumer))
    }

    @Test
    fun `tryGetValueNow samples plain producers and skips suspending ones`() {
        val plain = TransformTestContainer()
        val suspending = SuspendAttributeContainer()
        with(couplingManager) {
            assertEquals(3, (plain.getProducer("produceArray").tryGetValueNow() as DoubleArray).size)
            assertEquals(null, suspending.getProducer("produceDouble").tryGetValueNow())
        }
    }

    @Test
    fun `operation display labels fold in parameter values`() {
        assertEquals("Scale ×0.5", ScaleOperation(0.5).displayLabel)
        assertEquals("Scale ×2", ScaleOperation(2.0).displayLabel)
        assertEquals("Coerce in [0, 1]", CoerceInOperation(0.0, 1.0).displayLabel)
        assertEquals("Threshold ≥ 1.5", ThresholdOperation(1.5).displayLabel)
        assertEquals("Element [3]", ElementOperation(3).displayLabel)
        assertEquals("Mean", MeanOperation().displayLabel)
    }

    @Test
    fun `coupling descriptions show the chain with parameter values`() {
        val container = TransformTestContainer()
        val coupling = with(couplingManager) {
            container.getProducer("produceScalar") via ScaleOperation(0.5) couple container.getConsumer("consumeScalar")
        }
        assertEquals(true, "Scale ×0.5" in coupling.description) { coupling.description }
    }

    @Test
    fun `transform chain survives workspace serialization`() {
        val networkComponent = NetworkComponent("Net")
        workspace.addWorkspaceComponent(networkComponent)
        val source = Neuron()
        val target = Neuron()
        networkComponent.network.addNetworkModelAsync(source)
        networkComponent.network.addNetworkModelAsync(target)
        with(couplingManager) {
            source.getProducer("getActivation") via ScaleOperation(2.5) couple target.getConsumer("setActivation")
        }

        val serializer = WorkspaceSerializer(workspace)
        val bas = ByteArrayOutputStream()
        serializer.serialize(bas, true)
        workspace.clearWorkspace()

        runBlocking { serializer.deserialize(ByteArrayInputStream(bas.toByteArray())) }

        val coupling = workspace.couplingManager.couplings.single()
        val scale = coupling.transforms.single() as ScaleOperation
        assertEquals(2.5, scale.factor)

        val restoredComponent = workspace.getComponent("Net") as NetworkComponent
        val neurons = restoredComponent.network.getModels<Neuron>().toList()
        neurons[0].activation = 0.2
        runBlocking { workspace.couplingManager.updateCouplings() }
        assertEquals(0.5, neurons[1].activation)
    }
}
