package org.simbrain.workspace;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.simbrain.workspace.gui.DesktopComponent;

public class WorkspaceComponentDeserializer {
//    private final Map<Integer, WorkspaceComponent<?>> componentIds 
//        = new HashMap<Integer, WorkspaceComponent<?>>();
    private final Map<Integer, Attribute> attributes 
        = new HashMap<Integer, Attribute>();
    
//    int getId(final WorkspaceComponent<?> component) {
//        Integer id = componentIds.get(component);
//        
//        if (id == null) {
//            id = lastComponent++;
//            componentIds.put(component, id);
//        }
//        
//        return id;
//    }
    
    Attribute getAttribute(final int id) {
        return attributes.get(id);
    }
    
    @SuppressWarnings("unchecked")
    WorkspaceComponent<?> deserializeWorkspaceComponent(final String className,
            final InputStream input, final String name, final String format) {
        try {
            Class<WorkspaceComponent<?>> clazz 
                = (Class<WorkspaceComponent<?>>) Class.forName(className);
            Method method = clazz.getMethod("open", InputStream.class, String.class, String.class);
            
            WorkspaceComponent<?> component = (WorkspaceComponent<?>) method.invoke(null, input, name, format); 
            
            for (Consumer consumer : component.getConsumers()) {
                for (Attribute attribute : consumer.getConsumingAttributes()) {
                    int id = attribute.getId();
                    System.out.println("consumer: " + id);
                    if (id >= 0) attributes.put(attribute.getId(), attribute);
                }
            }
            
            for (Producer producer : component.getProducers()) {
                for (Attribute attribute : producer.getProducingAttributes()) {
                    int id = attribute.getId();
                    System.out.println("producer: " + id);
                    if (id >= 0) attributes.put(attribute.getId(), attribute);
                }
            }
            return component;
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
