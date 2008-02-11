package org.simbrain.workspace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class WorkspaceSerializer {
    private final Workspace workspace;
    
    public WorkspaceSerializer(Workspace workspace) {
        this.workspace = workspace;
    }
    
    public void exportWorkspace() {
        
    }
    
    public void importWorkspace() {
        
    }
    
    public void writeWorkspace(File file)
    {
        
    }
    
    public void serialize(OutputStream output) throws IOException {
        ZipOutputStream zipStream = new ZipOutputStream(output);
        TableOfContents contents = new TableOfContents();
        WorkspaceComponentSerializer componentSerializer = new WorkspaceComponentSerializer();
        
        for (WorkspaceComponent<?> component : workspace.getComponentList()) {
            String name = component.getName();
            zipStream.putNextEntry(new ZipEntry("components/" + name));
            int id = componentSerializer.serializeComponent(component, zipStream);
        }
        
        zipStream.putNextEntry(new ZipEntry("contents.xml"));
        
        contents.toXml(zipStream);
        
        zipStream.finish();
        
        output.flush();
        output.close();
    }
    
    public void deserialize(InputStream stream) throws IOException {
        Map<String, byte[]> entries = new HashMap();
        
        ZipInputStream zip = new ZipInputStream(stream);
        TableOfContents contents = null;
        WorkspaceComponentSerializer componentSerializer = new WorkspaceComponentSerializer();
        
        for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
            String name = entry.getName();
//            System.out.println("entry: " + name);
//            System.out.println("entry.getSize(): " + entry.getSize());
            
            if ("contents.xml".equals(name)) {
                contents = (TableOfContents) TableOfContents.xstream().fromXML(
                    new SubsetInputStream(zip, entry.getSize()));
                
//                System.out.println("entry.getSize(): " + entry.getSize());
//                
//                for (TableOfContents.Component component : contents.components) {
//                    System.out.println("component: " + component.uri);
//                }
            } else {
                byte[] bytes = new byte[(int) entry.getSize()];
                
                if (zip.read(bytes) != bytes.length) {
                    throw new IllegalArgumentException("did not get full entry");
                }
                
                entries.put(entry.getName(), bytes);
            }
            
            for (TableOfContents.Component component : contents.components) {
                WorkspaceComponent wc = componentSerializer.deserializeWorkspaceComponent(
                    component.className, new ByteArrayInputStream(
                    entries.get(component.uri)), "name", null);

                if (component.desktopComponent != null) {
                    workspace.toggleEvents(false);
                    
                    DesktopComponent dc = componentSerializer.deserializeDesktopComponent(
                        component.desktopComponent.className, new ByteArrayInputStream(
                        entries.get(component.desktopComponent.uri)), "name");
                    
                    SimbrainDesktop.getDesktop(workspace).addComponent(wc, dc);
                    
                    workspace.toggleEvents(true);
                }
                
                workspace.addWorkspaceComponent(wc);
            }
        }
    }
    
    private class SubsetInputStream extends InputStream {
        int index = 0;
        final InputStream stream;
        final long length;
        
        SubsetInputStream(InputStream stream, long length) {
            this.stream = stream;
            this.length = length;
        }

        @Override
        public int read() throws IOException {
            if (index++ < length) return stream.read();
            else return -1;
        }
    }
    
    static class TableOfContents {
        SimbrainDesktop desktop;
        ArrayList<Component> components = new ArrayList<Component>();
        ArrayList<Coupling> couplings;
        
        void addComponent(WorkspaceComponent<?> workspaceComponent) {
            TableOfContents.Component component = new TableOfContents.Component();
            components.add(component);
            
            component.className = workspaceComponent.getClass().getName();
//            tComp.id = componentSerializer.serializeComponent(component, stream);
            component.uri = workspaceComponent.getName();
        }
        
        Component getComponent(String uri) {
            for (Component component : components) {
                if (component.uri.equals(uri)) return component;
            }
            
            return null;
        }
        
        static class SimbrainDesktop {
            String uri;
        }
        
        static class Component {
            String className;
            String uri;
            int id;
            DesktopComponent desktopComponent;
            
            class DesktopComponent {
                String className;
                String uri;
            }
        }
        
        static class Coupling {
            int source;
            int target;
        }
        
        private void toXml(OutputStream stream) {
            xstream().toXML(this, stream);
        }
        
        private static XStream xstream() {
            XStream xstream = new XStream(new DomDriver());
            
            xstream.omitField(Component.class, "data");
            xstream.omitField(Component.DesktopComponent.class, "data");
//            xstream.omitField(Component.class, "data");
//            xstream.omitField(Component.class, "data");
            
            xstream.alias("Workspace", TableOfContents.class);
            xstream.alias("Component", Component.class);
            xstream.alias("Coupling", Coupling.class);
            xstream.alias("DesktopComponent", Component.DesktopComponent.class);
            
            xstream.addImplicitCollection(TableOfContents.class, "components", Component.class);
            xstream.addImplicitCollection(TableOfContents.class, "couplings", Coupling.class);
            xstream.addImplicitCollection(Component.class, "desktopComponents");
            
            return xstream;
        }
    }
}
