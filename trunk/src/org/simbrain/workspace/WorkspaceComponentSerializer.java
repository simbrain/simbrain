package org.simbrain.workspace;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;

import org.simbrain.workspace.gui.DesktopComponent;

public class WorkspaceComponentSerializer {
    private int lastComponent = 0;
    private final Map<WorkspaceComponent<?>, Integer> componentIds 
        = new IdentityHashMap<WorkspaceComponent<?>, Integer>();
    private int lastAttribute = 0;
    private final Map<Attribute, Integer> attributeIds 
        = new IdentityHashMap<Attribute, Integer>();
    
    int getId(final WorkspaceComponent<?> component) {
        Integer id = componentIds.get(component);
        
        if (id == null) {
            id = lastComponent++;
            componentIds.put(component, id);
        }
        
        return id;
    }
    
    int assignId(final Attribute attribute) {
        Integer id = attributeIds.get(attribute);
        
        if (id == null) {
            id = lastAttribute++;
            attributeIds.put(attribute, id);
        }
        
        attribute.setId(id);
        
        return id;
    }
    
    int serializeComponent(final WorkspaceComponent<?> component, final OutputStream stream) {
        component.save(stream, null);
        
        return getId(component);
    }
}
