package org.simbrain.workspace;

/**
 * Exception thrown when a coupling is created with mismatched attributes.
 *
 * @author jyoshimi
 */
public class MismatchedAttributesException extends Exception {

    /**
     * @param message
     */
    public MismatchedAttributesException(final String message) {
        super(message);
    }

}
