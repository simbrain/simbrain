package org.simbrain.workspace;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
     * Object used for building and reading the TOC of an
     * archive.
     * 
     * @author Matt Watson
     */
    class ArchiveContents {
//        SimbrainDesktop desktop;
        transient Map<WorkspaceComponent, String> componentUris = new HashMap<WorkspaceComponent, String>();
        final ArrayList<Component> components = new ArrayList<Component>();
        ArrayList<Coupling> couplings = new ArrayList<Coupling>();
        final WorkspaceComponentSerializer serializer;
        
        ArchiveContents(WorkspaceComponentSerializer serializer) {
            this.serializer = serializer;
        }
        
        Component addComponent(WorkspaceComponent<?> workspaceComponent) {
            Component component = new Component(serializer, workspaceComponent);
            components.add(component);
            
            componentUris.put(workspaceComponent, component.uri);
            
            System.out.println("uri in: " + component.uri);
            
            return component;
        }
        
        Component getComponent(String uri) {
            for (Component component : components) {
                if (component.uri.equals(uri)) return component;
            }
            
            return null;
        }
        
        Coupling addCoupling(org.simbrain.workspace.Coupling<?> coupling) {
            Coupling c = new Coupling(this, coupling);
            
            couplings.add(c);
            
            return c;
        }
        
        static class SimbrainDesktop {
            String uri;
        }
        
        static class Component {
            final String className;
            final String name;
            final String uri;
            final int id;
            final String format;
            DesktopComponent desktopComponent;
            
            Component(WorkspaceComponentSerializer serializer, WorkspaceComponent<?> component) {
                this.className = component.getClass().getCanonicalName();
                this.id = serializer.getId(component);
                this.name = component.getName();
                this.format = component.getDefaultFormatKey();
                this.uri = "components/" + id + '_' + name.replaceAll("\\s", "_") + '.' + format;
            }
            
            DesktopComponent addDesktopComponent(org.simbrain.workspace.gui.DesktopComponent<?> dc) {
                return desktopComponent = new DesktopComponent(this, dc);
            }
            
            static class DesktopComponent {
                final String className;
                final String uri;
                final String format;
                
                DesktopComponent(Component parent, org.simbrain.workspace.gui.DesktopComponent<?> dc) {
                    this.className = dc.getClass().getCanonicalName();
                    this.format = dc.getDefaultFormatKey();
                    this.uri = "guis/" + parent.id + '_' + parent.name.replaceAll("\\s", "_") + '.' + format;
                }
            }
        }
        
        static class Coupling {
            
            final Attribute source;
            final Attribute target;
            
            Coupling(ArchiveContents parent, org.simbrain.workspace.Coupling<?> coupling) {
                ProducingAttribute<?> producing = coupling.getProducingAttribute();
                ConsumingAttribute<?> consuming = coupling.getConsumingAttribute();
                
                this.source = new Attribute(parent, producing);
                this.target = new Attribute(parent, consuming);
            }
            
            static class Attribute {
                final String uri;
                final String key;
                
                Attribute(ArchiveContents parent, org.simbrain.workspace.Attribute attribute) {
                    WorkspaceComponent<?> comp = attribute.getParent().getParentComponent();
                    
                    this.uri = parent.componentUris.get(comp);
                    this.key = comp.getKeyForAttribute(attribute);
                    
                    System.out.println("uri: " + uri);
                }
            }
        }
        
        void toXml(OutputStream stream) {
            xstream().toXML(this, stream);
        }
        
        static XStream xstream() {
            XStream xstream = new XStream(new DomDriver());
            
            xstream.omitField(ArchiveContents.class, "serializer");
            xstream.omitField(Component.class, "serializer");
            xstream.omitField(Coupling.class, "serializer");
            xstream.omitField(Component.class, "data");
            xstream.omitField(Component.DesktopComponent.class, "data");
            
//            xstream.omitField(Component.class, "data");
//            xstream.omitField(Component.class, "data");
            
            xstream.alias("Workspace", ArchiveContents.class);
            xstream.alias("Component", Component.class);
            xstream.alias("Coupling", Coupling.class);
            xstream.alias("DesktopComponent", Component.DesktopComponent.class);
            
            xstream.addImplicitCollection(ArchiveContents.class, "components", Component.class);
            xstream.addImplicitCollection(ArchiveContents.class, "couplings", Coupling.class);
            xstream.addImplicitCollection(Component.class, "desktopComponents");
            
            return xstream;
        }
    }