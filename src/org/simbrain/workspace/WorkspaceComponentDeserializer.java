package org.simbrain.workspace;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.simbrain.workspace.gui.DesktopComponent;

public class WorkspaceComponentDeserializer {
    private final Map<String, WorkspaceComponent<?>> componentKeys 
        = new HashMap<String, WorkspaceComponent<?>>();
//    private final Map<Integer, Attribute> attributes 
//        = new HashMap<Integer, Attribute>();
    
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
    
    
//    Attribute getAttribute(ArchiveContents.Coupling coupling) {
//        return attributes.get(id);
//    }
    
    WorkspaceComponent<?> getComponent(String uri) {
        return componentKeys.get(uri);
    }
    
    
    @SuppressWarnings("unchecked")
    WorkspaceComponent<?> deserializeWorkspaceComponent(ArchiveContents.Component component,
            final InputStream input) {
        try {
            Class<WorkspaceComponent<?>> clazz 
                = (Class<WorkspaceComponent<?>>) Class.forName(component.className);
            Method method = clazz.getMethod("open", InputStream.class, String.class, String.class);
            
            WorkspaceComponent<?> wc = (WorkspaceComponent<?>) method.invoke(null, input, component.name, null);//component.format); 
            
            for (Consumer consumer : wc.getConsumers()) {
                for (Attribute attribute : consumer.getConsumingAttributes()) {
                    String key = wc.getKeyForAttribute(attribute);
                    System.out.println("consumer: " + key);
//                    if (id >= 0) attributes.put(attribute.getId(), attribute);
                }
            }
            
            for (Producer producer : wc.getProducers()) {
                for (Attribute attribute : producer.getProducingAttributes()) {
                    String key = wc.getKeyForAttribute(attribute);
                    System.out.println("producer: " + key);
//                    if (id >= 0) attributes.put(attribute.getId(), attribute);
                }
            }
            
            componentKeys.put(component.uri, wc);
            
            return wc;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @SuppressWarnings("unchecked")
    DesktopComponent<?> deserializeDesktopComponent(final String className,
            final WorkspaceComponent component, final InputStream input, final String name) {
        try {
            Class<WorkspaceComponent<?>> clazz 
                = (Class<WorkspaceComponent<?>>) Class.forName(className);
            Method method = clazz.getMethod("open", WorkspaceComponent.class,
                InputStream.class, String.class);
            
            return (DesktopComponent<?>) method.invoke(null, component, input, name);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
