package org.simbrain.network.gui.nodes

import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.format
import org.simbrain.util.getColorGradient
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import kotlin.math.min

class NeuronCircleNode(val networkPanel: NetworkPanel): PPath.Double() {

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
        addChild(it)
    }

    private val labelText = PText().also {
        it.font = NEURON_FONT
        addChild(it)
    }

    init {
        getUnionOfChildrenBounds(boundsReference)
    }

    private var activation: kotlin.Double = 0.0
    private var graphicalBounds = -1.0..1.0

    var isTextVisible = networkPanel.scalingFactor > TEXT_VISIBILITY_THRESHOLD
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
                mainNode.strokePaint = NetworkPreferences.lineColor
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
        updatePaint(activation.getColorGradient(graphicalBounds, NetworkPreferences.coolNodeColor, NetworkPreferences.hotNodeColor))
        updateStroke()

        if (isTextVisible) {
            activationText.text = if (activation > -0.95 && activation < 0.95) {
                activation.format(1).replace("0.", ".").replace(Regex("^-?.0$"), "0")
            } else {
                activation.format(0)
            }

            val targetWidth = min(NEURON_DIAMETER.toDouble() * 0.8, activationText.width)
            activationText.scale = targetWidth / activationText.width
            activationText.centerBoundsOnPoint(mainNode.bounds.x + mainNode.bounds.width / 2, mainNode.bounds.y + mainNode.bounds.height / 2)
        }
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
        isTextVisible = networkPanel.scalingFactor > TEXT_VISIBILITY_THRESHOLD
        super.paint(paintContext)
    }
}

/**
 * Default text visibility threshold.
 */
const val TEXT_VISIBILITY_THRESHOLD = 0.5

/**
 * Diameter of neuron.
 */
const val NEURON_DIAMETER = 24

/**
 * Neuron Font.
 */
val NEURON_FONT = Font("Arial", Font.PLAIN, 11)

/**
 * Priority Font.
 */
val PRIORITY_FONT = Font("Courier", Font.PLAIN, 9)

/**
 * Neuron font bold.
 */
val NEURON_FONT_BOLD = Font("Arial", Font.BOLD, 11)
