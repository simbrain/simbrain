package org.simbrain.workspace.serialization;

import org.simbrain.workspace.Attribute;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * The class used to represent an attribute in the archive.
 *
 * @author Matt Watson
 * @author Jeff Yoshimi
 */
@XStreamAlias("ArchivedAttribute")
public final class ArchivedAttribute {

    /** The attribute id. */
    private String attributeId;

    /**
     * Creates a new instance.
     *
     * @param parent The parent archive.
     * @param attribute The attribute this instance represents.
     */
    ArchivedAttribute(ArchivedWorkspace parent, Attribute attribute) {
        this.attributeId = attribute.getId();
    }

    /**
     * @return the attribute id
     */
    public String getAttributeId() {
        return attributeId;
    }
}