package org.simbrain.network.core

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.simbrain.network.spikeresponders.ShortTermPlasticity
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.subnetworks.SRNNetwork
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.updaterules.IzhikevichRule
import org.simbrain.util.point
import java.util.List

class NetworkTest {
    private val net: Network
    private val n1: Neuron
    private val n2: Neuron
    private val s1: Synapse
    private val ng1: NeuronCollection
    private val ng2: NeuronCollection
    private val na1: NeuronArray
    private val na2: NeuronArray
    private val nc1: NeuronCollection
    private val wm1: WeightMatrix
    private val sg1: SynapseGroup
    private val ng3: NeuronCollection
    private val ng4: NeuronCollection
    private val ng5: NeuronCollection
    private val ng6: NeuronCollection

    // TODO: Other subneworks
    private val bp: BackpropNetwork?
    private val srn: SRNNetwork?


    init {
        net = Network()

        n1 = Neuron()
        n1.label = "neuron1"
        n1.bias = 1.0
        net.addNetworkModelAsync(n1)
        n2 = Neuron().apply {
            updateRule = IzhikevichRule()
        }
        n2.label = "neuron2"
        net.addNetworkModelAsync(n2)

        s1 = Synapse(n1, n2).apply {
            spikeResponder = ShortTermPlasticity()
        }
        net.addNetworkModelAsync(s1)

        nc1 = NeuronCollection(List.of(n1, n2))
        net.addNetworkModelAsync(nc1)

        val ng1Neurons = List(10) { Neuron() }
        ng1Neurons.forEach { net.addNetworkModelAsync(it) }
        ng1 = NeuronCollection(ng1Neurons).apply { label = "neuron_group_1" }
        net.addNetworkModelAsync(ng1)
        val ng2Neurons = List(10) { Neuron() }
        ng2Neurons.forEach { net.addNetworkModelAsync(it) }
        ng2 = NeuronCollection(ng2Neurons).apply { label = "ng2" }
        net.addNetworkModelAsync(ng2)

        sg1 = SynapseGroup(ng1, ng2)
        net.addNetworkModelAsync(sg1)

        na1 = NeuronArray(10)
        na2 = NeuronArray(10)
        wm1 = WeightMatrix(na1, na2)
        net.addNetworkModelsAsync(List.of(na1, na2, wm1))

        val ng3Neurons = List(5) { Neuron() }
        ng3Neurons.forEach { net.addNetworkModelAsync(it) }
        ng3 = NeuronCollection(ng3Neurons).apply { label = "ng3" }
        val ng4Neurons = List(5) { Neuron() }
        ng4Neurons.forEach { net.addNetworkModelAsync(it) }
        ng4 = NeuronCollection(ng4Neurons).apply { label = "ng4" }
        val ng5Neurons = List(5) { Neuron() }
        ng5Neurons.forEach { net.addNetworkModelAsync(it) }
        ng5 = NeuronCollection(ng5Neurons).apply { label = "ng5" }
        val ng6Neurons = List(5) { Neuron() }
        ng6Neurons.forEach { net.addNetworkModelAsync(it) }
        ng6 = NeuronCollection(ng6Neurons).apply { label = "ng6" }

        net.addNetworkModelsAsync(ng3, ng4, ng5, ng6)

        bp = BackpropNetwork(intArrayOf(3, 5, 4), point(0, 0))
        bp.label = "backprop"
        srn = SRNNetwork(5, 5, 5, point(0, 0))
        srn.label = "srn"
        net.addNetworkModelsAsync(bp, srn)
    }

