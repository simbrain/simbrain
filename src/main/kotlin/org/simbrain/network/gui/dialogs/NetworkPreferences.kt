package org.simbrain.network.gui.dialogs

import org.simbrain.util.*
import java.awt.Color

object NetworkPreferences: PreferenceHolder {

    @UserParameter(label = "Network background color", tab = "Colors")
    var backgroundColor by ColorPreference(Color.WHITE)

    @UserParameter(label = "Line color", tab = "Colors")
    var lineColor by ColorPreference(Color.BLACK)

    @UserParameter(label = "Hot color", tab = "Colors")
    var hotNodeColor by ColorPreference(Color.RED)

    @UserParameter(label = "Cool color", tab = "Colors")
    var coolNodeColor by ColorPreference(Color.BLUE)

    @UserParameter(label = "Spiking color", tab = "Colors")
    var spikingColor by ColorPreference(Color.YELLOW)

    @UserParameter(label = "Excitatory color", tab = "Colors")
    var excitatorySynapseColor by ColorPreference(Color.RED)

    @UserParameter(label = "Inhibitory color", tab = "Colors")
    var inhibitorySynapseColor by ColorPreference(Color.BLUE)

    @UserParameter(label = "Zero color", tab = "Colors")
    var zeroWeightColor by ColorPreference(Color.LIGHT_GRAY)

    @UserParameter(label = "Min synapse size", tab = "GUI")
    var minWeightSize by IntegerPreference(7)

    @UserParameter(label = "Max synapse size", tab = "GUI")
    var maxWeightSize by IntegerPreference(20)

    @UserParameter(label = "Nudge amount", tab = "GUI")
    var nudgeAmount by IntegerPreference(2)

    @UserParameter(label = "Visibility threshold", tab = "GUI")
    var synapseVisibilityThreshold by IntegerPreference(200)

    @UserParameter(label = "Wand radius", tab = "GUI")
    var wandRadius by IntegerPreference(40)

    @UserParameter(label = "Self connection allowed")
    var selfConnectionAllowed by BooleanPreference(false)

    @UserParameter(label = "Network time step", tab = "Model")
    var defaultTimeStep by DoublePreference(.1)

    @UserParameter(label = "Default learning rate", tab = "Model")
    var learningRate by DoublePreference(.1)

}
