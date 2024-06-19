package org.simbrain.workspace.serialization;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.simbrain.workspace.Attribute;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * The class used to represent an attribute in the archive.
 *
 * @author Matt Watson
 * @author Jeff Yoshimi
 */
@XStreamAlias("ArchivedAttribute")
public final class ArchivedAttribute {

    /**
     * The id of the workspace component.
     */
    private String componentId;

    /**
     * The id of the consumer or producer object.
     */
    private String attributeId;

    /**
     * The method name.
     */
    private String methodName;

    /**
     * Creates a new ArchivedAttribute.
     *
     * @param component The component which owns this attribute.
     * @param attribute The attribute.
     */
    ArchivedAttribute(WorkspaceComponent component, Attribute attribute) {
        componentId = component.getName();
        attributeId = attribute.getId();
        methodName = attribute.getMethod().getName();
    }

    public String getComponentId() {
        return componentId;
    }

    public String getAttributeId() {
        return attributeId;
    }

    public String getMethodName() {
        return methodName;
    }

}
