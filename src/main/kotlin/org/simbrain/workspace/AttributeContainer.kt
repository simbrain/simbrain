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
     * Human-readable name for this container in coupling descriptions and plot labels. Defaults to [id]; override
     * to prefer a user-supplied name, e.g. a network model's label. Deliberately distinct from
     * `NetworkModel.displayName` so that classes inheriting both do not have to disambiguate.
     */
    val attributeName: String?
        get() = id

    /**
     * Name of the parent containers. Used for display only.
     */
    var containerName: String?
        get() = null
        set(value) {}

    val childrenContainers: List<AttributeContainer>?
        get() = null
}