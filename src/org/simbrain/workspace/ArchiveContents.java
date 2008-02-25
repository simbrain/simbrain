package org.simbrain.workspace;

import java.io.OutputStream;
import java.util.ArrayList;

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
        final ArrayList<Component> components = new ArrayList<Component>();
        ArrayList<Coupling> couplings = new ArrayList<Coupling>();
        final WorkspaceComponentSerializer serializer;
        
        ArchiveContents(WorkspaceComponentSerializer serializer) {
            this.serializer = serializer;
        }
        
        Component addComponent(WorkspaceComponent<?> workspaceComponent) {
            Component component = new Component(serializer, workspaceComponent);
            components.add(component);
            
            return component;
        }
        
        Component getComponent(String uri) {
            for (Component component : components) {
                if (component.uri.equals(uri)) return component;
            }
            
            return null;
        }
        
        Coupling addCoupling(org.simbrain.workspace.Coupling<?> coupling) {
            Coupling c = new Coupling(serializer, coupling);
            
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
            DesktopComponent desktopComponent;
            
            Component(WorkspaceComponentSerializer serializer, WorkspaceComponent<?> component) {
                this.className = component.getClass().getCanonicalName();
                this.id = serializer.getId(component);
                this.name = component.getName();
                this.uri = "components/" + id + '_' + name.replaceAll("\\s", "_");
            }
            
            DesktopComponent addDesktopComponent(org.simbrain.workspace.gui.DesktopComponent<?> dc) {
                return desktopComponent = new DesktopComponent(dc);
            }
            
            class DesktopComponent {
                final String className;
                final String uri;
                
                DesktopComponent(org.simbrain.workspace.gui.DesktopComponent<?> dc) {
                    this.className = dc.getClass().getCanonicalName();
                    this.uri = "guis/" + id + '_' + name.replaceAll("\\s", "_");
                }
            }
        }
        
        static class Coupling {            
            final int source;
            final int target;
            
            Coupling(WorkspaceComponentSerializer serializer, org.simbrain.workspace.Coupling<?> coupling) {
                ProducingAttribute<?> producing = coupling.getProducingAttribute();
                ConsumingAttribute<?> consuming = coupling.getConsumingAttribute();
                
                this.source = serializer.assignId(producing);
                this.target = serializer.assignId(consuming);
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