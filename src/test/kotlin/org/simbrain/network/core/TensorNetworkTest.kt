package org.simbrain.network.core

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TensorNetworkTest {

    @Test
    fun `full pipeline update cycle`() {
        val net = Network()

        // Input -> Conv -> Pool
        val input = Tensor(TensorShape(4, 4, 1))
        val convOutShape = input.shape.convOutputShape(3, 1, Padding.SAME, 2)
        val convOut = Tensor(convOutShape).apply {
            activationFunction = TensorActivation.RELU
        }
        val poolOutShape = convOutShape.poolOutputShape(2, 2)
        val poolOut = Tensor(poolOutShape)

        val conv = ConvolutionConnector(input, convOut, kernelSize = 3, numFilters = 2, stride = 1, padding = Padding.SAME)
        val pool = PoolingConnector(convOut, poolOut, poolSize = 2, stride = 2, poolingType = PoolingType.MAX)

        // Add to network
        net.addNetworkModelAsync(input, usePlacementManager = false)
        net.addNetworkModelAsync(convOut, usePlacementManager = false)
        net.addNetworkModelAsync(poolOut, usePlacementManager = false)
        net.addNetworkModelAsync(conv, usePlacementManager = false)
        net.addNetworkModelAsync(pool, usePlacementManager = false)

        // Set input activations
        input.isClamped = true
        input.activations.fill(1.0)

        // Run buffered update cycle
        // Tick 1: input stays clamped, conv accumulates from input and updates
        net.update()
        // Tick 2: pool accumulates from convOut and updates
        net.update()

        // After 2 ticks, pool should have non-zero activations
        assertTrue(poolOut.activations.any { it != 0.0 },
            "Pool output should have non-zero activations after pipeline runs")
    }

    @Test
    fun `tensors appear in update order`() {
        val inputTensor = Tensor(TensorShape(4, 4, 1))
        val convOutShape = inputTensor.shape.convOutputShape(3, 1, Padding.SAME, 1)
        val convOut = Tensor(convOutShape)
        val conv = ConvolutionConnector(inputTensor, convOut, kernelSize = 3, numFilters = 1, stride = 1, padding = Padding.SAME)

        // Tensor should come before TensorConnector in update order
        assertTrue(updatingOrder(inputTensor) < updatingOrder(conv))
        assertTrue(updatingOrder(convOut) < updatingOrder(conv))
    }

    @Test
    fun `delete tensor cascades to connectors`() {
        val source = Tensor(TensorShape(4, 4, 1))
        val outputShape = source.shape.convOutputShape(3, 1, Padding.SAME, 2)
        val target = Tensor(outputShape)
        val conv = ConvolutionConnector(source, target, kernelSize = 3, numFilters = 2, stride = 1, padding = Padding.SAME)

        val net = Network()
        net.addNetworkModelAsync(source, usePlacementManager = false)
        net.addNetworkModelAsync(target, usePlacementManager = false)
        net.addNetworkModelAsync(conv, usePlacementManager = false)

        runBlocking { source.delete() }

        // Connector should be cleaned up
        assertEquals(0, target.incomingTensorConnectors.size)
    }

    @Test
    fun `multiple incoming connectors sum additively`() {
        val net = Network()

        val src1 = Tensor(TensorShape(2, 2, 1))
        val src2 = Tensor(TensorShape(2, 2, 1))
        val target = Tensor(TensorShape(2, 2, 1))

        // Use 1x1 conv (identity-like) to just pass values through
        val conn1 = ConvolutionConnector(src1, target, kernelSize = 1, numFilters = 1, stride = 1, padding = Padding.VALID)
        val conn2 = ConvolutionConnector(src2, target, kernelSize = 1, numFilters = 1, stride = 1, padding = Padding.VALID)

        // Set all kernel weights to 1, biases to 0
        conn1.kernels.fill(1.0)
        conn1.filterBiases.fill(0.0)
        conn2.kernels.fill(1.0)
        conn2.filterBiases.fill(0.0)

        src1.isClamped = true
        src1.activations.fill(3.0)
        src2.isClamped = true
        src2.activations.fill(5.0)

        target.activationFunction = TensorActivation.IDENTITY

        // Accumulate inputs from both connectors
        with(net) { target.accumulateInputs() }
        with(net) { target.update() }

        // Each output should be 3 + 5 = 8 (both connectors contribute additively)
        assertTrue(target.activations.all { it == 8.0 },
            "Multiple connectors should sum: expected 8.0, got ${target.activations[0]}")
    }

    @Test
    fun `CNN pipeline survives serialization round-trip`() {
        val net = Network()

        // Build pipeline: Input(4x4x2) -> Conv(3x3, 3 filters, SAME) -> ReLU -> Pool(2x2) -> MaxPool
        val input = Tensor(TensorShape(4, 4, 2)).apply {
            label = "input"
            isClamped = true
        }
        val convOutShape = input.shape.convOutputShape(3, 1, Padding.SAME, 3)
        val convOut = Tensor(convOutShape).apply {
            label = "convOut"
            activationFunction = TensorActivation.RELU
        }
        val poolOutShape = convOutShape.poolOutputShape(2, 2)
        val poolOut = Tensor(poolOutShape).apply {
            label = "poolOut"
        }

        val conv = ConvolutionConnector(input, convOut, kernelSize = 3, numFilters = 3, stride = 1, padding = Padding.SAME)
        val pool = PoolingConnector(convOut, poolOut, poolSize = 2, stride = 2, poolingType = PoolingType.MAX)

        // Set known kernel weights so we can verify after round-trip
        conv.kernels.fill(0.5)
        conv.filterBiases.fill(0.1)

        net.addNetworkModelAsync(input, usePlacementManager = false)
        net.addNetworkModelAsync(convOut, usePlacementManager = false)
        net.addNetworkModelAsync(poolOut, usePlacementManager = false)
        net.addNetworkModelAsync(conv, usePlacementManager = false)
        net.addNetworkModelAsync(pool, usePlacementManager = false)

        // Serialize and deserialize
        val xstream = getNetworkXStream()
        val xml = xstream.toXML(net)
        val restored = xstream.fromXML(xml) as Network

        // Verify tensors survived
        val restoredInput = restored.getModelByLabel(Tensor::class.java, "input")
        val restoredConvOut = restored.getModelByLabel(Tensor::class.java, "convOut")
        val restoredPoolOut = restored.getModelByLabel(Tensor::class.java, "poolOut")
        assertNotNull(restoredInput, "Input tensor should survive serialization")
        assertNotNull(restoredConvOut, "Conv output tensor should survive serialization")
        assertNotNull(restoredPoolOut, "Pool output tensor should survive serialization")

        // Verify shapes preserved
        assertEquals(TensorShape(4, 4, 2), restoredInput!!.shape)
        assertEquals(convOutShape, restoredConvOut!!.shape)
        assertEquals(poolOutShape, restoredPoolOut!!.shape)

        // Verify tensor properties preserved
        assertTrue(restoredInput.isClamped)
        assertEquals(TensorActivation.RELU, restoredConvOut.activationFunction)

        // Verify connectors survived with correct parameters
        val restoredConv = restored.getModels<ConvolutionConnector>().firstOrNull()
        val restoredPool = restored.getModels<PoolingConnector>().firstOrNull()
        assertNotNull(restoredConv, "ConvolutionConnector should survive serialization")
        assertNotNull(restoredPool, "PoolingConnector should survive serialization")

        assertEquals(3, restoredConv!!.kernelSize)
        assertEquals(3, restoredConv.numFilters)
        assertEquals(Padding.SAME, restoredConv.padding)
        assertEquals(2, restoredPool!!.poolSize)
        assertEquals(PoolingType.MAX, restoredPool.poolingType)

        // Verify kernel weights preserved
        assertTrue(restoredConv.kernels.all { it == 0.5 },
            "Kernel weights should survive serialization")
        assertTrue(restoredConv.filterBiases.all { it == 0.1 },
            "Filter biases should survive serialization")

        // Verify connector wiring re-established (via Network.readResolve + afterRestore)
        assertEquals(restoredInput, restoredConv.source)
        assertEquals(restoredConvOut, restoredConv.target)
        assertEquals(restoredConvOut, restoredPool.source)
        assertEquals(restoredPoolOut, restoredPool.target)

        // Verify connector lists re-wired on tensors
        assertTrue(restoredInput.outgoingTensorConnectors.contains(restoredConv),
            "Input should have conv as outgoing connector after deserialization")
        assertTrue(restoredConvOut.incomingTensorConnectors.contains(restoredConv),
            "ConvOut should have conv as incoming connector after deserialization")
        assertTrue(restoredConvOut.outgoingTensorConnectors.contains(restoredPool),
            "ConvOut should have pool as outgoing connector after deserialization")
        assertTrue(restoredPoolOut.incomingTensorConnectors.contains(restoredPool),
            "PoolOut should have pool as incoming connector after deserialization")

        // Verify the restored network can still run
        restoredInput.activations.fill(1.0)
        restored.update()
        restored.update()
        assertTrue(restoredPoolOut.activations.any { it != 0.0 },
            "Restored pipeline should produce non-zero output after update")
    }

    @Test
    fun `FlattenConnector survives serialization round-trip`() {
        val net = Network()

        // Tensor(4x4x2) -> Flatten -> NeuronArray(32)
        val tensor = Tensor(TensorShape(4, 4, 2)).apply {
            label = "source"
            isClamped = true
        }
        val array = NeuronArray(32).apply {
            label = "target"
        }
        val flatten = FlattenConnector(tensor, array)

        net.addNetworkModelAsync(tensor, usePlacementManager = false)
        net.addNetworkModelAsync(array, usePlacementManager = false)
        net.addNetworkModelAsync(flatten, usePlacementManager = false)

        // Serialize and deserialize
        val xstream = getNetworkXStream()
        val xml = xstream.toXML(net)
        val restored = xstream.fromXML(xml) as Network

        // Verify models survived
        val restoredTensor = restored.getModelByLabel(Tensor::class.java, "source")
        val restoredArray = restored.getModelByLabel(NeuronArray::class.java, "target")
        val restoredFlatten = restored.getModels<FlattenConnector>().firstOrNull()
        assertNotNull(restoredTensor, "Source tensor should survive serialization")
        assertNotNull(restoredArray, "Target NeuronArray should survive serialization")
        assertNotNull(restoredFlatten, "FlattenConnector should survive serialization")

        // Verify wiring
        assertEquals(restoredTensor, restoredFlatten!!.source)
        assertEquals(restoredArray, restoredFlatten.target)
        assertTrue(restoredTensor!!.outgoingFlattenConnectors.contains(restoredFlatten),
            "Tensor should have flatten as outgoing connector after deserialization")
        assertTrue(restoredArray!!.incomingFlattenConnectors.contains(restoredFlatten),
            "NeuronArray should have flatten as incoming connector after deserialization")

        // Verify data flows through the restored pipeline
        restoredTensor.activations.fill(0.5)
        restored.update()  // Tensor clamped -> activations stay, flatten propagates
        restored.update()  // NeuronArray accumulates + updates

        val expected = 0.5
        assertTrue(restoredArray.activationArray.any { Math.abs(it - expected) < 1e-9 },
            "Flattened activations should propagate through restored pipeline")
    }

    @Test
    fun `FlattenConnector deletion cascades properly`() = runBlocking {
        val net = Network()

        val tensor = Tensor(TensorShape(2, 2, 1))
        val array = NeuronArray(4)
        val flatten = FlattenConnector(tensor, array)

        net.addNetworkModelAsync(tensor, usePlacementManager = false)
        net.addNetworkModelAsync(array, usePlacementManager = false)
        net.addNetworkModelAsync(flatten, usePlacementManager = false)

        // Deleting the source tensor should cascade to the FlattenConnector
        assertEquals(1, tensor.outgoingFlattenConnectors.size)
        assertEquals(1, array.incomingFlattenConnectors.size)

        net.deleteModels(listOf(tensor))

        assertTrue(net.getModels<FlattenConnector>().isEmpty(),
            "FlattenConnector should be deleted when source Tensor is deleted")
        assertTrue(array.incomingFlattenConnectors.isEmpty(),
            "NeuronArray should have no incoming flatten connectors after cascade delete")
    }
}
