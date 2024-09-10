package org.simbrain.network.gui.dialogs

import org.simbrain.network.connections.AllToAll
import org.simbrain.util.*
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import java.awt.Color

object NetworkPreferences: PreferenceHolder() {

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

    @UserParameter(label = "Weight Matrix arrow color ", tab = "Colors", order = 90)
    var weightMatrixArrowColor by ColorPreference(Color.ORANGE)

    @UserParameter(label = "Synapse Group arrow color ", tab = "Colors", order = 100)
    var synapseGroupArrowColor by ColorPreference(Color.GREEN)

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

    @UserParameter(label = "Default network time step", minimumValue = 0.0, increment = .1, tab = "Model")
    var defaultTimeStep by DoublePreference(.1)

    // Of course specific rules can have specific defaults
    @UserParameter(label = "Default learning rate", minimumValue = 0.0, increment = .1, tab = "Model")
    var defaultLearningRate by DoublePreference(.1)

    @UserParameter(label = "Default connection strategy", tab = "Connections")
    var connectionStrategy by ConnectionStrategyPreference(AllToAll())

    @UserParameter(
        label = "Weight Randomizer",
        description = "Randomizer for all free weights, regardless of polarity. Applying it can change the polarity of a neuron.",
        showDetails = false,
        order = 10,
        tab = "Randomizers"
    )
    var weightRandomizer by ProbabilityDistributionPreference(NormalDistribution(0.0, 0.1))

    @UserParameter(
        label = "Excitatory Randomizer",
        description = "Randomizer for all weights from polarized excitatory neurons. Applying it will not change the polarity of a neuron.",
        showDetails = false,
        order = 20,
        tab = "Randomizers"
    )
    var excitatoryRandomizer by ProbabilityDistributionPreference(UniformRealDistribution(0.0, 1.0))

    @UserParameter(
        label = "Inhibitory Randomizer",
        description = "Randomizer for all weights from polarized inhibitory neurons. Applying it will not change the polarity of a neuron.",
        showDetails = false,
        order = 30,
        tab = "Randomizers"
    )
    var inhibitoryRandomizer by ProbabilityDistributionPreference(UniformRealDistribution(-1.0, 0.0))

    @UserParameter(
        label = "Activation Randomizer",
        description = "Randomizer for all biases.",
        showDetails = false,
        order = 40,
        tab = "Randomizers"
    )
    var activationRandomizer by ProbabilityDistributionPreference(NormalDistribution(0.0, 1.0))

    @UserParameter(
        label = "Bias Randomizer",
        description = "Randomizer for all biases.",
        showDetails = false,
        order = 50,
        tab = "Randomizers"
    )
    var biasesRandomizer by ProbabilityDistributionPreference(NormalDistribution(0.0, 0.01))

    // Not currently exposing this with a user parameter since it's more of a convenience to remember between uses
    // of certain iteration dialogs.
    var numberOfIterations by IntegerPreference(10)

}
