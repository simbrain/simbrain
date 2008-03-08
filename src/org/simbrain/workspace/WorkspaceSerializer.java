package org.simbrain.workspace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;

/**
 * Serializes and deserializes workspaces.
 * 
 * @author Matt Watson
 */
public class WorkspaceSerializer {
    /** The number of bytes to attempt to read at a time from an InputStream. */
    private static final int BUFFER_SIZE = 1024;
    
    /** The current workspace. */
    private final Workspace workspace;
    
    private final SimbrainDesktop desktop;
    
    /**
     * Creates a new serializer.
     * 
     * @param workspace The workspace to serialize to or from.
     */
    public WorkspaceSerializer(final Workspace workspace) {
        this.workspace = workspace;
        this.desktop = SimbrainDesktop.getDesktop(workspace);
    }
    
    public void exportWorkspace() {
        
    }
    
    public void importWorkspace() {
        
    }
    
    public void writeWorkspace(File file)
    {
        
    }
    
    /**
     * Serializes the workspace to a zip compressed stream.
     * 
     * @param output The output stream to write to.
     * @throws IOException If there is an IO error.
     */
    public void serialize(final OutputStream output) throws IOException {
        ZipOutputStream zipStream = new ZipOutputStream(output);
        
        WorkspaceComponentSerializer serializer = new WorkspaceComponentSerializer();
        ArchiveContents archive = new ArchiveContents(serializer);
        
        for (WorkspaceComponent<?> component : workspace.getComponentList()) {
            ArchiveContents.Component archiveComp = archive.addComponent(component);
            
            ZipEntry entry = new ZipEntry(archiveComp.uri);
            zipStream.putNextEntry(entry);
            serializer.serializeComponent(component, zipStream);
            
            DesktopComponent<?> desktopComponent = SimbrainDesktop.getDesktop(workspace)
                .getDesktopComponent(component);
            
            if (desktopComponent != null) {
                ArchiveContents.Component.DesktopComponent dc
                    = archiveComp.addDesktopComponent(desktopComponent);
                entry = new ZipEntry(dc.uri);
                zipStream.putNextEntry(entry);
                desktopComponent.save(zipStream);
            }
        }
        
        for (Coupling<?> coupling : workspace.getManager().getCouplings()) {
            archive.addCoupling(coupling);
        }
        
        ZipEntry entry = new ZipEntry("contents.xml");
        zipStream.putNextEntry(entry);
        archive.toXml(zipStream);
        
        zipStream.finish();
    }
    
    /**
     * Creates a workspace from a zip compressed input stream.
     * 
     * @param stream the stream to read from.  This is expected to be zip compressed.
     * @throws IOException if an IO error occurs.
     */
    @SuppressWarnings("unchecked")
    public void deserialize(final InputStream stream) throws IOException {
        Map<String, byte[]> entries = new HashMap<String, byte[]>();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        
        byte[] buffer = new byte[BUFFER_SIZE];
        
        for (int read; (read = stream.read(buffer)) >= 0; ) {
            bytes.write(buffer, 0, read);
        }
        
        ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        
        ArchiveContents contents = null;
        WorkspaceComponentDeserializer componentDeserializer = new WorkspaceComponentDeserializer();
        
        ZipEntry entry = zip.getNextEntry();
        
        for (ZipEntry next; entry != null; entry = next) {
            next = zip.getNextEntry();
            entries.put(entry.getName(), new byte[(int) entry.getSize()]);
        }
        
        zip = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        
        while ((entry = zip.getNextEntry()) != null) {
            byte[] data = entries.get(entry.getName());
            
            read(zip, data);
        }
        
        contents = (ArchiveContents) ArchiveContents.xstream().fromXML(
            new ByteArrayInputStream(entries.get("contents.xml")));
        
        if (contents.components != null) {
            for (ArchiveContents.Component component : contents.components) {
                WorkspaceComponent<?> wc = componentDeserializer.deserializeWorkspaceComponent(
                    component, new ByteArrayInputStream(entries.get(component.uri)));
    
                if (component.desktopComponent != null) {
                    workspace.toggleEvents(false);
                }
                
                workspace.addWorkspaceComponent(wc);
                
                if (component.desktopComponent != null) {
                    DesktopComponent dc = componentDeserializer.deserializeDesktopComponent(
                        component.desktopComponent.className, wc, new ByteArrayInputStream(
                        entries.get(component.desktopComponent.uri)), component.name);
                    
                    desktop.addComponent(wc, dc);
                    
                    workspace.toggleEvents(true);
                }
            }
        }
        
        if (contents.couplings != null) {
            for (ArchiveContents.Coupling coupling : contents.couplings) {
                System.out.println("coupling source: " + coupling.source.uri + " " + coupling.source.key);
                System.out.println("coupling target: " + coupling.target.uri + " " + coupling.target.key);
                
                WorkspaceComponent<?> sourceComponent = componentDeserializer.getComponent(coupling.source.uri);
                WorkspaceComponent<?> targetComponent = componentDeserializer.getComponent(coupling.target.uri);
                
                workspace.addCoupling(new Coupling(    
                    (ProducingAttribute<?>) sourceComponent.getAttributeForKey(coupling.source.key),
                    (ConsumingAttribute<?>) targetComponent.getAttributeForKey(coupling.target.key)));
            }
        }
    }
    
    /**
     * Helper method that will read the InputStream repeatedly until the given
     * array is filled.
     * 
     * @param istream the InputStream to read from.
     * @param bytes the array to write to
     * @throws IOException if there is an IO error
     */
    private static void read(final InputStream istream, final byte[] bytes) throws IOException {
        int pos = 0;
        
        while (pos < bytes.length) {
            int read = istream.read(bytes, pos, bytes.length - pos);
            
            if (read < 0) { throw new RuntimeException("premature EOF"); }
            
            pos += read;
        }
    }
}
