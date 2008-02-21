package org.simbrain.workspace;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;


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
        
//        zipStream.setMethod(ZipOutputStream.STORED);
        
        WorkspaceComponentSerializer serializer = new WorkspaceComponentSerializer();
        ArchiveContents archive = new ArchiveContents(serializer);

        for (Coupling<?> coupling : workspace.getManager().getCouplings()) {
            archive.addCoupling(coupling);
        }
        
        for (WorkspaceComponent<?> component : workspace.getComponentList()) {
            ArchiveContents.Component archiveComp = archive.addComponent(component);
            
            ZipEntry entry = new ZipEntry(archiveComp.uri);
            zipStream.putNextEntry(entry);
            serializer.serializeComponent(component, zipStream);
        }
        
        ZipEntry entry = new ZipEntry("contents.xml");
        zipStream.putNextEntry(entry);
        archive.toXml(zipStream);
        
        zipStream.finish();
    }
    
    @SuppressWarnings("unchecked")
    public void deserialize(InputStream stream) throws IOException {
        Map<String, byte[]> entries = new HashMap<String, byte[]>();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        
        byte[] buffer = new byte[1024];
        
        for (int read; (read = stream.read(buffer)) >= 0; ) {
            bytes.write(buffer, 0, read);
        }
        
        ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        
        ArchiveContents contents = null;
        WorkspaceComponentDeserializer componentDeserializer = new WorkspaceComponentDeserializer();
        
        System.out.println("deserializing");
        
        ZipEntry entry = zip.getNextEntry();
        
        for (ZipEntry next; entry != null; entry = next) {
            next = zip.getNextEntry();
            
            String name = entry.getName();
            System.out.println("entry: " + name);
            System.out.println("entry.getSize(): " + entry.getSize());
            
            entries.put(name, new byte[(int) entry.getSize()]);
        }
        
        zip = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        
        while ((entry = zip.getNextEntry()) != null) {
            byte[] data = entries.get(entry.getName());
            
            zip.read(data);
        }
        
        contents = (ArchiveContents) ArchiveContents.xstream().fromXML(
            new ByteArrayInputStream(entries.get("contents.xml")));
        
        if (contents.components != null) {
            for (ArchiveContents.Component component : contents.components) {
                WorkspaceComponent<?> wc = componentDeserializer.deserializeWorkspaceComponent(
                    component.className, new ByteArrayInputStream(
                    entries.get(component.uri)), "name", null);
    
                workspace.addWorkspaceComponent(wc);
            }
        }
        
        if (contents.couplings != null) {
            for (ArchiveContents.Coupling coupling : contents.couplings) {
                workspace.addCoupling(new Coupling(
                    (ProducingAttribute<?>) componentDeserializer.getAttribute(coupling.source),
                    (ConsumingAttribute<?>) componentDeserializer.getAttribute(coupling.target)));
            }
        }
    }
}
