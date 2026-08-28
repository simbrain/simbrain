package org.simbrain.workspace

/**
 * Thrown when creating a coupling from a producer to a consumer whose types do not match.
 */
class MismatchedAttributesException(message: String) : Exception(message)
