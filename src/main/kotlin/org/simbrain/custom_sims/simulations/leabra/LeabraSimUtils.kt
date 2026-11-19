/**
 * Helpers and constants shared by the Leabra simulations.
 */
package org.simbrain.custom_sims.simulations.leabra

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.piccolo2d.nodes.PPath
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.nodes.WeightMatrixNode
import org.simbrain.util.point
import smile.math.matrix.Matrix
import java.awt.BasicStroke
import java.awt.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * A pattern with 1 at [index] and 0 elsewhere, or all zeros when [index] is null.
 */
fun oneHot(size: Int, index: Int?) = DoubleArray(size) { if (it == index) 1.0 else 0.0 }

/**
 * Relative strength of feedback projections, which are weaker than feedforward ones.
 */
const val FEEDBACK_SCALE = 0.3

private const val MESSAGE_FONT_SIZE = 32

/**
 * A message drawn on the network, such as the gate state. Messages use the theme's text color, which contrasts
 * strongly with the background; color is reserved for [showResult], where it carries meaning.
 */
fun leabraMessage(text: String, label: String, x: Int, y: Int) = NetworkTextObject(text).apply {
    this.label = label
    fontSize = MESSAGE_FONT_SIZE
    location = point(x, y)
}

/**
 * Dark green and red with about 5:1 contrast on white. They always come with a word and a symbol, so that color is
 * never the only cue, which matters for red-green color blindness.
 */
val CORRECT_COLOR = Color(0x1a, 0x7f, 0x37)
val WRONG_COLOR = Color(0xcf, 0x22, 0x2e)

/**
 * An empty label for [showResult].
 */
fun resultLabel(label: String, x: Int, y: Int) = leabraMessage("", label, x, y).apply { isBold = true }

private const val FLASH_MARGIN = 6.0
private const val FLASH_HOLD_MILLIS = 500L
private const val FLASH_FADE_MILLIS = 400L
private val FLASH_FILL = Color(255, 200, 0, 130)
private val FLASH_OUTLINE = Color(215, 140, 0)

/**
 * Highlight the weight matrices drawn by [nodes] while [block] runs, then hold briefly, call [afterHold], and fade
 * the highlight out. Used to show when and where learning happens.
 */
suspend fun flashWeights(
    networkPanel: NetworkPanel,
    nodes: List<WeightMatrixNode>,
    afterHold: () -> Unit = {},
    block: suspend () -> Unit = {}
) {
    val flashes = withContext(Dispatchers.Swing) {
        nodes.map { node ->
            val bounds = node.imageBox.globalFullBounds
            PPath.createRoundRectangle(
                bounds.x - FLASH_MARGIN, bounds.y - FLASH_MARGIN,
                bounds.width + 2 * FLASH_MARGIN, bounds.height + 2 * FLASH_MARGIN, 8.0, 8.0
            ).apply {
                paint = FLASH_FILL
                stroke = BasicStroke(3f)
                strokePaint = FLASH_OUTLINE
                pickable = false
                networkPanel.canvas.layer.addChild(this)
            }
        }
    }
    try {
        block()
    } finally {
        withContext(NonCancellable) {
            delay(FLASH_HOLD_MILLIS)
            afterHold()
            withContext(Dispatchers.Swing) { flashes.forEach { it.animateToTransparency(0f, FLASH_FADE_MILLIS) } }
            delay(FLASH_FADE_MILLIS)
            withContext(Dispatchers.Swing) { flashes.forEach { it.removeFromParent() } }
        }
    }
}

/**
 * Set the display ranges of a dopamine unit, which goes from -1 to 1, and of Go and NoGo units, which go from 0 to 1.
 * They are set by hand rather than updated, so their rule only sets how they are colored; its default range of
 * -10 to 10 would leave them nearly white.
 */
fun showDopamineAndGatingRanges(dopamine: NeuronArray, basalGanglia: NeuronArray) {
    dopamine.updateRule = LinearRule().apply {
        upperBound = 1.0
        lowerBound = -1.0
    }
    basalGanglia.updateRule = LinearRule().apply {
        upperBound = 1.0
        lowerBound = 0.0
    }
}

/**
 * Show whether an answer was [correct], or clear the label when null.
 */
fun NetworkTextObject.showResult(correct: Boolean?) {
    color = when (correct) {
        true -> CORRECT_COLOR
        false -> WRONG_COLOR
        null -> null
    }
    text = when (correct) {
        true -> "✓ CORRECT"
        false -> "✗ WRONG"
        null -> ""
    }
}

internal const val BG_LABEL = "Basal ganglia (BG)"
internal const val DOPAMINE_LABEL = "Dopamine (SNc)"
internal const val GATING_WEIGHTS_LABEL = "Gating weights"
internal const val DOPAMINE_PROJECTION_LABEL = "Dopamine"

/**
 * Offsets of the Go and NoGo units within each Go/NoGo pair of a basal ganglia layer.
 */
internal const val GO = 0
internal const val NOGO = 1

/**
 * Dopamine smaller than this is shown as no change. Learning still uses it.
 */
internal const val DOPAMINE_THRESHOLD = 0.1

/**
 * A sample from the standard normal distribution.
 */
internal fun Random.gaussian() = sqrt(-2 * ln(1 - nextDouble())) * cos(2 * PI * nextDouble())

/**
 * Let dopamine [da] change gating [weights] for the source units tagged in [tags], one Go/NoGo pair at rows [goRow]
 * and [nogoRow]. A tag is positive for a gate that opened and negative for one that stayed closed, so a burst
 * strengthens the choices made and a dip weakens them. Go and NoGo move in opposite directions, like D1 and D2
 * receptors, and each change is scaled by the room left between 0 and 1.
 */
internal fun learnGating(weights: Matrix, goRow: Int, nogoRow: Int, tags: DoubleArray, da: Double, rate: Double) {
    for (i in tags.indices) {
        if (tags[i] == 0.0) continue
        for ((j, sign) in listOf(goRow to 1.0, nogoRow to -1.0)) {
            val dw = sign * da * tags[i]
            val wt = weights[j, i]
            val bounded = if (dw > 0) dw * (1 - wt) else dw * wt
            weights[j, i] = (wt + rate * bounded).coerceIn(0.0, 1.0)
        }
    }
}

/**
 * Set the activations of a unit array that is set by hand rather than updated, such as basal ganglia or dopamine,
 * and redraw it.
 */
internal fun NeuronArray.showActivations(values: DoubleArray) {
    activations = Matrix.column(values)
    events.updated.fire()
}

/**
 * The reward a critic expects, stored as the bias of its dopamine unit so that it is saved with the network.
 */
internal var NeuronArray.expectedReward: Double
    get() = biases[0, 0]
    set(value) {
        biases[0, 0] = value
    }
