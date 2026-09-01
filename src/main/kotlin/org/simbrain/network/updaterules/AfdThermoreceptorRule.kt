/**
 * Thermosensory update rule modeled on the AFD neuron of C. elegans, after Ikeda, Matsumoto, and
 * Izquierdo (2021). The neuron's state is the convolution of a measured biphasic response kernel with a
 * Hill-thresholded temperature history, so it responds to temperature *change* rather than absolute
 * temperature. Temperature arrives through [Neuron.setTemperatureInput] (typically coupled from a world
 * temperature sensor), not through synaptic input, which the rule deliberately ignores — in the fitted
 * circuit AFD integrates no synaptic or electrical input, so a gap junction on an AFD neuron exports its
 * state without perturbing it.
 */
package org.simbrain.network.updaterules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.interfaces.MembranePotentialProvider
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.GuiEditable
import java.util.Base64
import java.util.zip.GZIPInputStream
import kotlin.math.exp
import kotlin.math.pow

/**
 * Implemented by data holders that accept a temperature sample from [Neuron.setTemperatureInput].
 */
interface TemperatureReceivingData {
    fun receiveTemperature(temperatureCelsius: Double)
}

class AfdThermoreceptorRule : NeuronUpdateRule<AfdScalarData, EmptyMatrixData>(), MembranePotentialProvider {

    @UserParameter(
        label = "Threshold temperature",
        description = "Temperatures below this contribute nothing to the response.",
        increment = .1,
        order = 1
    )
    var thresholdTemperature: Double = 15.54

    @UserParameter(
        label = "Dissociation constant",
        description = "Half-saturation constant of the Hill threshold response.",
        increment = 1.0,
        order = 2
    )
    var dissociationConstant: Double = 69.22

    @UserParameter(
        label = "Hill coefficient",
        description = "Steepness of the Hill threshold response.",
        increment = .1,
        order = 3
    )
    var hillCoefficient: Double = 4.80

    var outputBias by GuiEditable(
        initValue = 11.57,
        label = "Output bias",
        description = "The activation sent along synapses is sigmoid(state + output bias); " +
            "the state itself is what the node displays.",
        increment = .1,
        order = 4
    )

    /**
     * The response kernel is sampled every 0.1 s over a 100 s window, so the rule is calibrated for a
     * network time step of 0.1. The state is set directly from the convolution each step; no synaptic
     * input is integrated.
     */
    context(Network)
    override fun apply(neuron: Neuron, data: AfdScalarData) {
        if (!data.primed) {
            neuron.activation = 1.0 / (1.0 + exp(-(data.state + outputBias)))
            return
        }
        data.pushSample()
        data.state = data.convolve(responseKernel) { temperature -> thresholdResponse(temperature) }
        neuron.activation = 1.0 / (1.0 + exp(-(data.state + outputBias)))
    }

    private fun thresholdResponse(temperature: Double): Double = if (temperature < thresholdTemperature) {
        0.0
    } else {
        val difference = (temperature - thresholdTemperature).pow(hillCoefficient)
        difference / (dissociationConstant + difference)
    }

    override fun createScalarData(): AfdScalarData = AfdScalarData()

    override fun membranePotential(neuron: Neuron): Double = (neuron.dataHolder as? AfdScalarData)?.state ?: 0.0

    override fun getGraphicalValue(n: Neuron): Double = (n.dataHolder as? AfdScalarData)?.state ?: 0.0

    override val graphicalLowerBound: Double get() = -3.0

    override val graphicalUpperBound: Double get() = 3.0

    override fun copy(): AfdThermoreceptorRule {
        val copy = AfdThermoreceptorRule()
        copy.thresholdTemperature = thresholdTemperature
        copy.dissociationConstant = dissociationConstant
        copy.hillCoefficient = hillCoefficient
        copy.outputBias = outputBias
        return copy
    }

    override val timeType: Network.TimeType
        get() = Network.TimeType.CONTINUOUS

    override val name: String
        get() = "Thermoreceptor (AFD)"

    companion object {

        private const val RESPONSE_RESOURCE = "/org/simbrain/network/updaterules/afd_response.csv.gz.b64"

        /**
         * The measured AFD response function, sampled every 0.1 s over a 100 s window. Index 0 is the
         * oldest sample and the last index the newest, matching the temperature-history ordering it is
         * convolved against. The kernel is biphasic and nearly zero-sum, so the neuron differentiates
         * its input.
         */
        val responseKernel: DoubleArray by lazy {
            val stream = requireNotNull(AfdThermoreceptorRule::class.java.getResourceAsStream(RESPONSE_RESOURCE)) {
                "Missing AFD response function resource $RESPONSE_RESOURCE"
            }
            val decoded = Base64.getMimeDecoder().decode(stream.readBytes())
            GZIPInputStream(decoded.inputStream()).bufferedReader().readLines()
                .filter { it.isNotBlank() }
                .map { it.trim().toDouble() }
                .toDoubleArray()
                .also { require(it.size == 1000) { "Expected 1000 AFD response samples, found ${it.size}" } }
        }
    }
}

/**
 * State for [AfdThermoreceptorRule]: the convolved membrane state, the latest temperature sample, and a
 * transient ring buffer of temperature history. The history is not serialized; after loading it re-primes
 * from the first sample received, so AFD starts at rest instead of replaying stale dynamics.
 */
class AfdScalarData(
    @UserParameter(label = "State", description = "Convolved thermosensory membrane state.")
    var state: Double = 0.0
) : ScalarDataHolder, TemperatureReceivingData {

    @Transient
    private var history: DoubleArray? = null

    @Transient
    private var head = 0

    @Transient
    private var temperature = 0.0

    val primed: Boolean get() = history != null

    override fun receiveTemperature(temperatureCelsius: Double) {
        temperature = temperatureCelsius
        if (history == null) {
            history = DoubleArray(AfdThermoreceptorRule.responseKernel.size) { temperatureCelsius }
            head = 0
        }
    }

    /**
     * Overwrites the oldest history slot with the newest sample, matching the shift-and-append of the
     * reference model without moving the array.
     */
    fun pushSample() {
        val buffer = history ?: return
        buffer[head] = temperature
        head = (head + 1) % buffer.size
    }

    /**
     * Sums kernel[i] × response(history[i]) oldest-first, in the same order as the reference model so
     * results agree to floating-point identity.
     */
    fun convolve(kernel: DoubleArray, response: (Double) -> Double): Double {
        val buffer = requireNotNull(history)
        var sum = 0.0
        for (index in kernel.indices) {
            sum += kernel[index] * response(buffer[(head + index) % buffer.size])
        }
        return sum
    }

    override fun copy(): AfdScalarData {
        val copy = AfdScalarData(state)
        history?.let { buffer ->
            copy.history = buffer.copyOf()
            copy.head = head
            copy.temperature = temperature
        }
        return copy
    }

    override fun clear() {
        state = 0.0
        history = null
        head = 0
        temperature = 0.0
    }
}
