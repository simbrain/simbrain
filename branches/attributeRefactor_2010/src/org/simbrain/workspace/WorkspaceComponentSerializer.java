package org.simbrain.workspace;

import java.io.OutputStream;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Class used to assist with serializing components.
 *
 * @author Matt Watson
 */
public class WorkspaceComponentSerializer {

    /** The last component id. */
    private int lastComponent = 0;

    /** Map of components to their ids. */
    private final Map<WorkspaceComponent, Integer> componentIds
        = new IdentityHashMap<WorkspaceComponent, Integer>();

    /** Output stream. */
    private final OutputStream stream;

    /**
     * Construct serializer.
     *
     * @param stream output stream
     */
    WorkspaceComponentSerializer(final OutputStream stream) {
        this.stream = stream;
    }

    /**
     * Returns the id associated with a component.
     *
     * @param component The component to return an id for.
     * @return The component's id.
     */
    int getId(final WorkspaceComponent component) {
        Integer id = componentIds.get(component);
        if (id == null) {
            id = lastComponent++;
            componentIds.put(component, id);
        }
        return id;
    }

    /**
     * Serializes a component and returns the id for that component.
     *
     * @param component The component to serialize.
     * @param stream The stream to write to.
     * @return The id for the component that was serialized.
     */
    int serializeComponent(final WorkspaceComponent component) {
        component.save(stream, null);
        return getId(component);
    }
}
