package org.simbrain.workspace;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.simbrain.workspace.gui.GuiComponent;

/**
 * Class used to assist with deserializing workspace components.
 * 
 * @author Matt Watson
 */
public class WorkspaceComponentDeserializer {
    /** A map of keys to their components. */
    private final Map<String, WorkspaceComponent<?>> componentKeys
        = new HashMap<String, WorkspaceComponent<?>>();
    
    /**
     * Returns the workspace component associated with the given uri.
     * 
     * @param uri The uri for the component to retrieve.
     * @return The component for the uri.
     */
    WorkspaceComponent<?> getComponent(final String uri) {
        return componentKeys.get(uri);
    }
    
    /**
     * Deserializes a workspace component using the information from the provided
     * component and input stream.
     * 
     * @param component The component entry from the archive contents.
     * @param input The input stream to read data from.
     * @return The deserialized WorkspaceComponent.
     */
    @SuppressWarnings("unchecked")
    WorkspaceComponent<?> deserializeWorkspaceComponent(final ArchiveContents.Component component,
            final InputStream input) {
        try {
            Class<WorkspaceComponent<?>> clazz
                = (Class<WorkspaceComponent<?>>) Class.forName(component.className);
            Method method = clazz.getMethod("open", InputStream.class, String.class, String.class);
            
            WorkspaceComponent<?> wc = (WorkspaceComponent<?>)
                method.invoke(null, input, component.name, null);
                        
            componentKeys.put(component.uri, wc);
            wc.setChangedSinceLastSave(false);
            return wc;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Deserializes a desktop component given a class, input stream and name.
     * 
     * @param className The class name for the DesktopComponent
     * @param component The desktop component entry for the desktop component.
     * @param input The input stream.
     * @param name The name of the desktop component.
     * @return The deserialized desktop component.
     */
    @SuppressWarnings("unchecked")
    GuiComponent<?> deserializeDesktopComponent(final String className,
            final WorkspaceComponent component, final InputStream input, final String name) {
        try {
            Class<WorkspaceComponent<?>> clazz
                = (Class<WorkspaceComponent<?>>) Class.forName(className);
            Method method = clazz.getMethod("open", WorkspaceComponent.class,
                InputStream.class, String.class);
            
            return (GuiComponent<?>) method.invoke(null, component, input, name);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