    @Test
    fun testDeleteObjects() = runBlocking {
        // Neurons and Synapses
        val initialNeuronCount = net.getModels(Neuron::class.java).size // 42

        n1.delete()
        Assertions.assertEquals(initialNeuronCount - 1, net.getModels(Neuron::class.java).size)
        // Deleting the neuron should also delete the synapse
        Assertions.assertEquals(0, net.getModels(Synapse::class.java).size)

        // Deleting all neurons in nc1 should delete the neuron collection
        Assertions.assertEquals(7, net.getModels(NeuronCollection::class.java).size) // nc1 + ng1..ng6
        n2.delete()
        Assertions.assertEquals(6, net.getModels(NeuronCollection::class.java).size) // ng1..ng6

        // Neuron Collections and Synapse Groups
        ng1.delete()
        Assertions.assertEquals(5, net.getModels(NeuronCollection::class.java).size) // ng2 + ng3..ng6
        // Deleting the neuron collection should also delete the synapse group
        Assertions.assertEquals(0, net.getModels(SynapseGroup::class.java).size)

        // Neuron Arrays and WeightMatrices
        na1.delete()
        Assertions.assertEquals(1, net.getModels(NeuronArray::class.java).size)
        // Deleting the neuron array should also delete the weight matrix
        Assertions.assertEquals(0, net.getModels(WeightMatrix::class.java).size)

        // Additional neuron collections and subnets
        Assertions.assertEquals(2, net.getModels(Subnetwork::class.java).size)
        ng3.delete()
        bp!!.delete()
        srn!!.delete()
        Assertions.assertEquals(4, net.getModels(NeuronCollection::class.java).size) // ng2 + ng4..ng6
        Assertions.assertEquals(0, net.getModels(Subnetwork::class.java).size)
    }

    @Test
    fun getByLabel() {
        Assertions.assertEquals(n1, net.getModelByLabel(Neuron::class.java, "neuron1"))
        Assertions.assertEquals(n2, net.getModelByLabel(Neuron::class.java, "neuron2"))
        Assertions.assertEquals(ng1, net.getModelByLabel(NeuronCollection::class.java, "neuron_group_1"))
        Assertions.assertEquals(ng2, net.getModelByLabel(NeuronCollection::class.java, "ng2"))
    }

    @Test
    fun testXML() {
        val xmlRep = getNetworkXStream().toXML(net)

        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network

        val n1 = fromXml.getModelByLabel(Neuron::class.java, "neuron1")
        Assertions.assertNotNull(n1)
        Assertions.assertEquals(1.0, n1.bias)
        Assertions.assertNotNull(fromXml.getModelByLabel(Neuron::class.java, "neuron2"))
        Assertions.assertNotNull(fromXml.getModelByLabel(NeuronCollection::class.java, "neuron_group_1"))
        Assertions.assertNotNull(fromXml.getModelByLabel(NeuronCollection::class.java, "ng2"))
        Assertions.assertNotNull(fromXml.getModelByLabel(NeuronCollection::class.java, "ng3"))
        Assertions.assertNotNull(fromXml.getModelByLabel(NeuronCollection::class.java, "ng4"))
        Assertions.assertNotNull(fromXml.getModelByLabel(NeuronCollection::class.java, "ng5"))
        Assertions.assertNotNull(fromXml.getModelByLabel(NeuronCollection::class.java, "ng6"))
        Assertions.assertNotNull(fromXml.getModelByLabel(BackpropNetwork::class.java, "backprop"))
        Assertions.assertNotNull(fromXml.getModelByLabel(SRNNetwork::class.java, "srn"))
    }

    @Test
    fun testSynapseCounts() {
        // 1 free synapse

        Assertions.assertEquals(1, net.freeSynapses.size)

        // 1 free synapse + 100 in the synapseGroup = 101
        Assertions.assertEquals(101, net.flatSynapseList.size)
    }

    @Test
    fun testNeuronCounts() {
        // All NeuronCollection neurons are free neurons in the network
        // 2 (n1, n2) + 10 (ng1) + 10 (ng2) + 5*4 (ng3-ng6) = 42
        Assertions.assertEquals(42, net.freeNeurons.size)

        // flatNeuronList includes free neurons + neurons from NeuronCollections (which are the same)
        // so no double counting. 42 unique neurons.
        Assertions.assertEquals(42, net.flatNeuronList.size)
    }

