package org.simbrain.network.neurongroups

import org.simbrain.network.core.Network
import org.simbrain.network.groups.AbstractNeuronCollection
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.GuiEditable

sealed class NeuronGroupParams: CopyableObject {


    var numNeurons by GuiEditable(
        initValue = 10,
        label = "Number of Neurons",
    )

    abstract fun create(net: Network): AbstractNeuronCollection

    override fun getTypeList(): List<Class<out CopyableObject>> = listOf(
        CompetitiveGroupParams::class.java,
        SoftmaxParams::class.java,
        SOMParams::class.java,
        WinnerTakeAllParams::class.java
    )
}