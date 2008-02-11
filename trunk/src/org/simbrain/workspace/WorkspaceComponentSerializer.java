package org.simbrain.workspace;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import org.simbrain.workspace.gui.DesktopComponent;

public class WorkspaceComponentSerializer {
    int serializeComponent(final WorkspaceComponent<?> component, final OutputStream stream) {
        component.save(stream, null);
        
        return 0;
    }
    
    @SuppressWarnings("unchecked")
    WorkspaceComponent<?> deserializeWorkspaceComponent(final String className,
            final InputStream input, final String name, final String format) {
        try {
            Class<WorkspaceComponent<?>> clazz 
                = (Class<WorkspaceComponent<?>>) Class.forName(className);
            Method method = clazz.getMethod("open", InputStream.class, String.class, String.class);
            
            return (WorkspaceComponent<?>) method.invoke(null, input, name, format);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @SuppressWarnings("unchecked")
    DesktopComponent<?> deserializeDesktopComponent(final String className,
            final InputStream input, final String name) {
        try {
            Class<WorkspaceComponent<?>> clazz 
                = (Class<WorkspaceComponent<?>>) Class.forName(className);
            Method method = clazz.getMethod("open", InputStream.class, String.class, String.class);
            
            return (DesktopComponent<?>) method.invoke(null, input, name);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
