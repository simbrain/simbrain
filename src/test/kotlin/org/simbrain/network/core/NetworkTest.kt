package org.simbrain.network.core

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.simbrain.network.neurongroups.*
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
    private val ng1: NeuronGroup
    private val ng2: NeuronGroup
    private val na1: NeuronArray
    private val na2: NeuronArray
    private val nc1: NeuronCollection
    private val wm1: WeightMatrix
    private val sg1: SynapseGroup
    private val softmax: SoftmaxGroup
    private val som: SOMGroup
    private val wta: WinnerTakeAll
    private val competitive: CompetitiveGroup

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

        ng1 = NeuronGroup(10)
        ng1.label = "neuron_group_1"
        net.addNetworkModelAsync(ng1)
        ng2 = NeuronGroup(10)
        ng2.label = "ng2"
        net.addNetworkModelAsync(ng2)

        sg1 = SynapseGroup(ng1, ng2)
        net.addNetworkModelAsync(sg1)

        na1 = NeuronArray(10)
        na2 = NeuronArray(10)
        wm1 = WeightMatrix(na1, na2)
        net.addNetworkModelsAsync(List.of(na1, na2, wm1))

        softmax = SoftmaxGroup(5)
        softmax.label = "softmax"
        som = SOMGroup(5)
        som.label = "som"
        competitive = CompetitiveGroup(5)
        competitive.label = "competitive"
        wta = WinnerTakeAll(net, 5)
        wta.label = "wta"

        net.addNetworkModelsAsync(softmax, som, competitive, wta)

        bp = BackpropNetwork(intArrayOf(3, 5, 4), point(0, 0))
        bp.label = "backprop"
        srn = SRNNetwork(5, 5, 5, point(0, 0))
        srn.label = "srn"
        net.addNetworkModelsAsync(bp, srn)
    }

    @Test
    fun testDeleteObjects() = runBlocking {
        // Neurons and Synapses

        n1.delete()
        Assertions.assertEquals(1, net.getModels(Neuron::class.java).size)
        // Deleting the neuron should also delete the synapse
        Assertions.assertEquals(0, net.getModels(Synapse::class.java).size)

        // Deleting all neurons should delete the neuron collection
        Assertions.assertEquals(1, net.getModels(NeuronCollection::class.java).size)
        n2.delete()
        Assertions.assertEquals(0, net.getModels(NeuronCollection::class.java).size)

        // Neuron Groups and Synapse Groups
        ng1.delete()
        Assertions.assertEquals(1, net.getModels(NeuronGroup::class.java).size)
        // Deleting the neuron group should also delete the synapse group
        Assertions.assertEquals(0, net.getModels(SynapseGroup::class.java).size)

        // Neuron Arrays and WeightMatrices
        na1.delete()
        Assertions.assertEquals(1, net.getModels(NeuronArray::class.java).size)
        // Deleting the neuron group should also delete the synapse group
        Assertions.assertEquals(0, net.getModels(WeightMatrix::class.java).size)

        // Subnets and custom groups
        Assertions.assertEquals(1, net.getModels(SoftmaxGroup::class.java).size)
        // TODO: getModels(BackpropNetwork.class) fails, because of how the
        //  NetworkModelList is created (see NetworkModel.add). But changing that
        //  breaks things and we don't yet have use cases for getmodels on specific subnets
        Assertions.assertEquals(2, net.getModels(Subnetwork::class.java).size)
        softmax.delete()
        bp!!.delete()
        srn!!.delete()
        Assertions.assertEquals(0, net.getModels(SoftmaxGroup::class.java).size)
        Assertions.assertEquals(0, net.getModels(Subnetwork::class.java).size)
    }

    @Test
    fun getByLabel() {
        Assertions.assertEquals(n1, net.getModelByLabel(Neuron::class.java, "neuron1"))
        Assertions.assertEquals(n2, net.getModelByLabel(Neuron::class.java, "neuron2"))
        Assertions.assertEquals(ng1, net.getModelByLabel(NeuronGroup::class.java, "neuron_group_1"))
        Assertions.assertEquals(ng2, net.getModelByLabel(NeuronGroup::class.java, "ng2"))
    }

    @Test
    fun testXML() {
        val xmlRep = getNetworkXStream().toXML(net)

        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network

        val n1 = fromXml.getModelByLabel(Neuron::class.java, "neuron1")
        Assertions.assertNotNull(n1)
        Assertions.assertEquals(1.0, n1.bias)
        Assertions.assertNotNull(fromXml.getModelByLabel(Neuron::class.java, "neuron2"))
        Assertions.assertNotNull(fromXml.getModelByLabel(NeuronGroup::class.java, "neuron_group_1"))
        Assertions.assertNotNull(fromXml.getModelByLabel(NeuronGroup::class.java, "ng2"))
        Assertions.assertNotNull(fromXml.getModelByLabel(SoftmaxGroup::class.java, "softmax"))
        Assertions.assertNotNull(fromXml.getModelByLabel(SOMGroup::class.java, "som"))
        Assertions.assertNotNull(fromXml.getModelByLabel(CompetitiveGroup::class.java, "competitive"))
        Assertions.assertNotNull(fromXml.getModelByLabel(WinnerTakeAll::class.java, "wta"))
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
        // 2 free neurons
        Assertions.assertEquals(2, net.freeNeurons.size)

        // Calculate expected total:
        // 2 free neurons + 
        // 2 x 10 in each of two neuron groups (ng1, ng2) = 20 +
        // 4 x 5 in each of four special neuron groups (softmax, som, competitive, wta) = 20
        // Total = 2 + 20 + 20 = 42
        // (Note: the 2 in neuron collection are the same as the free neurons)
        Assertions.assertEquals(42, net.flatNeuronList.size)
    }

    @Test
    fun testFlatListsComprehensive() {
        // Test flatNeuronList - manually count all neurons from all sources
        var expectedNeuronCount = 0
        
        // Free neurons (top-level)
        expectedNeuronCount += net.freeNeurons.size // 2 neurons (n1, n2)
        
        // Neurons from neuron groups (NeuronArray are matrix-based, not collections of Neuron objects)
        expectedNeuronCount += ng1.neuronList.size // 10 neurons
        expectedNeuronCount += ng2.neuronList.size // 10 neurons
        
        // Neurons from special neuron groups
        expectedNeuronCount += softmax.neuronList.size // 5 neurons
        expectedNeuronCount += som.neuronList.size // 5 neurons
        expectedNeuronCount += competitive.neuronList.size // 5 neurons
        expectedNeuronCount += wta.neuronList.size // 5 neurons
        
        // Neurons from subnetworks (BackpropNetwork and SRNNetwork use NeuronArray objects which are matrix-based, 
        // not collections of individual Neuron objects, so they don't contribute to flatNeuronList)
        
        Assertions.assertEquals(expectedNeuronCount, net.flatNeuronList.size,
            "flatNeuronList should include all neurons from free neurons, neuron groups, neuron arrays, and subnetworks")
        
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
        
        // Verify that special neuron group subtypes are also captured (this was a previous bug)
        Assertions.assertTrue(net.flatNeuronList.containsAll(softmax.neuronList),
            "flatNeuronList should contain all neurons from SoftmaxGroup (NeuronGroup subtype)")
        Assertions.assertTrue(net.flatNeuronList.containsAll(som.neuronList),
            "flatNeuronList should contain all neurons from SOMGroup (NeuronGroup subtype)")
        Assertions.assertTrue(net.flatNeuronList.containsAll(competitive.neuronList),
            "flatNeuronList should contain all neurons from CompetitiveGroup (NeuronGroup subtype)")
        Assertions.assertTrue(net.flatNeuronList.containsAll(wta.neuronList),
            "flatNeuronList should contain all neurons from WinnerTakeAll (NeuronGroup subtype)")
        
        // Verify that synapse group synapses are included in flat list
        Assertions.assertTrue(net.flatSynapseList.containsAll(sg1.synapses),
            "flatSynapseList should contain all synapses from synapse groups")
        
        // Weight matrices don't contain individual synapse objects, so no verification needed
    }
}