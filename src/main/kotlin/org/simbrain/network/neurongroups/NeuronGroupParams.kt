package org.simbrain.network.neurongroups

import org.simbrain.network.core.Network
import org.simbrain.network.groups.AbstractNeuronCollection
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.GuiEditable

sealed class NeuronGroupParams: CopyableObject {

    var creationMode = true

    var numNeurons by GuiEditable(
        initValue = 10,
        label = "Number of Neurons",
        onUpdate = { enableWidget(creationMode) },
    )

    abstract fun create(net: Network): AbstractNeuronCollection

    override fun getTypeList(): List<Class<out CopyableObject>>? = if (creationMode) {
        listOf(
            CompetitiveGroupParams::class.java,
            SoftmaxParams::class.java,
            SOMParams::class.java,
            WinnerTakeAllParams::class.java
        )
    } else {
        null // remove dropdown after creation
    }

    fun commonCopy(params: NeuronGroupParams) {
        params.creationMode = creationMode
        params.numNeurons = numNeurons
    }
}