package org.simbrain.custom_sims.simulations.sorn

import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.learningrules.STDPRule
import org.simbrain.network.learningrules.SynapseUpdateRule
import org.simbrain.network.util.EmptyScalarData

class AddSTDPRule : STDPRule() {
    private var srcSpk = false

    private var tarSpk = false

    override fun init(synapse: Synapse) {
    }

    override val name: String
        get() = "STDP"

    override fun copy(): SynapseUpdateRule<*, *> {
        val duplicateSynapse = STDPRule()
        duplicateSynapse.tauMinus = this.tauMinus
        duplicateSynapse.tauPlus = this.tauPlus
        duplicateSynapse.wMinus = this.wMinus
        duplicateSynapse.wPlus = this.wPlus
        duplicateSynapse.learningRate = this.learningRate
        return duplicateSynapse
    }

    context(Network)
    override fun apply(synapse: Synapse, data: EmptyScalarData) {
        val ss = synapse.source.isSpike
        val st = synapse.target.isSpike
        synapse.strength += learningRate * ((if (srcSpk && st) 1 else 0) - (if (tarSpk && ss) 1 else 0))
        srcSpk = ss
        tarSpk = st
    }
}
