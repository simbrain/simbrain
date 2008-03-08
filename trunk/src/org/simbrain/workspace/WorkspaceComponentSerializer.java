package org.simbrain.workspace;

import java.io.OutputStream;
import java.util.IdentityHashMap;
import java.util.Map;

public class WorkspaceComponentSerializer {
    private int lastComponent = 0;
    private final Map<WorkspaceComponent<?>, Integer> componentIds 
        = new IdentityHashMap<WorkspaceComponent<?>, Integer>();
    private int lastAttribute = 0;
    private final Map<Attribute, String> attributeKeys 
        = new IdentityHashMap<Attribute, String>();
    
    int getId(final WorkspaceComponent<?> component) {
        Integer id = componentIds.get(component);
        
        if (id == null) {
            id = lastComponent++;
            componentIds.put(component, id);
        }
        
        return id;
    }
    
//    int assignId(final Attribute attribute) {
//        Integer id = attributeKeys.get(attribute);
//        
//        if (id == null) {
//            id = lastAttribute++;
//            attributeKeys.put(attribute, id);
//        }
//        
//        attribute.setId(id);
//        
//        return id;
//    }
    
    int serializeComponent(final WorkspaceComponent<?> component, final OutputStream stream) {
        component.save(stream, null);
        
        return getId(component);
    }
}
