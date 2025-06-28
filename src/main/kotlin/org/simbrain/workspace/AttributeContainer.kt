package org.simbrain.workspace

/**
 * Designates an object as one that contains [Consumable] or [Producible] annotations, that can be linked together in
 * [Coupling]s.
 */
interface AttributeContainer {
    /**
     * Returns an attribute id that can be used to identify this container. Used in persistence (see
     * [org.simbrain.workspace.serialization.ArchivedAttribute] and in displaying Producers and Consumers.
     */
    val id: String?

    /**
     * Name of the parent containers. Used for display only.
     */
    var containerName: String?
        get() = null
        set(value) {}

    val childrenContainers: List<AttributeContainer>?
        get() = null
}