package org.simbrain.network.core

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Synapse
import org.simbrain.network.neurongroups.*
import org.simbrain.network.spikeresponders.UDF
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.subnetworks.SRNNetwork
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.updaterules.IzhikevichRule
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.point
import java.util.List

class NetworkTest {
    lateinit var net: Network
    lateinit var n1: Neuron
    lateinit var n2: Neuron
    lateinit var s1: Synapse
    lateinit var ng1: NeuronGroup
    lateinit var ng2: NeuronGroup
    lateinit var na1: NeuronArray
    lateinit var na2: NeuronArray
    lateinit var nc1: NeuronCollection
    lateinit var wm1: WeightMatrix
    lateinit var sg1: SynapseGroup
    lateinit var softmax: SoftmaxGroup
    lateinit var som: SOMGroup
    lateinit var wta: WinnerTakeAll
    lateinit var competitive: CompetitiveGroup

    // TODO: Other subneworks
    var bp: BackpropNetwork? = null
    var srn: SRNNetwork? = null


    @BeforeEach
    fun setUpNetwork() {
        net = Network()

        n1 = Neuron()
        n1.label = "neuron1"
        (n1.dataHolder as BiasedScalarData).bias = 1.0
        net.addNetworkModel(n1)
        n2 = Neuron().apply {
            updateRule = IzhikevichRule()
        }
        n2.label = "neuron2"
        net.addNetworkModel(n2)

        s1 = Synapse(n1, n2).apply {
            spikeResponder = UDF()
        }
        net.addNetworkModel(s1)

        nc1 = NeuronCollection(List.of(n1, n2))
        net.addNetworkModel(nc1)

        ng1 = NeuronGroup(10)
        ng1.label = "neuron_group_1"
        net.addNetworkModel(ng1)
        ng2 = NeuronGroup(10)
        ng2.label = "ng2"
        net.addNetworkModel(ng2)

        sg1 = SynapseGroup(ng1, ng2)
        net.addNetworkModel(sg1)

        na1 = NeuronArray(10)
        na2 = NeuronArray(10)
        wm1 = WeightMatrix(na1, na2)
        net.addNetworkModels(List.of(na1, na2, wm1))

        softmax = SoftmaxGroup(5)
        softmax.label = "softmax"
        som = SOMGroup(5)
        som.label = "som"
        competitive = CompetitiveGroup(5)
        competitive.label = "competitive"
        wta = WinnerTakeAll(net, 5)
        wta.label = "wta"

        net.addNetworkModels(softmax, som, competitive, wta)

        bp = BackpropNetwork(intArrayOf(3, 5, 4), point(0, 0))
        bp!!.label = "backprop"
        srn = SRNNetwork(5, 5, 5, point(0, 0))
        srn!!.label = "srn"
        net.addNetworkModels(bp!!, srn!!)
    }

    @Test
    fun testDeleteObjects() {
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
        Assertions.assertEquals(1.0, (n1.dataHolder as BiasedScalarData).bias)
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

        // 2 free neurons, 2 x 10 in each of two neuron groups = 22
        // (2 in neuron collection are free neurons)
        Assertions.assertEquals(22, net.flatNeuronList.size)
    }
}