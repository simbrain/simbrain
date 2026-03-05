package org.simbrain.network.gui.dialogs

import org.simbrain.network.connections.AllToAll
import org.simbrain.network.gui.WandPalette
import org.simbrain.util.*
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import java.awt.Color

object NetworkPreferences: PreferenceHolder() {

    @UserParameter(
        label = "Network background color",
        description = "Background color of the network canvas",
        tab = "Colors",
        order = 10
    )
    var backgroundColor by ColorPreference(Color.WHITE)

    @UserParameter(
        label = "Node hot color",
        description = "Color for neurons with high positive activation or excitatory polarity",
        tab = "Colors",
        order = 20
    )
    var hotNodeColor by ColorPreference(Color.RED)

    @UserParameter(
        label = "Node cool color",
        description = "Color for neurons with low/negative activation or inhibitory polarity",
        tab = "Colors",
        order = 30
    )
    var coolNodeColor by ColorPreference(Color.BLUE)

    @UserParameter(
        label = "Connection line color",
        description = "Color for connections between neurons",
        tab = "Colors",
        order = 40
    )
    var connectionLineColor by ColorPreference(Color.BLACK)

    @UserParameter(
        label = "Spiking color",
        description = "Color used to highlight neurons and connections when spiking",
        tab = "Colors",
        order = 50
    )
    var spikingColor by ColorPreference(Color.YELLOW)

    @UserParameter(
        label = "Excitatory color",
        description = "Color for excitatory synapses (positive weights)",
        tab = "Colors",
        order = 60
    )
    var excitatorySynapseColor by ColorPreference(Color.RED)

    @UserParameter(
        label = "Inhibitory color",
        description = "Color for inhibitory synapses (negative weights)",
        tab = "Colors",
        order = 70
    )
    var inhibitorySynapseColor by ColorPreference(Color.BLUE)

    @UserParameter(
        label = "Zero color",
        description = "Color for synapses with zero or near-zero weights",
        tab = "Colors",
        order = 80
    )
    var zeroWeightColor by ColorPreference(Color.LIGHT_GRAY)

    @UserParameter(
        label = "Weight matrix arrow color",
        description = "Color for arrows indicating weight matrix connections",
        tab = "Colors",
        order = 90
    )
    var weightMatrixArrowColor by ColorPreference(Color.ORANGE)

    @UserParameter(
        label = "Weight matrix boundary color",
        description = "Color for weight matrix boundaries and outlines",
        tab = "Colors",
        order = 100
    )
    var weightMatrixBoundaryColor by ColorPreference(Color.ORANGE)

    @UserParameter(
        label = "Synapse group arrow color",
        description = "Color for arrows indicating synapse group connections",
        tab = "Colors",
        order = 110
    )
    var synapseGroupArrowColor by ColorPreference(Color.GREEN)

    @UserParameter(
        label = "Connector arrow color",
        description = "Color for arrows between weight matrices, tensor layers, etc.",
        tab = "Colors",
        order = 120
    )
    var connectorArrowColor by ColorPreference(Color.ORANGE)

    @UserParameter(
        label = "Nudge amount",
        description = "Distance in pixels to move selected objects when using Shift + arrow keys",
        tab = "GUI",
        order = 10
    )
    var nudgeAmount by DoublePreference(2.0)

    @UserParameter(
        label = "Neuron activation decimal places",
        description = "Number of decimal places to display in neuron activation text",
        minimumValue = 0.0,
        tab = "GUI",
        order = 20
    )
    var neuronActivationDecimalPlaces by IntegerPreference(1)

    @UserParameter(
        label = "Tooltip decimal places",
        description = "Number of decimal places to display in neuron tooltips",
        minimumValue = 0.0,
        tab = "GUI",
        order = 30
    )
    var tooltipDecimalPlaces by IntegerPreference(8)

    @UserParameter(
        label = "Min synapse size",
        description = "Minimum visual diameter for synapse circles (in pixels).",
        tab = "GUI",
        order = 40
    )
    var minWeightSize by IntegerPreference(7)

