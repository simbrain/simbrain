package org.simbrain.network.gui.dialogs

import org.simbrain.util.*
import java.awt.Color

object NetworkPreferences: PreferenceHolder {

    @UserParameter(label = "Network background color", tab = "Colors", order = 10)
    var backgroundColor by ColorPreference(Color.WHITE)

    @UserParameter(label = "Node Hot color", tab = "Colors", order = 20)
    var hotNodeColor by ColorPreference(Color.RED)

    @UserParameter(label = "Node Cool color", tab = "Colors", order = 30)
    var coolNodeColor by ColorPreference(Color.BLUE)

    @UserParameter(label = "Line color", tab = "Colors", order = 40)
    var lineColor by ColorPreference(Color.BLACK)

    @UserParameter(label = "Spiking color", tab = "Colors", order = 50)
    var spikingColor by ColorPreference(Color.YELLOW)

    @UserParameter(label = "Excitatory color", tab = "Colors", order = 60)
    var excitatorySynapseColor by ColorPreference(Color.RED)

    @UserParameter(label = "Inhibitory color", tab = "Colors", order = 70)
    var inhibitorySynapseColor by ColorPreference(Color.BLUE)

    @UserParameter(label = "Zero color", tab = "Colors", order = 80)
    var zeroWeightColor by ColorPreference(Color.LIGHT_GRAY)

    @UserParameter(label = "Min synapse size", tab = "GUI")
    var minWeightSize by IntegerPreference(7)

    @UserParameter(label = "Max synapse size", tab = "GUI")
    var maxWeightSize by IntegerPreference(20)

    @UserParameter(label = "Nudge amount", tab = "GUI")
    var nudgeAmount by DoublePreference(2.0)

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
