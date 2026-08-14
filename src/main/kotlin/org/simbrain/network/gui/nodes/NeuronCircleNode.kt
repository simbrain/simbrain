package org.simbrain.network.gui.nodes

import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.NetworkTheme
import org.simbrain.util.Theme
import org.simbrain.util.format
import org.simbrain.util.getColorGradient
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import kotlin.math.min

class NeuronCircleNode(private val scalingFactor: () -> kotlin.Double = { 1.0 }): PPath.Double() {

    constructor(networkPanel: NetworkPanel) : this({ networkPanel.scalingFactor })

    private val circle = createEllipse(
        (0 - NEURON_DIAMETER / 2).toFloat(),
        (0 - NEURON_DIAMETER / 2).toFloat(),
        NEURON_DIAMETER.toFloat(),
        NEURON_DIAMETER.toFloat()
    ).also { addChild(it) }

    private val square = createRectangle(
        (0 - NEURON_DIAMETER / 2).toFloat(),
        (0 - NEURON_DIAMETER / 2).toFloat(),
        NEURON_DIAMETER.toFloat(),
        NEURON_DIAMETER.toFloat()
    )

    var useSquare = false
        set(value) {
            field = value
            updateShape(value)
        }

    val mainNode get() = if (useSquare) square else circle

    var isSpiking = false
        set(value) {
            field = value
            redraw()
        }

    var customStrokeColor: Color? = null
        set(value) {
            if (field == value) return
            field = value
            redraw()
        }

    private val activationText = PText().also {
        it.font = NEURON_FONT_BOLD
        it.textPaint = NetworkTheme.current.valueText
        addChild(it)
    }

    private val labelText = PText().also {
        it.font = NEURON_FONT
        it.textPaint = NetworkTheme.current.valueText
        addChild(it)
    }

    init {
        getUnionOfChildrenBounds(boundsReference)
    }

    private var activation: kotlin.Double = 0.0
    private var graphicalBounds = -1.0..1.0

    var isTextVisible = scalingFactor() > TEXT_VISIBILITY_THRESHOLD
        private set(value) {
            if (field != value) {
                field = value
                if (value) {
                    drawActivation(activation, graphicalBounds)
                    addChild(activationText)
                    addChild(labelText)
                } else {
                    removeChild(activationText)
                    removeChild(labelText)
                }
            }
        }

    private fun updatePaint(color: Color) {
        mainNode.paint = if (isSpiking) {
            NetworkPreferences.spikingColor
        } else {
            color
        }
    }

    private fun updateStroke() {
        customStrokeColor.let { value ->
            if (isSpiking) {
                mainNode.strokePaint = NetworkPreferences.spikingColor
                return
            }
            if (value == null) {
                mainNode.strokePaint = NetworkTheme.current.nodeOutline
            } else {
                mainNode.strokePaint = value
            }
        }
    }

    private fun redraw() {
        drawActivation(activation, graphicalBounds)
    }

    fun drawActivation(activation: kotlin.Double, graphicalBounds: ClosedFloatingPointRange<kotlin.Double>) {
        this.activation = activation
        this.graphicalBounds = graphicalBounds
        activationText.textPaint = NetworkTheme.current.valueText
        labelText.textPaint = NetworkTheme.current.valueText
        updatePaint(activation.getColorGradient(graphicalBounds, NetworkPreferences.coolNodeColor, NetworkPreferences.hotNodeColor))
        updateStroke()

        updateActivationText()
    }

    private fun updateActivationText() {
        if (isTextVisible) {
            val decimalPlaces = NetworkPreferences.neuronActivationDecimalPlaces
            activationText.text = if (activation > -0.95 && activation < 0.95) {
                // For small values, use the preference-based decimal places but apply special formatting
                activation.format(decimalPlaces).replace("0.", ".").replace(Regex("^-?.0+$"), "0")
            } else {
                // For large values, use the preference-based decimal places
                activation.format(decimalPlaces)
            }

            val targetWidth = min(NEURON_DIAMETER.toDouble() * 0.8, activationText.width)
            activationText.scale = targetWidth / activationText.width
            activationText.centerBoundsOnPoint(mainNode.bounds.x + mainNode.bounds.width / 2, mainNode.bounds.y + mainNode.bounds.height / 2)
        }
    }

    fun forceUpdateActivationText() {
        // Force text visibility check and update regardless of scaling factor
        isTextVisible = scalingFactor() > TEXT_VISIBILITY_THRESHOLD
        updateActivationText()
    }

    fun setLabel(label: String?) {
        if (label != null) {
            if (indexOfChild(labelText) == -1) {
                addChild(labelText)
            }
            labelText.text = label
            labelText.centerBoundsOnPoint(mainNode.bounds.x + mainNode.bounds.width / 2, mainNode.bounds.y - labelText.bounds.height + 5.0)
        } else {
            removeChild(labelText)
        }
        getUnionOfChildrenBounds(boundsReference)
    }

    fun setClamped(clamped: Boolean) {
        if (clamped) {
            mainNode.stroke = BasicStroke(2.0f)
        } else {
            mainNode.stroke = BasicStroke(1.0f)
        }
    }

    private fun updateShape(useSquare: Boolean) {
        if (useSquare) {
            removeChild(circle)
            addChild(0, square)
        } else {
            removeChild(square)
            addChild(0, circle)
        }
    }

    override fun paint(paintContext: PPaintContext?) {
        isTextVisible = scalingFactor() > TEXT_VISIBILITY_THRESHOLD
        super.paint(paintContext)
    }
}

const val TEXT_VISIBILITY_THRESHOLD = 0.5

const val NEURON_DIAMETER = 24

val NEURON_FONT: Font get() = Theme.body

val NEURON_FONT_BOLD: Font get() = Theme.bodyBold