    @UserParameter(
        label = "Max synapse size",
        description = "Maximum visual diameter for synapse circles (in pixels).",
        tab = "GUI",
        order = 50
    )
    var maxWeightSize by IntegerPreference(20)

    @UserParameter(
        label = "Visibility threshold",
        description = "Hide individual synapses when synapse groups exceed this number.",
        tab = "GUI",
        order = 60
    )
    var synapseVisibilityThreshold by IntegerPreference(200)

    @UserParameter(
        label = "Wand radius",
        description = "Radius in pixels for the wand selection tool.",
        tab = "GUI",
        order = 70
    )
    var wandRadius by IntegerPreference(40)

    /**
     * The wand palette containing configured wand actions.
     * Not exposed as a @UserParameter since it has its own dedicated UI.
     * Stored in a file since it can exceed Java Preferences size limit.
     */
    var wandPalette: WandPalette
        get() = WandPaletteStorage.load()
        set(value) = WandPaletteStorage.save(value)

    @UserParameter(
        label = "Weight matrix target-source format",
        description = "If yes, each row corresponds to an output and each column corresponds to an input. " +
                "If no (source-target format), each row corresponds to an input and each column corresponds to an output.",
        tab = "GUI",
        order = 80)
    var weightMatrixTargetSource by BooleanPreference(true)

    @UserParameter(
        label = "Matrix image max width/height",
        description = "Maximum size of the weight matrix image in pixels in either dimension (i.e. width or height).",
        minimumValue = 1.0,
        maximumValue = 46340.0,
        tab = "GUI",
        order = 90
    )
    var weightMatrixImageMaxSize by IntegerPreference(1000)

    @UserParameter(
        label = "Default network time step",
        description = "Default time step for network simulation (in milliseconds).",
        minimumValue = 0.0,
        increment = .1,
        tab = "Model",
        order = 10
    )
    var defaultTimeStep by DoublePreference(.1)

    // Of course specific rules can have specific defaults
    @UserParameter(
        label = "Default learning rate",
        description = "Default learning rate for learning algorithms.",
        minimumValue = 0.0,
        increment = .1,
        tab = "Model",
        order = 20
    )
    var defaultLearningRate by DoublePreference(.1)

    @UserParameter(
        label = "Default connection strategy",
        description = "Default method for connecting neurons when creating synapses. Applied when using the 1-3 trick (press 1 to set sources, then 3 to connect with this strategy).",
        tab = "Connections",
        order = 10
    )
    var connectionStrategy by ConnectionStrategyPreference(AllToAll())

    @UserParameter(
        label = "Weight randomizer",
        description = "Randomizer for all free weights, regardless of polarity. Applying it can change the polarity of a neuron.",
        showDetails = false,
        order = 10,
        tab = "Randomizers"
    )
    var weightRandomizer by ProbabilityDistributionPreference(NormalDistribution(0.0, 1.5))

    @UserParameter(
        label = "Excitatory randomizer",
        description = "Randomizer for all weights from polarized excitatory neurons. Applying it will not change the polarity of a neuron.",
        showDetails = false,
        order = 20,
        tab = "Randomizers"
    )
    var excitatoryRandomizer by ProbabilityDistributionPreference(UniformRealDistribution(0.0, 1.0))

    @UserParameter(
        label = "Inhibitory randomizer",
        description = "Randomizer for all weights from polarized inhibitory neurons. Applying it will not change the polarity of a neuron.",
        showDetails = false,
        order = 30,
        tab = "Randomizers"
    )
    var inhibitoryRandomizer by ProbabilityDistributionPreference(UniformRealDistribution(-1.0, 0.0))

    @UserParameter(
        label = "Activation randomizer",
        description = "Randomizer for initial neuron activation values.",
        showDetails = false,
        order = 40,
        tab = "Randomizers"
    )
    var activationRandomizer by ProbabilityDistributionPreference(NormalDistribution(0.0, 1.0))

    @UserParameter(
        label = "Bias randomizer",
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
