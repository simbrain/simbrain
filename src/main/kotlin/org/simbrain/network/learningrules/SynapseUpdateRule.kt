/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.learningrules

import org.simbrain.network.core.Connector
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.learningrules.*
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.util.Utils
import org.simbrain.util.propertyeditor.CopyableObject
import java.util.*

/**
 * A local learning rule for updating synapses.
 *
 * @author Jeff Yoshimi
 */
abstract class SynapseUpdateRule<out DS : ScalarDataHolder, out DM : MatrixDataHolder> : CopyableObject {
    override fun getTypeList(): List<Class<out CopyableObject?>>? {
        return RULE_LIST
    }

    /**
     * Defines the learning rule as it applies to scalar data.
     *
     * TODO: Finish doing this for all learning rules.
     *
     * @param synapse a reference to a synapse with its parameters and access to source and target neurons, etc.
     * @param data a scalar data holder that can hold data that must be updated with this rule.
     */
    context(Network)
    abstract fun apply(synapse: Synapse, data: @UnsafeVariance DS)

    /**
     * Override to return an appropriate data holder for a given rule.
     */
    fun createScalarData(): DS {
        return DEFAULT_SCALAR_DATA as DS
    }

    /**
     * Override to define a learning update rule for weight matrices and other connectors.
     *
     * NOTE: Only a few of these have been done.
     *
     * @param connector reference to a weight matrix or other connector and its matrix-valued data
     * @param dataHolder a holder for mutable data used in matrix versions of an update rule
     */
    context(Network)
    open fun apply(connector: Connector, dataHolder: @UnsafeVariance DM) {}

    /**
     * Override to return an appropriate data holder for a given rule.
     */
    fun createMatrixData(size: Int): DM {
        return DEFAULT_MATRIX_DATA as DM
    }

    /**
     * Initialize the update rule and make necessary changes to the parent
     * synapse.
     *
     * @param synapse parent synapse
     */
    abstract fun init(synapse: Synapse)

    /**
     * Returns a deep copy of the update rule.
     *
     * @return Duplicated update rule
     */
    abstract fun deepCopy(): SynapseUpdateRule<*, *>

    /**
     * Returns a name for this learning rule. Used in combo boxes in
     * the GUI.
     *
     * @return the description.
     */
    abstract override val name: String

    /**
     * Returns string for tool tip or short description. Override to provide
     * custom information.
     *
     * @param synapse reference to parent synapse
     * @return tool tip text
     */
    fun getToolTipText(synapse: Synapse): String {
        return "(" + synapse.id + ") Strength: " + Utils.round(synapse.strength, MAX_DIGITS)
    }

    override fun copy(): SynapseUpdateRule<*, *> {
        return deepCopy()
    }

    companion object {
        /**
         * Rules for drop-down list used by [org.simbrain.util.propertyeditor.ObjectTypeEditor]
         * to set the learning rule on a synapse.
         */
        var RULE_LIST: List<Class<out CopyableObject?>> = Arrays.asList<Class<out CopyableObject?>>(
            StaticSynapseRule::class.java,
            HebbianRule::class.java, HebbianCPCARule::class.java, HebbianThresholdRule::class.java,
            OjaRule::class.java, PfisterGerstner2006Rule::class.java, ShortTermPlasticityRule::class.java,
            STDPRule::class.java, SubtractiveNormalizationRule::class.java
        )

        /**
         * The maximum number of digits to display in the tool tip.
         */
        private const val MAX_DIGITS = 9

        /**
         * Default data holder for scalar data.
         */
        private val DEFAULT_SCALAR_DATA: ScalarDataHolder = EmptyScalarData

        /**
         * Default data holder for matrix data.
         */
        private val DEFAULT_MATRIX_DATA: MatrixDataHolder = EmptyMatrixData
    }
}
