package org.simbrain.workspace.serialization;

import org.simbrain.workspace.Attribute;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * The class used to represent an attribute in the archive.
 *
 * @author Matt Watson
 * @author Jeff Yoshimi
 */
@XStreamAlias("ArchivedAttribute")
public final class ArchivedAttribute {

    /** The id of the workspace component. */
    private String componentId;

    /** The id of the couplable object. */
    private String id;

    /** The method name. */
    private String methodName;

    /**
     * Creates a new ArchivedAttribute.
     * @param component The component which owns this attribute.
     * @param attribute The attribute.
     */
    ArchivedAttribute(WorkspaceComponent component, Attribute attribute) {
        componentId = component.getName();
        id = attribute.getId();
        methodName = attribute.getMethod().getName();
    }

    public String getComponentId() {
        return componentId;
    }

    public String getId() {
        return id;
    }

    public String getMethodName() {
        return methodName;
    }

}