    @Test
    fun testFlatListsComprehensive() {
        // All NeuronCollection neurons are free neurons, so flatNeuronList = freeNeurons + subnetwork neurons
        // Since we have no subnetwork NeuronCollections here (BackpropNetwork/SRNNetwork use NeuronArray),
        // flatNeuronList should equal freeNeurons
        val expectedNeuronCount = net.freeNeurons.size // 42

        Assertions.assertEquals(expectedNeuronCount, net.flatNeuronList.size,
            "flatNeuronList should include all neurons")
        
        // Test flatSynapseList - manually count all synapses from all sources
        var expectedSynapseCount = 0
        
        // Free synapses (top-level)
        expectedSynapseCount += net.freeSynapses.size // 1 synapse (s1)
        
        // Synapses from synapse groups
        expectedSynapseCount += sg1.synapses.size // Should be ng1.size * ng2.size = 100
        
        // Synapses from weight matrices (connecting NeuronArrays)
        // Weight matrices don't contain individual Synapse objects (they're matrix-based), 
        // so they don't contribute to flatSynapseList
        
        // Synapses from subnetworks (BackpropNetwork and SRNNetwork use WeightMatrix objects which don't contain 
        // individual Synapse objects, so they don't contribute to flatSynapseList)
        
        Assertions.assertEquals(expectedSynapseCount, net.flatSynapseList.size,
            "flatSynapseList should include all synapses from free synapses, synapse groups, weight matrices, and subnetworks")
        
        // Verify that free neurons are actually included in flat list
        Assertions.assertTrue(net.flatNeuronList.containsAll(net.freeNeurons), 
            "flatNeuronList should contain all free neurons")
        
        // Verify that free synapses are actually included in flat list
        Assertions.assertTrue(net.flatSynapseList.containsAll(net.freeSynapses),
            "flatSynapseList should contain all free synapses")
        
        // Verify that neuron group neurons are included in flat list
        Assertions.assertTrue(net.flatNeuronList.containsAll(ng1.neuronList),
            "flatNeuronList should contain all neurons from neuron groups")
        Assertions.assertTrue(net.flatNeuronList.containsAll(ng2.neuronList),
            "flatNeuronList should contain all neurons from neuron groups")
        
        // Verify that additional neuron groups are also captured
        Assertions.assertTrue(net.flatNeuronList.containsAll(ng3.neuronList),
            "flatNeuronList should contain all neurons from ng3")
        Assertions.assertTrue(net.flatNeuronList.containsAll(ng4.neuronList),
            "flatNeuronList should contain all neurons from ng4")
        Assertions.assertTrue(net.flatNeuronList.containsAll(ng5.neuronList),
            "flatNeuronList should contain all neurons from ng5")
        Assertions.assertTrue(net.flatNeuronList.containsAll(ng6.neuronList),
            "flatNeuronList should contain all neurons from ng6")
        
        // Verify that synapse group synapses are included in flat list
        Assertions.assertTrue(net.flatSynapseList.containsAll(sg1.synapses),
            "flatSynapseList should contain all synapses from synapse groups")
        
        // Weight matrices don't contain individual synapse objects, so no verification needed
    }

    @Test
    fun `deleting a subset of a collection's neurons keeps surviving siblings mapped`() = runBlocking {
        val testNet = Network()
        val neurons = (0 until 4).map { Neuron() }
        neurons.forEach { testNet.addNetworkModel(it) }
        val collection = NeuronCollection(neurons)
        testNet.addNetworkModel(collection)

        testNet.deleteModels(neurons.take(2))

        // The map-clearing in deleteModels only fires when the parent is deleted; a partial delete must
        // leave surviving siblings mapped (an undo snapshot taken later reads this map).
        Assertions.assertEquals(collection, testNet.childToParentMap[neurons[2]],
            "a surviving sibling must stay mapped to its collection")
        Assertions.assertEquals(collection, testNet.childToParentMap[neurons[3]],
            "a surviving sibling must stay mapped to its collection")
        Assertions.assertFalse(testNet.childToParentMap.containsKey(neurons[0]),
            "the deleted neuron must be removed from the map")
        Assertions.assertFalse(testNet.childToParentMap.containsKey(neurons[1]),
            "the deleted neuron must be removed from the map")
    }
}