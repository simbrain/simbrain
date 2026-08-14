package org.simbrain.network.core

/**
 * Optional detailed diagnostic output for a network model whose meaningful contents are not
 * themselves registered as network models.
 */
interface NetworkDebugInfoProvider {
    fun appendNetworkDebugInfo(builder: StringBuilder, indent: String = "")
}
